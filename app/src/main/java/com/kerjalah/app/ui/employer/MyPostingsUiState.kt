package com.kerjalah.app.ui.employer

import com.kerjalah.app.ui.common.UserMessage
import com.kerjalah.app.ui.job.JobUi

// [A] State for the employer's "My Postings" screen.
data class MyPostingsUiState(
    val postings: List<JobUi> = emptyList(),
    val isLoading: Boolean = true,
    val message: UserMessage? = null, // a delete that did not go through
)
