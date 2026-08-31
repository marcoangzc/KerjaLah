package com.kerjalah.app.ui.application

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kerjalah.app.data.ApplicationStatus

// [B] Status chip shared by student AND employer screens.
// One source of truth for status colors = transparent tracking everywhere.
@Composable
fun StatusChip(status: ApplicationStatus) {
    val container = when (status) {
        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        ApplicationStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiaryContainer
        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (status) {
        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
        ApplicationStatus.ACCEPTED -> MaterialTheme.colorScheme.onTertiaryContainer
        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
    }
    val label = when (status) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
    }
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
