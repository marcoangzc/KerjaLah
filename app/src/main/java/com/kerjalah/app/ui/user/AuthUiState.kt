package com.kerjalah.app.ui.user

import com.kerjalah.app.data.UserRole

// [B] UI = UI Elements + UI State.
// States for the whole auth flow: Splash -> Login -> Register -> Role.

data class SplashUiState(
    val ready: Boolean = false,        // true after the short brand delay
    val loggedInRole: UserRole? = null, // skip login if a session exists
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    // Not null after a successful login -> screen fires navigation event.
    val loggedInRole: UserRole? = null,
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val readyForRole: Boolean = false, // form ok -> go pick a role
)

data class RoleUiState(
    // Not null after account creation -> screen fires navigation event.
    val completedRole: UserRole? = null,
    val isWorking: Boolean = false,     // sign-up request in flight
    val errorMessage: String? = null,   // e.g. email already registered
)
