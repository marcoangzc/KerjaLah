package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.AppError
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.data.UserRole
import com.kerjalah.app.ui.common.asMessage
import com.kerjalah.app.ui.common.asRetryableMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Role selection logic layer, register step 2 of 2 (UDF).
// Picking a role runs the real Supabase sign-up + profile insert.
class RoleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoleUiState())
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    // Remembered so the snackbar's Retry knows what to repeat.
    private var lastRole: UserRole? = null

    fun onRoleSelected(role: UserRole) {
        if (_uiState.value.isWorking) return // ignore double taps
        lastRole = role
        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch {
            when (val result = UserRepository.completeRegistration(role)) {
                is Outcome.Success -> _uiState.value = RoleUiState(completedRole = role)
                is Outcome.Failure -> _uiState.value =
                    RoleUiState(message = result.error.toSignUpMessage())
            }
        }
    }

    fun onRetry() {
        lastRole?.let { onRoleSelected(it) }
    }

    /** The snackbar has been shown; drop it so it cannot reappear. */
    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    // An already-registered email or a disabled sign-up will fail identically
    // on a second try, so those get no Retry action - only transient faults do.
    private fun AppError.toSignUpMessage() = when (this) {
        AppError.EmailTaken,
        AppError.SignUpDisabled,
        AppError.WeakPassword,
        AppError.InvalidEmail,
        AppError.EmailNotConfirmed,
        AppError.RegistrationExpired,
        -> asMessage()
        else -> asRetryableMessage()
    }
}
