package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.UserRepository
import com.kerjalah.app.data.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Role selection logic layer, register step 2 of 2 (UDF).
// Picking a role runs the real Supabase sign-up + profile insert.
class RoleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoleUiState())
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun onRoleSelected(role: UserRole) {
        if (_uiState.value.isWorking) return // ignore double taps
        _uiState.value = _uiState.value.copy(isWorking = true, errorMessage = null)
        viewModelScope.launch {
            val error = UserRepository.completeRegistration(role)
            _uiState.value = if (error == null) {
                RoleUiState(completedRole = role)
            } else {
                RoleUiState(errorMessage = error)
            }
        }
    }
}
