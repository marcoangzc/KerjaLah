package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.ApplicationRepository
import com.kerjalah.app.data.data.ApplicationStatus
import com.kerjalah.app.data.data.JobRepository
import com.kerjalah.app.data.data.UserRepository
import com.kerjalah.app.ui.application.toApplicantUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [B] Module 3 - Applicant Detail logic layer (employer side, UDF).
// Accept / Reject are HUMAN decisions - the AI advisor (Groq Llama 3.3)
// only adds a suggestion box here; it never presses these buttons itself.
class ApplicantDetailViewModel(private val appId: String) : ViewModel() {

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
            ) { app, users, jobs ->
                ApplicantDetailUiState(
                    applicant = app?.let { a ->
                        a.toApplicantUi(
                            student = users.find { it.id == a.studentId },
                            job = jobs.find { it.id == a.jobId },
                        )
                    },
                    isLoading = false,
                    notFound = app == null,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Events from UI: the employer's decision. Status flows back down
    // through the stream - and the student sees it instantly (Realtime).
    fun onAcceptClick() {
        viewModelScope.launch {
            ApplicationRepository.updateStatus(appId, ApplicationStatus.ACCEPTED)
        }
    }

    fun onRejectClick() {
        viewModelScope.launch {
            ApplicationRepository.updateStatus(appId, ApplicationStatus.REJECTED)
        }
    }
}
