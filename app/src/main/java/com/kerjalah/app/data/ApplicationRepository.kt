package com.kerjalah.app.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// [B] Module 3 - Application repository, now backed by Supabase.
// Same public API as the mock version -> ViewModels unchanged.
// Realtime here IS the killer feature: the employer accepts on their
// phone, Supabase pushes the change, the student's list recolors itself.
object ApplicationRepository {

    private const val TAG = "ApplicationRepository"
    private val supabase get() = SupabaseClientProvider.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    val applications: StateFlow<List<Application>> = _applications.asStateFlow()

    init {
        scope.launch { refresh() }
        scope.launch { subscribeToChanges() }
    }

    // Note: RLS trims what we can see server-side (student -> own rows,
    // employer -> rows of own jobs), so this is never "everything".
    suspend fun refresh() {
        runCatching {
            supabase.from("applications").select().decodeList<ApplicationRow>()
        }.onSuccess { rows ->
            _applications.value = rows.map { it.toDomain() }
        }.onFailure {
            Log.e(TAG, "Loading applications failed (offline?)", it)
        }
    }

    fun getById(appId: String): Flow<Application?> =
        applications.map { list -> list.find { it.id == appId } }

    // --- CRUD: Create (Apply). False if already applied or on failure. ---
    //
    // The whole apply flow now lives in the :advisor Ktor server: the phone
    // says only "I want job X" (see AiClient), and the server verifies the JWT,
    // loads the job and the student's own profile, scores the fit with Groq and
    // writes the row. That is why the Groq key is no longer in the APK and why
    // a student can no longer hand-write their own ai_match_percent.
    //
    // What is deliberately kept from the previous client-side version:
    // - NonCancellable: once the request is away, leaving the screen must not
    //   lose the application (cancelling the ViewModel scope used to).
    // - The advisor never gates anything: if Groq fails or times out server
    //   side, the server still inserts the row, just without advice.
    suspend fun apply(jobId: String, studentId: String): Boolean =
        withContext(NonCancellable) {
            val duplicate = _applications.value.any {
                it.jobId == jobId && it.studentId == studentId
            }
            if (duplicate) return@withContext false // DB unique(job_id, student_id) also guards

            val created = when (AiClient.assessAndApply(jobId)) {
                AiClient.ApplyOutcome.CREATED -> true
                // A DUPLICATE the local cache had not seen yet is still not a
                // new application, so it reports false like the check above.
                AiClient.ApplyOutcome.DUPLICATE -> false
                // The advisor is unreachable (not running, or no network).
                // "AI advises, it never gates" has to hold for the advisor
                // being down too, not just for Groq being down - otherwise
                // tapping Apply silently does nothing at all.
                AiClient.ApplyOutcome.FAILED -> applyWithoutAdvice(jobId, studentId)
            }
            refresh()
            created
        }

    // Direct insert, no AI columns. This is the only write the client still
    // has rights to: the database grants `authenticated` INSERT on exactly
    // (job_id, student_id, status), so even this path cannot invent a match
    // percentage or backdate applied_at.
    private suspend fun applyWithoutAdvice(jobId: String, studentId: String): Boolean =
        runCatching {
            supabase.from("applications").insert(
                ApplicationInsert(jobId = jobId, studentId = studentId),
            )
            Log.w(TAG, "Advisor unreachable - applied without AI advice")
            true
        }.onFailure { Log.e(TAG, "Fallback apply failed", it) }.getOrDefault(false)

    // --- CRUD: Update (employer decides). ---
    suspend fun updateStatus(appId: String, status: ApplicationStatus) {
        runCatching {
            supabase.from("applications").update({
                set("status", status.name)
            }) {
                filter { eq("id", appId) }
            }
        }.onFailure { Log.e(TAG, "Update status failed", it) }
        refresh()
    }

    // --- CRUD: Delete (student withdraws; RLS allows only own PENDING). ---
    suspend fun withdraw(appId: String) {
        runCatching {
            supabase.from("applications").delete {
                filter { eq("id", appId) }
            }
        }.onFailure { Log.e(TAG, "Withdraw failed", it) }
        refresh()
    }

    private suspend fun subscribeToChanges() {
        runCatching {
            val channel = supabase.channel("public-applications")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "applications"
            }.onEach { refresh() }.launchIn(scope)
            channel.subscribe()
        }.onFailure { Log.e(TAG, "Realtime subscribe failed", it) }
    }
}
