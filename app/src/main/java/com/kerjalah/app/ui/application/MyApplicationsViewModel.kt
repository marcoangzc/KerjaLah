package com.kerjalah.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.ApplicationRepository
import com.kerjalah.app.data.data.CurrentUser
import com.kerjalah.app.data.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [B] Module 3 - My Applications logic layer (UDF).
// Combines TWO data streams (applications + jobs) into one UiState.
// When the employer changes a status, the applications flow emits
// and this list updates by itself - transparent tracking.
class MyApplicationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                ApplicationRepository.applications,
                JobRepository.jobs,
            ) { applications, jobs ->
                applications
                    .filter { it.studentId == CurrentUser.STUDENT_ID }
                    .sortedByDescending { it.appliedAt } // newest first
                    .map { app -> app.toStudentUi(jobs.find { it.id == app.jobId }) }
            }.collect { list ->
                _uiState.value = MyApplicationsUiState(
                    applications = list,
                    isLoading = false,
                )
            }
        }
    }
}
