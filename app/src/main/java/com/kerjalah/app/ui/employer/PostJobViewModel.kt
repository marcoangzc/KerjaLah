package com.kerjalah.app.ui.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.CurrentUser
import com.kerjalah.app.data.FairWage
import com.kerjalah.app.data.Job
import com.kerjalah.app.data.JobRepository
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.ui.common.asRetryableMessage
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
    // Each one clears only its own field error: fixing the pay box must not
    // erase the complaint about an empty title before it has been read.

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, titleError = null)
    }

    fun onCompanyChange(value: String) {
        _uiState.value = _uiState.value.copy(companyName = value, companyError = null)
    }

    fun onLocationChange(value: String) {
        _uiState.value = _uiState.value.copy(location = value, locationError = null)
    }

    fun onPayChange(value: String) {
        // Live Fair-Wage Check while typing - instant feedback for the demo.
        val pay = value.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(
            payText = value,
            fairWageOk = pay?.let { FairWage.isFair(it) },
            payError = null,
        )
    }

    fun onHoursChange(value: String) {
        _uiState.value = _uiState.value.copy(hoursText = value, hoursError = null)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, descriptionError = null)
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun onRetry() = onSaveClick()

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return // ignore double taps

        val pay = state.payText.toDoubleOrNull()
        val hours = state.hoursText.toIntOrNull()

        // Everything wrong with the form, reported at once and per field.
        val validated = state.copy(
            titleError = if (state.title.isBlank()) "Give the job a title." else null,
            companyError = if (state.companyName.isBlank()) "Name the company." else null,
            locationError = if (state.location.isBlank()) "Say where the work is." else null,
            descriptionError =
                if (state.description.isBlank()) "Describe the work briefly." else null,
            payError = payProblem(pay),
            hoursError = if (hours == null || hours <= 0) {
                "Enter whole hours, e.g. 12."
            } else {
                null
            },
        )
        val hasFieldError = listOf(
            validated.titleError,
            validated.companyError,
            validated.locationError,
            validated.descriptionError,
            validated.payError,
            validated.hoursError,
        ).any { it != null }

        if (hasFieldError || pay == null || hours == null) {
            _uiState.value = validated
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
        _uiState.value = validated.copy(isSaving = true, message = null)
        // Supabase calls are suspend -> run in the ViewModel's scope.
        viewModelScope.launch {
            val result = if (jobId == null) {
                JobRepository.addJob(job)
            } else {
                JobRepository.updateJob(job)
            }
            // isSaved used to flip whatever the repository did, so a rejected
            // write still bounced the employer back to a list without the job.
            _uiState.value = when (result) {
                is Outcome.Success -> _uiState.value.copy(isSaving = false, isSaved = true)
                is Outcome.Failure -> _uiState.value.copy(
                    isSaving = false,
                    message = result.error.asRetryableMessage(),
                )
            }
        }
    }

    // The Fair-Wage Check gate: a posting below minimum wage can NOT go live.
    // It is a fact about the pay field, so it is reported on the pay field.
    private fun payProblem(pay: Double?): String? = when {
        pay == null || pay <= 0 -> "Enter the hourly pay, e.g. 10.50."
        !FairWage.isFair(pay) ->
            "Fair-Wage Check: pay at least RM %.2f / hour (Malaysia minimum wage)."
                .format(FairWage.MIN_HOURLY_RM)
        else -> null
    }
}
