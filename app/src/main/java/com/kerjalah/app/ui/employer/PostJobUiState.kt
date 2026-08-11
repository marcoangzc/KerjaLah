package com.kerjalah.app.ui.employer

// [A] State for the Post / Edit Job form.
// Every text field lives in state -> single source of truth (UDF).
data class PostJobUiState(
    val title: String = "",
    val companyName: String = "",
    val location: String = "",
    val payText: String = "",      // raw text; parsed to Double on check/save
    val hoursText: String = "",
    val description: String = "",
    val isEditMode: Boolean = false,
    // null = pay box still empty; true/false = live Fair-Wage Check result.
    val fairWageOk: Boolean? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,  // flips true after save -> screen navigates back
)
