package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.AiClient
import com.kerjalah.app.data.AiOutcome
import com.kerjalah.app.data.ApplicationRepository
import com.kerjalah.app.data.ApplicationStatus
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.ui.application.ApplicantUi
import com.kerjalah.app.ui.application.label
import com.kerjalah.app.ui.application.toApplicantUi
import com.kerjalah.app.ui.common.UserMessage
import com.kerjalah.app.ui.common.asRetryableMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// [B] Module 3 - Applicant Detail logic layer (employer side, UDF).
// Accept / Reject are HUMAN decisions - the AI advisor (Groq) only adds a
// suggestion box here; it never presses these buttons itself.
class ApplicantDetailViewModel(private val appId: String) : ViewModel() {

    // Everything that is NOT derived from the data streams. Kept in one flow
    // so a Realtime emission cannot wipe a snackbar or a running AI retry -
    // which is exactly what happened when the state was rebuilt from scratch
    // on every combine emission.
    private data class Transient(
        val advisor: AdvisorState? = null, // null = use the stored advice
        val isDeciding: Boolean = false,
        val message: UserMessage? = null,
    )

    private val _transient = MutableStateFlow(Transient())

    private val _uiState = MutableStateFlow(ApplicantDetailUiState())
    val uiState: StateFlow<ApplicantDetailUiState> = _uiState.asStateFlow()

    init {
        // Make sure the profile list is fresh (a new student may have applied).
        viewModelScope.launch { UserRepository.refreshUsers() }
        viewModelScope.launch {
            combine(
                ApplicationRepository.getById(appId),
                UserRepository.users,
                JobRepository.jobs,
                _transient,
            ) { app, users, jobs, transient ->
                val applicant = app?.let { a ->
                    a.toApplicantUi(
                        student = users.find { it.id == a.studentId },
                        job = jobs.find { it.id == a.jobId },
                    )
                }
                ApplicantDetailUiState(
                    applicant = applicant,
                    isLoading = false,
                    notFound = app == null,
                    advisor = transient.advisor ?: applicant.storedAdvice(),
                    isDeciding = transient.isDeciding,
                    message = transient.message,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Events from UI: the employer's decision. Status flows back down
    // through the stream - and the student sees it instantly (Realtime).
    fun onAcceptClick() = decide(ApplicationStatus.ACCEPTED)

    fun onRejectClick() = decide(ApplicationStatus.REJECTED)

    private fun decide(status: ApplicationStatus) {
        if (_transient.value.isDeciding) return // ignore double taps
        _transient.update { it.copy(isDeciding = true, message = null) }
        viewModelScope.launch {
            val result = ApplicationRepository.updateStatus(appId, status)
            _transient.update {
                it.copy(
                    isDeciding = false,
                    // A rejected write used to leave the chip on PENDING with
                    // no hint that the student had not been told anything.
                    message = (result as? Outcome.Failure)?.error?.asRetryableMessage(),
                )
            }
            lastDecision = status.takeIf { result is Outcome.Failure }
        }
    }

    private var lastDecision: ApplicationStatus? = null

    fun onRetryDecision() {
        lastDecision?.let { decide(it) }
    }

    fun onMessageShown() {
        _transient.update { it.copy(message = null) }
    }

    // --- AI advisor, re-run on demand -------------------------------------
    //
    // Note for the tutor: this verdict is NOT written back. The ai_* columns
    // are immutable after insert (applications_guard_immutable_columns, see
    // supabase_migration_02.sql), which is what stops an employer rewriting a
    // score. So a re-run is a fresh estimate for this sitting only, and the
    // card says so rather than pretending it was saved.
    fun onAdvisorRetryClick() {
        if (_transient.value.advisor is AdvisorState.Loading) return
        _transient.update { it.copy(advisor = AdvisorState.Loading) }
        viewModelScope.launch {
            val application = ApplicationRepository.getById(appId).first()
            val job = JobRepository.jobs.value.find { it.id == application?.jobId }
            val student = UserRepository.users.value.find { it.id == application?.studentId }
            if (job == null || student == null) {
                _transient.update {
                    it.copy(
                        advisor = AdvisorState.Failed(
                            "The advisor needs the job and the applicant's profile, " +
                                "and one of them isn't loaded yet.",
                        ),
                    )
                }
                return@launch
            }
            // assessApplication never throws and never outruns its own budget,
            // so this coroutine cannot leave the card stuck on Loading.
            val advisor = when (val outcome = AiClient.assessApplication(job, student)) {
                is AiOutcome.Advice -> AdvisorState.Advice(
                    matchPercent = outcome.assessment.matchPercent,
                    label = outcome.assessment.suggestedStatus.label(),
                    reason = outcome.assessment.reason,
                    isLive = true,
                )
                is AiOutcome.Unavailable -> AdvisorState.Failed(outcome.error.message)
            }
            _transient.update { it.copy(advisor = advisor) }
        }
    }
}

// The advice stored on the row when the student applied, if there is any.
private fun ApplicantUi?.storedAdvice(): AdvisorState {
    val percent = this?.aiMatchPercent ?: return AdvisorState.Missing
    return AdvisorState.Advice(
        matchPercent = percent,
        label = aiSuggestedLabel,
        reason = aiReason,
    )
}
