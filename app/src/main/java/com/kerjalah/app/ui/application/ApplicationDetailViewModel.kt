package com.kerjalah.app.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.ApplicationRepository
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.ui.common.UserMessage
import com.kerjalah.app.ui.common.asRetryableMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [B] Module 3 - Application Detail logic layer (student side, UDF).
class ApplicationDetailViewModel(private val appId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationDetailUiState())
    val uiState: StateFlow<ApplicationDetailUiState> = _uiState.asStateFlow()

    // Kept outside the combine so a Realtime emission cannot wipe them.
    private val _isWithdrawing = MutableStateFlow(false)
    private val _message = MutableStateFlow<UserMessage?>(null)

    init {
        viewModelScope.launch {
            combine(
                ApplicationRepository.getById(appId),
                JobRepository.jobs,
                _isWithdrawing,
                _message,
            ) { app, jobs, withdrawing, message ->
                ApplicationDetailUiState(
                    application = app?.let { a ->
                        a.toStudentUi(jobs.find { it.id == a.jobId })
                    },
                    isLoading = false,
                    // Null after withdraw -> screen navigates back.
                    closed = app == null,
                    isWithdrawing = withdrawing,
                    message = message,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Event from UI: withdraw this application (only pending ones).
    // A failure here used to be invisible: the button did nothing, the row
    // stayed, and the student had no idea whether they were still applied.
    fun onWithdrawClick() {
        if (_isWithdrawing.value) return
        viewModelScope.launch {
            _isWithdrawing.value = true
            _message.value = null
            val result = ApplicationRepository.withdraw(appId)
            if (result is Outcome.Failure) {
                _message.value = result.error.asRetryableMessage()
            }
            _isWithdrawing.value = false
        }
    }

    fun onRetry() = onWithdrawClick()

    fun onMessageShown() {
        _message.value = null
    }
}
