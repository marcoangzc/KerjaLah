package com.kerjalah.app.ui.job

// [A] UI = UI Elements + UI State.
// These classes are the single "State" each job screen renders from.

data class JobListUiState(
    val jobs: List<JobUi> = emptyList(),
    val isLoading: Boolean = true,
)

data class JobDetailUiState(
    val job: JobUi? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false, // true when job was deleted while viewing
    val isApplied: Boolean = false, // current student already applied (Module 3)
    val isApplying: Boolean = false, // apply + AI check in flight
)
