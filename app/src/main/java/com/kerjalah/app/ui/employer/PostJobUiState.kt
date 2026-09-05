package com.kerjalah.app.ui.employer

import com.kerjalah.app.ui.common.UserMessage

// [A] State for the Post / Edit Job form.
// Every text field lives in state -> single source of truth (UDF).
//
// One errorMessage for the whole form used to sit under the description box
// saying "Please fill in all fields", which told the employer nothing about
// WHICH field. Each field now carries its own supporting text instead.
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
    val titleError: String? = null,
    val companyError: String? = null,
    val locationError: String? = null,
    val payError: String? = null,
    val hoursError: String? = null,
    val descriptionError: String? = null,
    val isSaving: Boolean = false,
    val message: UserMessage? = null, // the save itself failed -> snackbar
    val isSaved: Boolean = false,  // flips true after save -> screen navigates back
)
