package com.kerjalah.app.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.ApplicationRepository
import com.kerjalah.app.data.CurrentUser
import com.kerjalah.app.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// [A] Module 2 - Job Detail logic layer (UDF).
// jobId comes from the navigation route (student/job/{jobId}).
// Module 3 hook: combines the job stream with the applications stream,
// so the Apply button knows if this student already applied.
class JobDetailViewModel(private val jobId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    // Separate flow so a combine emission can't wipe the in-flight flag.
    private val _isApplying = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                JobRepository.getJobById(jobId),
                ApplicationRepository.applications,
                _isApplying,
            ) { job, applications, applying ->
                JobDetailUiState(
                    job = job?.toUi(),
                    isLoading = false,
                    notFound = job == null,
                    isApplied = applications.any {
                        it.jobId == jobId && it.studentId == CurrentUser.STUDENT_ID
                    },
                    isApplying = applying,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Event from UI: apply for this job (Module 3).
    // Takes a few seconds: the AI advisor runs before the insert, and
    // apply() is NonCancellable so leaving this screen cannot lose it.
    fun onApplyClick() {
        if (_isApplying.value) return // ignore double taps
        viewModelScope.launch {
            _isApplying.value = true
            ApplicationRepository.apply(jobId, CurrentUser.STUDENT_ID)
            _isApplying.value = false
        }
    }
}
