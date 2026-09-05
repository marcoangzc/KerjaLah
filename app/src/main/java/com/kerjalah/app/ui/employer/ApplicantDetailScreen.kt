package com.kerjalah.app.ui.employer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerjalah.app.ui.application.StatusChip
import com.kerjalah.app.ui.common.SnackbarMessageEffect

// [B] Module 3 - Applicant Detail screen (employer side, UDF).
// The employer reviews the student and decides: accept or reject.
//
// The AI advisor card renders in every state - advice, missing, loading,
// failed - and never gates anything below it. That is the point: a Groq
// outage costs this screen one card, not the hiring decision.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantDetailScreen(
    appId: String,
    viewModel: ApplicantDetailViewModel = viewModel(key = "applicant-detail-$appId") {
        ApplicantDetailViewModel(appId)
    },
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    SnackbarMessageEffect(
        message = uiState.message,
        hostState = snackbarHostState,
        onShown = { viewModel.onMessageShown() },
        onAction = { viewModel.onRetryDecision() },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Applicant Detail") },
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
        val applicant = uiState.applicant
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.notFound || applicant == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("This application was withdrawn.")
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = applicant.studentName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (applicant.studentOrg.isNotBlank()) {
                                    Text(
                                        text = applicant.studentOrg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            StatusChip(status = applicant.status)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Applying for",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = applicant.jobTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "About the applicant",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = applicant.studentBio.ifBlank { "No bio yet." },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        // AI advisor card - employer-only, and self-contained:
                        // whatever state it is in, the buttons below still work.
                        Spacer(modifier = Modifier.height(16.dp))
                        AdvisorCard(
                            state = uiState.advisor,
                            onRetryClick = { viewModel.onAdvisorRetryClick() },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        // Human-in-the-loop: only these buttons change status.
                        Button(
                            onClick = { viewModel.onAcceptClick() },
                            enabled = !uiState.isDeciding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isDeciding) "Saving..." else "Accept")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.onRejectClick() },
                            enabled = !uiState.isDeciding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Reject",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The student sees your decision instantly " +
                                "in their My Applications tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// [B] The AI advisor card, in all four of its states.
//
// The old version simply did not exist when there was no advice, so a failed
// advisor was indistinguishable from an advisor that had never been asked -
// and the employer had no way to ask again.
@Composable
private fun AdvisorCard(
    state: AdvisorState,
    onRetryClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Suggestion",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (state) {
                is AdvisorState.Advice -> AdvisorAdvice(state)

                AdvisorState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Asking the advisor...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                AdvisorState.Missing -> AdvisorFallback(
                    text = "No AI suggestion was recorded for this application.",
                    actionLabel = "Ask the advisor",
                    onActionClick = onRetryClick,
                )

                is AdvisorState.Failed -> AdvisorFallback(
                    text = state.text,
                    actionLabel = "Retry",
                    onActionClick = onRetryClick,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI advice only - the decision is always yours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// [B] The advice itself. Wording matters: it SUGGESTS, the human decides -
// that is our human-in-the-loop rule for the whole app.
@Composable
private fun AdvisorAdvice(advice: AdvisorState.Advice) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${advice.matchPercent}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "match with this job",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            advice.label?.let {
                Text(
                    text = "Assessed as: $it",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    advice.reason?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = it, style = MaterialTheme.typography.bodyMedium)
    }
    if (advice.isLive) {
        // Honest label. The ai_* columns are immutable after insert, so a
        // re-run cannot be written back - see ApplicantDetailViewModel.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Fresh estimate, not saved to the application.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// [B] The graceful-degradation state: says what happened in plain words and
// offers exactly one way forward, without touching anything else on screen.
@Composable
private fun AdvisorFallback(
    text: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(
        onClick = onActionClick,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        Text(actionLabel)
    }
}
