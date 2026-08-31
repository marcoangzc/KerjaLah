package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Login logic layer (UDF).
// Supabase call is suspend -> runs inside viewModelScope.
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter email and password.")
            return
        }
        viewModelScope.launch {
            val role = UserRepository.login(state.email, state.password)
            _uiState.value = if (role != null) {
                _uiState.value.copy(loggedInRole = role)
            } else {
                _uiState.value.copy(
                    errorMessage = "Login failed. Check email, password and connection.",
                )
            }
        }
    }
}
