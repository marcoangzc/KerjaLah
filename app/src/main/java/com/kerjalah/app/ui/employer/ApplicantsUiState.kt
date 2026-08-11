package com.kerjalah.app.ui.employer

import com.kerjalah.app.ui.application.ApplicantUi

// [B] UI = UI Elements + UI State (employer side of Module 3).

data class ApplicantsUiState(
    val jobTitle: String = "",
    val applicants: List<ApplicantUi> = emptyList(),
    val isLoading: Boolean = true,
)

data class ApplicantDetailUiState(
    val applicant: ApplicantUi? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
)
