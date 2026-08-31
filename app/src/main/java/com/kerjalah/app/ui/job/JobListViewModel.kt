package com.kerjalah.app.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [A] Module 2 - Job List logic layer (UDF).
// Data flows: Repository -> ViewModel(toUi) -> StateFlow -> Screen.
class JobListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState.asStateFlow()

    init {
        // Collect the repository stream once; every data change
        // automatically becomes a new UiState (unidirectional).
        viewModelScope.launch {
            JobRepository.jobs.collect { jobs ->
                _uiState.value = JobListUiState(
                    jobs = jobs.map { it.toUi() },
                    isLoading = false,
                )
            }
        }
    }
}
