package com.kerjalah.app.ui.user

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.kerjalah.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// [B] Module 1 - Register logic layer (UDF).
// Step 1 of 2: validate the form, park the data in the repository.
// The actual Supabase sign-up happens on the Role screen (step 2),
// so a duplicate-email error also surfaces there.
//
// Nothing here touches the network, so nothing here needs a snackbar: every
// message this screen can produce belongs to one specific field.
class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null)
    }

    // Editing either password box also clears the "must match" complaint on the
    // other one: that message was about the pair, not about one field.
    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordError = null,
            confirmPasswordError = null,
        )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = value,
            confirmPasswordError = null,
        )
    }

    fun onContinueClick() {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Please enter your name." else null
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()
        val emailError = if (emailValid) null else "Please enter a valid email address."
        val passwordError = if (state.password.length < 6) "Use at least 6 characters." else null
        val confirmError =
            if (state.password != state.confirmPassword) "Both passwords must match." else null

        if (nameError != null || emailError != null ||
            passwordError != null || confirmError != null
        ) {
            _uiState.value = state.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
            )
            return
        }

        UserRepository.setPendingRegistration(
            name = state.name,
            email = state.email,
            password = state.password,
        )
        _uiState.value = state.copy(
            nameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            readyForRole = true,
        )
    }

    // Called after navigation fired, so coming back doesn't re-trigger it.
    fun onNavigatedToRole() {
        _uiState.value = _uiState.value.copy(readyForRole = false)
    }
}
