package com.kerjalah.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.ApplicationRepository
import com.kerjalah.app.data.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [B] Module 3 - Application Detail logic layer (student side, UDF).
class ApplicationDetailViewModel(private val appId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationDetailUiState())
    val uiState: StateFlow<ApplicationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                ApplicationRepository.getById(appId),
                JobRepository.jobs,
            ) { app, jobs ->
                ApplicationDetailUiState(
                    application = app?.let { a ->
                        a.toStudentUi(jobs.find { it.id == a.jobId })
                    },
                    isLoading = false,
                    // Null after withdraw -> screen navigates back.
                    closed = app == null,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Event from UI: withdraw this application (only pending ones).
    fun onWithdrawClick() {
        viewModelScope.launch {
            ApplicationRepository.withdraw(appId)
        }
    }
}
