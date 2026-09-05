package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.CurrentUser
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.ui.common.asRetryableMessage
import com.kerjalah.app.ui.job.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [A] Module 2 - My Postings logic layer (UDF).
// Shows only jobs posted by the logged-in employer.
class MyPostingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyPostingsUiState())
    val uiState: StateFlow<MyPostingsUiState> = _uiState.asStateFlow()

    // Remembered so the snackbar's Retry knows which posting to delete.
    private var lastDeletedId: String? = null

    init {
        viewModelScope.launch {
            JobRepository.jobs.collect { jobs ->
                // copy(), not a fresh MyPostingsUiState: rebuilding the whole
                // state here wiped any snackbar message the moment Realtime
                // pushed an unrelated update.
                _uiState.value = _uiState.value.copy(
                    postings = jobs
                        .filter { it.employerId == CurrentUser.EMPLOYER_ID }
                        .map { it.toUi() },
                    isLoading = false,
                )
            }
        }
    }

    // Event from UI: delete one of my postings.
    // Why here: Composables never touch the repository directly (UDF rule).
    fun onDeleteClick(jobId: String) {
        lastDeletedId = jobId
        viewModelScope.launch {
            val result = JobRepository.deleteJob(jobId)
            if (result is Outcome.Failure) {
                // The card would otherwise just stay on screen with no
                // explanation, looking like an ignored tap.
                _uiState.value = _uiState.value.copy(
                    message = result.error.asRetryableMessage(),
                )
            }
        }
    }

    fun onRetry() {
        lastDeletedId?.let { onDeleteClick(it) }
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
