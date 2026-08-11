package com.kerjalah.app.data.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    // --- CRUD: Create (Apply). False if already applied. ---
    // AI phase: ask Gemini for a match assessment ONCE, in the background,
    // and store it WITH the new row. If the AI fails or times out, the
    // student still applies normally - AI advises, it never gates.
    suspend fun apply(jobId: String, studentId: String): Boolean {
        val duplicate = _applications.value.any {
            it.jobId == jobId && it.studentId == studentId
        }
        if (duplicate) return false // DB unique(job_id, student_id) also guards

        val job = JobRepository.jobs.value.find { it.id == jobId }
        val student = UserRepository.currentUser.value
        val ai = if (job != null && student != null) {
            withTimeoutOrNull(12_000) { GeminiClient.assessApplication(job, student) }
        } else {
            null
        }

        val ok = runCatching {
            supabase.from("applications").insert(
                ApplicationInsert(
                    jobId = jobId,
                    studentId = studentId,
                    status = ApplicationStatus.PENDING.name,
                    appliedAt = System.currentTimeMillis(),
                    aiMatchPercent = ai?.matchPercent,
                    aiSuggestedStatus = ai?.suggestedStatus,
                    aiReason = ai?.reason,
                ),
            )
        }.onFailure { Log.e(TAG, "Apply failed", it) }.isSuccess
        refresh()
        return ok
    }

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
