package com.kerjalah.app.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.ApplicationRepository
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.data.UserRole
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

    // Separate flows so a combine emission can't wipe them.
    private val _isApplying = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            // currentUser is part of the combine on purpose. Reading the id
            // once inside the block used the CurrentUser fallback id whenever
            // the profile had not loaded yet, and nothing recomputed the flag
            // when it finally arrived - so an existing application could show
            // as "not applied" until something else changed.
            combine(
                JobRepository.getJobById(jobId),
                ApplicationRepository.applications,
                UserRepository.currentUser,
                _isApplying,
                _error,
            ) { job, applications, user, applying, error ->
                val studentId = user?.takeIf { it.role == UserRole.STUDENT }?.id
                JobDetailUiState(
                    job = job?.toUi(),
                    isLoading = false,
                    notFound = job == null,
                    isApplied = studentId != null && applications.any {
                        it.jobId == jobId && it.studentId == studentId
                    },
                    isApplying = applying,
                    errorMessage = error,
                )
            }.collect { _uiState.value = it }
        }
    }

    // Event from UI: apply for this job (Module 3).
    // Takes a few seconds: the AI advisor runs before the insert, and
    // apply() is NonCancellable so leaving this screen cannot lose it.
    fun onApplyClick() {
        if (_isApplying.value) return // ignore double taps

        // Never apply as the placeholder id: the row would be rejected by RLS
        // (student_id must equal auth.uid()) and the failure would look like
        // the button simply doing nothing.
        val studentId = UserRepository.currentUser.value
            ?.takeIf { it.role == UserRole.STUDENT }
            ?.id
        if (studentId == null) {
            _error.value = "Log in as a student to apply for this job."
            return
        }

        viewModelScope.launch {
            _isApplying.value = true
            _error.value = null
            val result = ApplicationRepository.apply(jobId, studentId)
            // A duplicate is not an error - the row already exists, and the
            // button flips to "Applied" from the applications stream.
            _error.value = when (result) {
                ApplicationRepository.ApplyResult.CREATED,
                ApplicationRepository.ApplyResult.DUPLICATE -> null
                ApplicationRepository.ApplyResult.FAILED ->
                    "Could not send your application. Check your connection and try again."
            }
            _isApplying.value = false
        }
    }
}
