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
// Same public surface as the mock; only return types of login/register
// got richer (role / error message) so screens can react properly.
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

    // --- Login. Returns the role on success, null on bad credentials/offline. ---
    suspend fun login(email: String, password: String): UserRole? = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        val uid = supabase.auth.currentUserOrNull()?.id ?: return@runCatching null
        val profile = fetchProfile(uid) ?: return@runCatching null
        _currentUser.value = profile
        profile.role
    }.onFailure { Log.e(TAG, "Login failed", it) }.getOrNull()

    // --- Register step 1: park the form data (validated by the ViewModel). ---
    fun setPendingRegistration(name: String, email: String, password: String) {
        pendingName = name.trim()
        pendingEmail = email.trim()
        pendingPassword = password
    }

    // --- Register step 2: create the auth user; the profile row follows. ---
    // Returns null on success, or a user-readable error message.
    //
    // We no longer INSERT into profiles from here. Sign-up carries the role and
    // name as user metadata, and the on_auth_user_created trigger writes the
    // profile inside the same transaction as the auth user
    // (see supabase_migration_01.sql). The old two-call version had no
    // transaction: a dropped connection between them left an auth account with
    // no profile, which could never log in and could not be retried.
    suspend fun completeRegistration(role: UserRole): String? {
        val name = pendingName ?: return "Registration info missing - please go back."
        val email = pendingEmail ?: return "Registration info missing - please go back."
        val password = pendingPassword ?: return "Registration info missing - please go back."
        return runCatching {
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
            // Needs a live session right after sign-up. If this fails:
            // Supabase Dashboard -> Authentication -> disable "Confirm email".
            val uid = supabase.auth.currentUserOrNull()?.id
                ?: error("No session after sign-up. Disable 'Confirm email' in Supabase Auth settings.")
            _currentUser.value = fetchProfile(uid)
                ?: error("Profile was not created. Check the on_auth_user_created trigger.")
            clearPending()
            null // success
        }.getOrElse { e ->
            Log.e(TAG, "Registration failed", e)
            e.message ?: "Registration failed. Check your connection."
        }
    }

    // --- Update profile of the logged-in user. ---
    suspend fun updateProfile(name: String, organization: String, bio: String) {
        val current = _currentUser.value ?: return
        runCatching {
            supabase.from("profiles").update({
                set("name", name.trim())
                set("organization", organization.trim())
                set("bio", bio.trim())
            }) {
                filter { eq("id", current.id) }
            }
            _currentUser.value = current.copy(
                name = name.trim(),
                organization = organization.trim(),
                bio = bio.trim(),
            )
        }.onFailure { Log.e(TAG, "Update profile failed", it) }
    }

    suspend fun logout() {
        runCatching { supabase.auth.signOut() }
            .onFailure { Log.e(TAG, "Logout failed", it) }
        _currentUser.value = null
    }

    // --- Delete account. Client-side we remove the profile + sign out.
    // Deleting the auth user itself needs a service key (the :advisor server) -
    // out of scope; be ready to explain this trade-off to the tutor. ---
    suspend fun deleteAccount() {
        val current = _currentUser.value ?: return
        runCatching {
            supabase.from("profiles").delete {
                filter { eq("id", current.id) }
            }
        }.onFailure { Log.e(TAG, "Delete profile failed", it) }
        logout()
    }

    // Refresh the profile list used for Module 3 joins.
    // RLS now returns only your own row plus, for an employer, the students who
    // actually applied to one of their jobs - not the whole user directory.
    // These rows carry no email: profiles.email is gone and auth.users is
    // readable only for yourself.
    suspend fun refreshUsers() {
        runCatching {
            supabase.from("profiles").select().decodeList<ProfileRow>()
        }.onSuccess { rows ->
            _users.value = rows.map { it.toDomain() }
        }.onFailure { Log.e(TAG, "Loading profiles failed", it) }
    }

    // Your own profile, with the address taken from the session rather than
    // from a duplicated profiles.email column.
    private suspend fun fetchProfile(uid: String): User? = runCatching {
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
