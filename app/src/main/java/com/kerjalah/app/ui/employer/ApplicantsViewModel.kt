package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.ApplicationRepository
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.ui.application.toApplicantUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [B] Module 3 - Applicants list logic layer (employer side, UDF).
// Joins THREE streams: applications for this job + student profiles + the job.
class ApplicantsViewModel(private val jobId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicantsUiState())
    val uiState: StateFlow<ApplicantsUiState> = _uiState.asStateFlow()

    init {
        // Make sure the profile list is fresh (a new student may have applied).
        viewModelScope.launch { UserRepository.refreshUsers() }
        viewModelScope.launch {
            combine(
                ApplicationRepository.applications,
                UserRepository.users,
                JobRepository.getJobById(jobId),
            ) { applications, users, job ->
                ApplicantsUiState(
                    jobTitle = job?.title ?: "Applicants",
                    applicants = applications
                        .filter { it.jobId == jobId }
                        .sortedByDescending { it.appliedAt }
                        .map { app ->
                            app.toApplicantUi(
                                student = users.find { it.id == app.studentId },
                                job = job,
                            )
                        },
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }
}
