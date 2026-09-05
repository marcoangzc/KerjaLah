package com.kerjalah.app.ui.job

import com.kerjalah.app.ui.common.UserMessage

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
    // A rejected apply is a transient action failure -> Snackbar, not a
    // permanent red paragraph wedged above the button. The old inline text
    // stayed on screen until the next apply attempt, so a student who had
    // already read it, understood it and moved on kept being told off.
    val message: UserMessage? = null,
)
