package com.screenshotvault.ui.screens.feed

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.domain.model.ScreenshotItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenshotCard(
    item: ScreenshotItem,
    onClick: () -> Unit,
    onSolvedToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTopicClick: ((String) -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
        ) {
            // Thumbnail with no-content indicator
            Box {
                AsyncImage(
                    model = Uri.parse(item.contentUri),
                    contentDescription = item.displayName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    alpha = if (item.noContent) 0.5f else 1f,
                )
                if (item.noContent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.HideImage,
                            contentDescription = "No content",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Title or filename
                val title = item.essence?.title ?: item.displayName
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Domain or date (or "No content" label)
                val subtitle = when {
                    item.noContent -> "No content • ${formatDate(item.capturedAt)}"
                    item.domain != null -> item.domain
                    else -> formatDate(item.capturedAt)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.noContent) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                // Summary bullets (if available)
                item.essence?.summaryBullets?.take(2)?.forEach { bullet ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• $bullet",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Topics
                item.essence?.topics?.let { topics ->
                    if (topics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            topics.take(3).forEach { topic ->
                                androidx.compose.material3.SuggestionChip(
                                    onClick = { onTopicClick?.invoke(topic) },
                                    label = {
                                        Text(
                                            text = topic,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    modifier = Modifier.height(24.dp),
                                )
                            }
                            if (topics.size > 3) {
                                Text(
                                    text = "+${topics.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                            }
                        }
                    }
                }
            }

            // Status & Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Processing status indicator
                when (item.status) {
                    ProcessingStatus.PROCESSING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    ProcessingStatus.NEW -> {
                        Icon(
                            imageVector = Icons.Default.Pending,
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    ProcessingStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    ProcessingStatus.DONE -> {
                        // No indicator needed
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Solved toggle
                IconButton(
                    onClick = { onSolvedToggle(!item.solved) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (item.solved) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                        contentDescription = if (item.solved) "Mark unsolved" else "Mark solved",
                        tint = if (item.solved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableScreenshotCard(
    item: ScreenshotItem,
    onClick: () -> Unit,
    onSolvedToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onTopicClick: ((String) -> Unit)? = null,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSolvedDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right -> show solved confirmation
                    showSolvedDialog = true
                    false // Reset the swipe, don't dismiss
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left -> show delete confirmation
                    showDeleteDialog = true
                    false // Don't dismiss yet, wait for confirmation
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    // Solved confirmation dialog
    if (showSolvedDialog) {
        AlertDialog(
            onDismissRequest = { showSolvedDialog = false },
            title = { Text("Mark as Solved") },
            text = { Text("Mark this screenshot as solved?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSolvedDialog = false
                        onSolvedToggle(true)
                    },
                ) {
                    Text("Mark Solved")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSolvedDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Screenshot") },
            text = { Text("Are you sure you want to delete this screenshot? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for solved
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error // Red for delete
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipe_color",
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Done
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        },
        content = {
            ScreenshotCard(
                item = item,
                onClick = onClick,
                onSolvedToggle = onSolvedToggle,
                onTopicClick = onTopicClick,
            )
        },
    )
}
