package com.kerjalah.app.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// [B] Module 1 - User repository, now backed by Supabase Auth + profiles.
// Same public surface as the mock; login/register now return an Outcome so
// screens learn WHY something failed without ever seeing the exception.
// Passwords never touch our code or database - Supabase Auth owns them.
object UserRepository {

    private const val TAG = "UserRepository"
    private val supabase get() = SupabaseClientProvider.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // All profiles (for employer <-> student joins in Module 3).
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Who is logged in right now. Null = logged out.
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Splash waits for this: true once Supabase finished restoring
    // (or failing to restore) a saved session.
    private val _sessionChecked = MutableStateFlow(false)
    val sessionChecked: StateFlow<Boolean> = _sessionChecked.asStateFlow()

    // Register happens in two steps (form -> role screen);
    // the form data waits here in between.
    private var pendingName: String? = null
    private var pendingEmail: String? = null
    private var pendingPassword: String? = null

    init {
        // React to session changes: app start restore, login, logout.
        scope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val uid = status.session.user?.id
                        if (uid != null && _currentUser.value?.id != uid) {
                            _currentUser.value = fetchProfile(uid)
                        }
                        refreshUsers()
                        // RLS returns nothing before login, so the other
                        // caches must re-sync now that we have a session.
                        JobRepository.refresh()
                        ApplicationRepository.refresh()
                        _sessionChecked.value = true
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentUser.value = null
                        _sessionChecked.value = true
                    }
                    else -> Unit // still initializing -> keep waiting
                }
            }
        }
    }

    // --- Login. Success carries the role; failure carries an AppError. ---
    // This used to return UserRole?, so every cause - wrong password, no
    // network, missing profile row - collapsed into null and the screen had to
    // guess with one catch-all sentence. Now the screen is told which of them
    // happened, and still never sees the exception.
    suspend fun login(email: String, password: String): Outcome<UserRole> {
        val signIn = resultOf {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }
        signIn.exceptionOrNull()?.let {
            return Outcome.Failure(it.logged(TAG, "Login failed"))
        }

        val uid = supabase.auth.currentUserOrNull()?.id
        if (uid == null) {
            Log.w(TAG, "Sign-in reported success but no session was stored")
            return Outcome.Failure(AppError.SessionExpired)
        }
        val profile = fetchProfile(uid)
        if (profile == null) {
            Log.w(TAG, "Signed in, but the profiles row for this account is missing")
            return Outcome.Failure(AppError.ProfileNotReady)
        }
        _currentUser.value = profile
        return Outcome.Success(profile.role)
    }

    // --- Register step 1: park the form data (validated by the ViewModel). ---
    fun setPendingRegistration(name: String, email: String, password: String) {
        pendingName = name.trim()
        pendingEmail = email.trim()
        pendingPassword = password
    }

    // --- Register step 2: create the auth user; the profile row follows. ---
    // Returns Ok on success, or a Failure the screen can render as-is.
    //
    // The two "impossible" branches below used to be thrown as error("...")
    // strings and then shown to the user verbatim - sentences like "Disable
    // 'Confirm email' in Supabase Auth settings", which is an instruction for
    // us, not for a student. That guidance now goes to Logcat, where we read
    // it, and the user gets a sentence about their own situation.
    //
    // We no longer INSERT into profiles from here. Sign-up carries the role and
    // name as user metadata, and the on_auth_user_created trigger writes the
    // profile inside the same transaction as the auth user
    // (see supabase_migration_01.sql). The old two-call version had no
    // transaction: a dropped connection between them left an auth account with
    // no profile, which could never log in and could not be retried.
    suspend fun completeRegistration(role: UserRole): Outcome<Unit> {
        val name = pendingName ?: return Outcome.Failure(AppError.RegistrationExpired)
        val email = pendingEmail ?: return Outcome.Failure(AppError.RegistrationExpired)
        val password = pendingPassword ?: return Outcome.Failure(AppError.RegistrationExpired)

        val signUp = resultOf {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // The trigger sanitises these: anything that is not exactly
                // "EMPLOYER" becomes a student, so metadata can never smuggle
                // a value past the profiles role CHECK constraint.
                this.data = buildJsonObject {
                    put("role", role.name)
                    put("name", name)
                }
            }
        }
        signUp.exceptionOrNull()?.let {
            return Outcome.Failure(it.logged(TAG, "Sign-up failed"))
        }

        // Needs a live session right after sign-up. If this branch is taken:
        // Supabase Dashboard -> Authentication -> disable "Confirm email".
        val uid = supabase.auth.currentUserOrNull()?.id
        if (uid == null) {
            Log.w(TAG, "No session after sign-up - is \"Confirm email\" still enabled?")
            return Outcome.Failure(AppError.EmailNotConfirmed)
        }
        val profile = fetchProfile(uid)
        if (profile == null) {
            Log.w(TAG, "Profile row was not created - check the on_auth_user_created trigger")
            return Outcome.Failure(AppError.ProfileNotReady)
        }

        _currentUser.value = profile
        clearPending()
        return Ok
    }

    // --- Update profile of the logged-in user. ---
    // A failed save used to be logged and then reported as success, so the
    // screen navigated back and the edit had simply vanished.
    suspend fun updateProfile(name: String, organization: String, bio: String): Outcome<Unit> {
        val current = _currentUser.value ?: return Outcome.Failure(AppError.SessionExpired)
        return resultOf {
            supabase.from("profiles").update({
                set("name", name.trim())
                set("organization", organization.trim())
                set("bio", bio.trim())
            }) {
                filter { eq("id", current.id) }
            }
        }.fold(
            onSuccess = {
                _currentUser.value = current.copy(
                    name = name.trim(),
                    organization = organization.trim(),
                    bio = bio.trim(),
                )
                Ok
            },
            onFailure = { Outcome.Failure(it.logged(TAG, "Update profile failed")) },
        )
    }

    // Signing out locally always succeeds: the user asked to be signed out, so
    // we drop the session even if the server call did not go through.
    suspend fun logout() {
        resultOf { supabase.auth.signOut() }
            .onFailure { Log.e(TAG, "Sign-out call failed; clearing the local session anyway", it) }
        _currentUser.value = null
    }

    // --- Delete account. Client-side we remove the profile + sign out.
    // Deleting the auth user itself needs a service key (server-side) -
    // out of scope; be ready to explain this trade-off to the tutor. ---
    suspend fun deleteAccount(): Outcome<Unit> {
        val current = _currentUser.value ?: return Outcome.Failure(AppError.SessionExpired)
        val deleted = resultOf {
            supabase.from("profiles").delete {
                filter { eq("id", current.id) }
            }
        }
        // Only sign out once the row is really gone. Signing out after a failed
        // delete looked exactly like a successful deletion - until the same
        // profile reappeared on the next login.
        deleted.exceptionOrNull()?.let {
            return Outcome.Failure(it.logged(TAG, "Delete profile failed"))
        }
        logout()
        return Ok
    }

    // Refresh the profile list used for Module 3 joins.
    // RLS now returns only your own row plus, for an employer, the students who
    // actually applied to one of their jobs - not the whole user directory.
    // These rows carry no email: profiles.email is gone and auth.users is
    // readable only for yourself.
    suspend fun refreshUsers() {
        resultOf {
            supabase.from("profiles").select().decodeList<ProfileRow>()
        }.onSuccess { rows ->
            _users.value = rows.map { it.toDomain() }
        }.onFailure { Log.e(TAG, "Loading profiles failed", it) }
    }

    // Your own profile, with the address taken from the session rather than
    // from a duplicated profiles.email column.
    private suspend fun fetchProfile(uid: String): User? = resultOf {
        val email = supabase.auth.currentUserOrNull()?.email.orEmpty()
        supabase.from("profiles").select {
            filter { eq("id", uid) }
        }.decodeSingleOrNull<ProfileRow>()?.toDomain(email)
    }.onFailure { Log.e(TAG, "Fetch profile failed", it) }.getOrNull()

    private fun clearPending() {
        pendingName = null
        pendingEmail = null
        pendingPassword = null
    }
}
