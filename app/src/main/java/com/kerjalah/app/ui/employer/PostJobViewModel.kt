package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.CurrentUser
import com.kerjalah.app.data.FairWage
import com.kerjalah.app.data.Job
import com.kerjalah.app.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// [A] Module 2 - Post / Edit Job logic layer (UDF).
// jobId == null -> post new job; jobId != null -> edit existing job.
// The Fair-Wage Check gate lives HERE, not in the UI:
// the UI only renders state, the rule belongs to the logic layer.
class PostJobViewModel(private val jobId: String?) : ViewModel() {

    private val _uiState = MutableStateFlow(PostJobUiState(isEditMode = jobId != null))
    val uiState: StateFlow<PostJobUiState> = _uiState.asStateFlow()

    init {
        // Edit mode: pre-fill the form once from the repository.
        if (jobId != null) {
            viewModelScope.launch {
                val job = JobRepository.getJobById(jobId).first() ?: return@launch
                _uiState.value = _uiState.value.copy(
                    title = job.title,
                    companyName = job.companyName,
                    location = job.location,
                    payText = job.payPerHour.toString(),
                    hoursText = job.hoursPerWeek.toString(),
                    description = job.description,
                    fairWageOk = FairWage.isFair(job.payPerHour),
                )
            }
        }
    }

    // --- Events UP from the UI (one function per field, UDF style) ---

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, errorMessage = null)
    }

    fun onCompanyChange(value: String) {
        _uiState.value = _uiState.value.copy(companyName = value, errorMessage = null)
    }

    fun onLocationChange(value: String) {
        _uiState.value = _uiState.value.copy(location = value, errorMessage = null)
    }

    fun onPayChange(value: String) {
        // Live Fair-Wage Check while typing - instant feedback for the demo.
        val pay = value.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            payText = value,
            fairWageOk = pay?.let { FairWage.isFair(it) },
            errorMessage = null,
        )
    }

    fun onHoursChange(value: String) {
        _uiState.value = _uiState.value.copy(hoursText = value, errorMessage = null)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, errorMessage = null)
    }

    fun onSaveClick() {
        val state = _uiState.value

        // Basic validation first.
        if (state.title.isBlank() || state.companyName.isBlank() ||
            state.location.isBlank() || state.description.isBlank()
        ) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields.")
            return
        }
        val pay = state.payText.toDoubleOrNull()
        val hours = state.hoursText.toIntOrNull()
        if (pay == null || hours == null || pay <= 0 || hours <= 0) {
            _uiState.value = state.copy(errorMessage = "Pay and hours must be valid numbers.")
            return
        }

        // Fair-Wage Check gate: a posting below minimum wage can NOT go live.
        if (!FairWage.isFair(pay)) {
            _uiState.value = state.copy(
                errorMessage = "Fair-Wage Check failed: pay must be at least " +
                    "RM %.2f / hour (Malaysia minimum wage).".format(FairWage.MIN_HOURLY_RM),
            )
            return
        }

        val job = Job(
            id = jobId ?: "", // empty for a new job - the database generates the id
            employerId = CurrentUser.EMPLOYER_ID,
            title = state.title.trim(),
            companyName = state.companyName.trim(),
            location = state.location.trim(),
            payPerHour = pay,
            hoursPerWeek = hours,
            description = state.description.trim(),
        )
        // Supabase calls are suspend -> run in the ViewModel's scope.
        viewModelScope.launch {
            if (jobId == null) JobRepository.addJob(job) else JobRepository.updateJob(job)
            // Screen watches isSaved and navigates back (event, not direct nav).
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
