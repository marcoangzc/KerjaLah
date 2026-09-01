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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

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

    // Outcome of an apply attempt. A plain Boolean could not tell "you already
    // applied" apart from "the write was rejected", so the screen showed the
    // same silent nothing for both.
    enum class ApplyResult { CREATED, DUPLICATE, FAILED }

    // --- CRUD: Create (Apply). ---
    // The AI advisor runs BEFORE the insert because RLS lets students
    // INSERT their own rows but never UPDATE them - so the advice must
    // ride along with the insert payload. If the advisor fails or times
    // out, the row is still inserted WITHOUT advice: AI advises, it
    // never gates the application.
    //
    // Robustness rules (why the advisor no longer "sometimes disappears"):
    // 1. A jobs-cache miss no longer silently skips the advisor - we fetch
    //    that single row straight from the DB instead.
    // 2. NonCancellable: once started, the insert always completes even if
    //    the student leaves the screen mid-wait (previously cancelling the
    //    ViewModel scope could lose the whole application).
    // 3. applied_at is no longer sent at all - the database stamps it.
    suspend fun apply(jobId: String, studentId: String): ApplyResult =
        withContext(NonCancellable) {
            val duplicate = _applications.value.any {
                it.jobId == jobId && it.studentId == studentId
            }
            // DB unique(job_id, student_id) also guards this.
            if (duplicate) return@withContext ApplyResult.DUPLICATE

            val job = findJobForAdvisor(jobId)
            if (job == null) Log.w(TAG, "Advisor skipped: job $jobId not found")
            val student = UserRepository.currentUser.value
            if (student == null) Log.w(TAG, "Advisor skipped: no logged-in user")

            val ai = if (job != null && student != null) {
                withTimeoutOrNull(12_000.milliseconds) { AiClient.assessApplication(job, student) }
            } else {
                null
            }
            if (ai == null) Log.w(TAG, "Inserting application WITHOUT advisor data")

            val ok = runCatching {
                supabase.from("applications").insert(
                    ApplicationInsert(
                        jobId = jobId,
                        studentId = studentId,
                        status = ApplicationStatus.PENDING.name,
                        aiMatchPercent = ai?.matchPercent,
                        aiSuggestedStatus = ai?.suggestedStatus?.name,
                        aiReason = ai?.reason,
                    ),
                )
            }.onFailure {
                // The most likely cause here is a database permission error on
                // the ai_* columns - see supabase_migration_02.sql. Log the
                // whole thing: the message names the exact column.
                Log.e(TAG, "Apply failed", it)
            }.isSuccess
            refresh()
            if (ok) ApplyResult.CREATED else ApplyResult.FAILED
        }

    // Cache-first lookup with a single-row fetch as fallback, so a slow
    // jobs list can never silently disable the advisor.
    private suspend fun findJobForAdvisor(jobId: String): Job? =
        JobRepository.jobs.value.find { it.id == jobId } ?: runCatching {
            supabase.from("jobs").select {
                filter { eq("id", jobId) }
            }.decodeSingleOrNull<JobRow>()?.toDomain()
        }.onFailure { Log.e(TAG, "Advisor job fetch failed", it) }.getOrNull()

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
