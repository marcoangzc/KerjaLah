package com.kerjalah.app.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kerjalah.app.data.AppError

// [B] A one-shot, transient notice for a Snackbar.
//
// Two rules come out of this type:
//  - It is built from an AppError, so the text is always one of our sentences.
//  - It is nullable state that the ViewModel clears the moment the snackbar is
//    gone, which is what stops "sticky" errors from following the user around
//    the screen after they have already moved on.
data class UserMessage(
    val text: String,
    val actionLabel: String? = null, // "Retry" when the action is worth repeating
)

fun AppError.asMessage(actionLabel: String? = null) = UserMessage(message, actionLabel)

fun AppError.asRetryableMessage() = UserMessage(message, actionLabel = "Retry")

// [B] Shows [message] once, then tells the ViewModel to drop it.
//
// Why a shared effect instead of copy-pasting LaunchedEffect into nine
// screens: the show-then-clear handshake is the part that is easy to get
// wrong, and getting it wrong is exactly how an error becomes permanent.
@Composable
fun SnackbarMessageEffect(
    message: UserMessage?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
    onAction: () -> Unit = {},
) {
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message.text,
            actionLabel = message.actionLabel,
            withDismissAction = message.actionLabel == null,
            duration = SnackbarDuration.Short,
        )
        onShown() // consumed - a recomposition must not show it again
        if (result == SnackbarResult.ActionPerformed) onAction()
    }
}
