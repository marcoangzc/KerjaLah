package com.kerjalah.app.ui.user

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.kerjalah.app.data.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// [B] Module 1 - Register logic layer (UDF).
// Step 1 of 2: validate the form, park the data in the repository.
// The actual Supabase sign-up happens on the Role screen (step 2),
// so a duplicate-email error also surfaces there.
class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errorMessage = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun onContinueClick() {
        val state = _uiState.value
        val error = when {
            state.name.isBlank() -> "Please enter your name."
            !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
                "Please enter a valid email."
            state.password.length < 6 -> "Password needs at least 6 characters."
            state.password != state.confirmPassword -> "Passwords do not match."
            else -> null
        }
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }
        UserRepository.setPendingRegistration(
            name = state.name,
            email = state.email,
            password = state.password,
        )
        _uiState.value = state.copy(readyForRole = true)
    }

    // Called after navigation fired, so coming back doesn't re-trigger it.
    fun onNavigatedToRole() {
        _uiState.value = _uiState.value.copy(readyForRole = false)
    }
}
