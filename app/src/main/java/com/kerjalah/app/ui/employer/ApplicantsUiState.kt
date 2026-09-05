package com.kerjalah.app.ui.employer

import com.kerjalah.app.ui.application.ApplicantUi
import com.kerjalah.app.ui.common.UserMessage

// [B] UI = UI Elements + UI State (employer side of Module 3).

data class ApplicantsUiState(
    val jobTitle: String = "",
    val applicants: List<ApplicantUi> = emptyList(),
    val isLoading: Boolean = true,
)

// [B] The AI advisor card is its own little state machine, deliberately
// separate from the rest of the screen.
//
// This is the "graceful degradation" rule made explicit: a Groq outage may
// cost the employer the suggestion card and nothing else. Accept and Reject
// stay live in every one of these states, because the human decision has
// never depended on the AI.
sealed interface AdvisorState {

    /** Advice we have. [isLive] = re-run just now rather than stored at apply time. */
    data class Advice(
        val matchPercent: Int,
        val label: String?,
        val reason: String?,
        val isLive: Boolean = false,
    ) : AdvisorState

    /** The application carries no advice - the advisor was down when it was filed. */
    data object Missing : AdvisorState

    /** A re-run is in flight. */
    data object Loading : AdvisorState

    /** The re-run failed. [text] is already user-readable; Retry is offered. */
    data class Failed(val text: String) : AdvisorState
}

data class ApplicantDetailUiState(
    val applicant: ApplicantUi? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val advisor: AdvisorState = AdvisorState.Missing,
    val isDeciding: Boolean = false,   // accept/reject in flight
    val message: UserMessage? = null,  // a decision that did not save
)
