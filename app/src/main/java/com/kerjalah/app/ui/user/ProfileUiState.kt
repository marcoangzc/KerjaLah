package com.kerjalah.app.ui.user

import com.kerjalah.app.ui.common.UserMessage

// [B] States for Profile + Edit Profile (both roles share these).

data class ProfileUiState(
    val profile: ProfileUi? = null,
    val isLoading: Boolean = true,
    val loggedOut: Boolean = false, // logout or delete -> screen navigates away
    val message: UserMessage? = null, // e.g. "Delete account" could not go through
)

data class EditProfileUiState(
    val name: String = "",
    val orgLabel: String = "Organization", // "University" / "Company"
    val orgValue: String = "",
    val bio: String = "",
    val nameError: String? = null,   // field-level -> isError + supportingText
    val isSaving: Boolean = false,
    val message: UserMessage? = null, // action-level -> snackbar
    val isSaved: Boolean = false,
)
