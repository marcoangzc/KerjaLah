package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.AppError
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.ui.common.asMessage
import com.kerjalah.app.ui.common.asRetryableMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Login logic layer (UDF).
// Supabase call is suspend -> runs inside viewModelScope.
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Typing in a field clears THAT field's error and nothing else. Clearing
    // everything on every keystroke made a second problem vanish before the
    // user had read it.
    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null)
    }

    /** The snackbar is gone; drop the message so it cannot come back. */
    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isSubmitting) return // ignore double taps

        // Validate every field at once, so the form shows all of its problems
        // instead of one at a time.
        val emailError = if (state.email.isBlank()) "Enter your email." else null
        val passwordError = if (state.password.isBlank()) "Enter your password." else null
        if (emailError != null || passwordError != null) {
            _uiState.value = state.copy(
                emailError = emailError,
                passwordError = passwordError,
            )
            return
        }

        _uiState.value = state.copy(
            isSubmitting = true,
            emailError = null,
            passwordError = null,
            message = null,
        )
        viewModelScope.launch {
            when (val result = UserRepository.login(state.email, state.password)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    loggedInRole = result.value,
                )
                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = result.error.toLoginMessage(),
                )
            }
        }
    }

    // Bad credentials are not worth a Retry button - the same inputs fail the
    // same way. Everything else is worth one tap.
    private fun AppError.toLoginMessage() =
        if (this == AppError.InvalidCredentials) asMessage() else asRetryableMessage()
}
