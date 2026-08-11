package com.kerjalah.app.ui.application

// [B] UI = UI Elements + UI State (student side of Module 3).

data class MyApplicationsUiState(
    val applications: List<ApplicationUi> = emptyList(),
    val isLoading: Boolean = true,
)

data class ApplicationDetailUiState(
    val application: ApplicationUi? = null,
    val isLoading: Boolean = true,
    val closed: Boolean = false, // withdrawn or gone -> navigate back
)
