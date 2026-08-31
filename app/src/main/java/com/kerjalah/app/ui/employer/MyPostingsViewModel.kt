package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.CurrentUser
import com.kerjalah.app.data.JobRepository
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

    init {
        viewModelScope.launch {
            JobRepository.jobs.collect { jobs ->
                _uiState.value = MyPostingsUiState(
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
        viewModelScope.launch {
            JobRepository.deleteJob(jobId)
        }
    }
}
