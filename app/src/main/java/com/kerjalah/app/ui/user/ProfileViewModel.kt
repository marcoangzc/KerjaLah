package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.Outcome
import com.kerjalah.app.data.UserRepository
import com.kerjalah.app.ui.common.asRetryableMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// [B] Module 1 - Profile logic layer (UDF). Shared by BOTH roles:
// the same ViewModel + screen serve student/profile and employer/profile.
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Collect the session: edits from Edit Profile appear instantly.
        viewModelScope.launch {
            UserRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    profile = user?.toUi(),
                    isLoading = false,
                    // user == null after logout/delete -> triggers navigation
                    loggedOut = user == null,
                )
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            UserRepository.logout()
        }
    }

    // A failed delete used to sign the user out anyway, which read as "account
    // deleted" right up until the same profile came back at the next login.
    // Now the session survives and the employer/student is told to try again.
    fun onDeleteAccountClick() {
        viewModelScope.launch {
            val result = UserRepository.deleteAccount()
            if (result is Outcome.Failure) {
                _uiState.value = _uiState.value.copy(
                    message = result.error.asRetryableMessage(),
                )
            }
        }
    }

    fun onRetry() = onDeleteAccountClick()

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
