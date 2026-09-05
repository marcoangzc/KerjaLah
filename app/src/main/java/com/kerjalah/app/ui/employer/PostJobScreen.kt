package com.kerjalah.app.ui.employer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerjalah.app.data.FairWage
import com.kerjalah.app.ui.common.SnackbarMessageEffect

// [A] Module 2 - Post / Edit Job screen (UI Layer, UDF).
// Every keystroke goes UP to the ViewModel (onXxxChange),
// new state flows DOWN and re-renders the fields.
//
// Validation renders per field (isError + supportingText). Only a failed
// SAVE - the part that can fail for reasons outside this form - gets a
// snackbar, with Retry.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    jobId: String?, // null = post new, not null = edit
    viewModel: PostJobViewModel = viewModel(key = "post-job-${jobId ?: "new"}") {
        PostJobViewModel(jobId)
    },
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // React to "saved" state with a side effect -> navigate back.
    // Why LaunchedEffect: navigation is an event, not part of rendering.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    SnackbarMessageEffect(
        message = uiState.message,
        hostState = snackbarHostState,
        onShown = { viewModel.onMessageShown() },
        onAction = { viewModel.onRetry() },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Job" else "Post a Job") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp) // form stays readable on tablets
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text("Job title") },
                    singleLine = true,
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.companyName,
                    onValueChange = { viewModel.onCompanyChange(it) },
                    label = { Text("Company name") },
                    singleLine = true,
                    isError = uiState.companyError != null,
                    supportingText = uiState.companyError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = { viewModel.onLocationChange(it) },
                    label = { Text("Location (near which campus?)") },
                    singleLine = true,
                    isError = uiState.locationError != null,
                    supportingText = uiState.locationError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.payText,
                        onValueChange = { viewModel.onPayChange(it) },
                        label = { Text("Pay (RM / hour)") },
                        singleLine = true,
                        // The Fair-Wage Check verdict is a fact about THIS
                        // field, so it is reported on this field.
                        isError = uiState.payError != null,
                        supportingText = uiState.payError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = uiState.hoursText,
                        onValueChange = { viewModel.onHoursChange(it) },
                        label = { Text("Hours / week") },
                        singleLine = true,
                        isError = uiState.hoursError != null,
                        supportingText = uiState.hoursError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Live Fair-Wage Check feedback (the KerjaLah killer feature).
                when (uiState.fairWageOk) {
                    true -> FairWageStatusCard(
                        ok = true,
                        text = "Fair-Wage Check passed. This job can go live.",
                    )
                    false -> FairWageStatusCard(
                        ok = false,
                        text = "Below RM %.2f / hour (Malaysia minimum wage). "
                            .format(FairWage.MIN_HOURLY_RM) +
                            "This posting cannot go live.",
                    )
                    null -> { /* pay box empty - show nothing yet */ }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    label = { Text("Job description") },
                    minLines = 4,
                    isError = uiState.descriptionError != null,
                    supportingText = uiState.descriptionError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.onSaveClick() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Saving..."
                            uiState.isEditMode -> "Save Changes"
                            else -> "Publish Job"
                        },
                    )
                }
            }
        }
    }
}

// [A] Small status card for the live Fair-Wage Check result.
@Composable
private fun FairWageStatusCard(ok: Boolean, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (ok) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
    }
}
