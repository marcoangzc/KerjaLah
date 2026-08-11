package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.UserRepository
import com.kerjalah.app.data.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Edit Profile logic layer (UDF). Shared by both roles.
class EditProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        // Pre-fill the form once from the current session.
        val user = UserRepository.currentUser.value
        if (user != null) {
            _uiState.value = EditProfileUiState(
                name = user.name,
                orgLabel = if (user.role == UserRole.STUDENT) "University" else "Company",
                orgValue = user.organization,
                bio = user.bio,
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errorMessage = null)
    }

    fun onOrgChange(value: String) {
        _uiState.value = _uiState.value.copy(orgValue = value, errorMessage = null)
    }

    fun onBioChange(value: String) {
        _uiState.value = _uiState.value.copy(bio = value, errorMessage = null)
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Name cannot be empty.")
            return
        }
        // Supabase call is suspend -> run in the ViewModel's scope.
        viewModelScope.launch {
            UserRepository.updateProfile(
                name = state.name,
                organization = state.orgValue,
                bio = state.bio,
            )
            _uiState.value = state.copy(isSaved = true)
        }
    }
}
