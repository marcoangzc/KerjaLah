package com.kerjalah.app.ui.user

// [B] States for Profile + Edit Profile (both roles share these).

data class ProfileUiState(
    val profile: ProfileUi? = null,
    val isLoading: Boolean = true,
    val loggedOut: Boolean = false, // logout or delete -> screen navigates away
)

data class EditProfileUiState(
    val name: String = "",
    val orgLabel: String = "Organization", // "University" / "Company"
    val orgValue: String = "",
    val bio: String = "",
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)
