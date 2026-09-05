package com.kerjalah.app.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerjalah.app.data.UserRole
import com.kerjalah.app.ui.common.SnackbarMessageEffect

// [B] Module 1 - Role selection screen, register step 2 of 2 (UI Layer, UDF).
// One tap = one event up; account creation happens in the ViewModel.
@Composable
fun RoleScreen(
    viewModel: RoleViewModel = viewModel(),
    onRoleConfirmed: (UserRole) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.completedRole) {
        uiState.completedRole?.let { onRoleConfirmed(it) }
    }

    // Sign-up failures used to print raw exception text under the two cards -
    // "RestException ... user_already_exists" and worse - and then stay there.
    // Now they are one plain sentence in a snackbar that clears itself.
    SnackbarMessageEffect(
        message = uiState.message,
        hostState = snackbarHostState,
        onShown = { viewModel.onMessageShown() },
        onAction = { viewModel.onRetry() },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "How will you use KerjaLah?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                RoleCard(
                    title = "I'm a Student",
                    subtitle = "Browse verified part-time jobs near campus and apply.",
                    icon = Icons.Filled.Person,
                    enabled = !uiState.isWorking,
                    onClick = { viewModel.onRoleSelected(UserRole.STUDENT) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                RoleCard(
                    title = "I'm an Employer",
                    subtitle = "Post fair-wage jobs and manage applicants.",
                    icon = Icons.Filled.AccountCircle,
                    enabled = !uiState.isWorking,
                    onClick = { viewModel.onRoleSelected(UserRole.EMPLOYER) },
                )

                if (uiState.isWorking) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Creating your account...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// [B] One tappable role option.
@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
