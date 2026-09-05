package com.kerjalah.app.ui.user

import com.kerjalah.app.data.UserRole
import com.kerjalah.app.ui.common.UserMessage

// [B] UI = UI Elements + UI State.
// States for the whole auth flow: Splash -> Login -> Register -> Role.
//
// Two kinds of error live here on purpose, because Material 3 treats them
// differently:
//  - per-FIELD errors (nameError, emailError, ...) drive `isError` and
//    `supportingText` on the exact OutlinedTextField that is wrong;
//  - `message` is a transient action failure and goes to a Snackbar.
// A field error is not a snackbar and a snackbar is not a field error;
// mixing them is how "Please fill in all fields" ends up floating over the
// keyboard with no idea which field it means.

data class SplashUiState(
    val ready: Boolean = false,        // true after the short brand delay
    val loggedInRole: UserRole? = null, // skip login if a session exists
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val message: UserMessage? = null,
    // Not null after a successful login -> screen fires navigation event.
    val loggedInRole: UserRole? = null,
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val readyForRole: Boolean = false, // form ok -> go pick a role
)

data class RoleUiState(
    // Not null after account creation -> screen fires navigation event.
    val completedRole: UserRole? = null,
    val isWorking: Boolean = false,     // sign-up request in flight
    val message: UserMessage? = null,   // e.g. email already registered
)
