package com.kerjalah.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerjalah.app.data.data.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// [B] Splash logic: brand moment + wait for Supabase to restore
// any saved session, then tell the screen where to go.
class SplashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1200) // brand moment
            // Wait until the session check finished (max 4s, then give up
            // and go to login - covers the offline case gracefully).
            withTimeoutOrNull(4000) {
                UserRepository.sessionChecked.first { it }
            }
            _uiState.value = SplashUiState(
                ready = true,
                loggedInRole = UserRepository.currentUser.value?.role,
            )
        }
    }
}
