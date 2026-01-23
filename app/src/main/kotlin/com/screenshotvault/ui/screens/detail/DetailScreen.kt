package com.screenshotvault.ui.screens.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.screenshotvault.data.db.entities.ProcessingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    screenshotId: String,
    onNavigateBack: () -> Unit,
    onTopicClick: ((String) -> Unit)? = null,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screenshot Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.screenshot?.let { item ->
                        IconButton(
                            onClick = { viewModel.toggleSolved() },
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
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.screenshot == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Screenshot not found")
                }
            }
            else -> {
                val item = uiState.screenshot!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Preview Image with processing overlay
                    Card {
                        Box {
                            AsyncImage(
                                model = Uri.parse(item.contentUri),
                                contentDescription = item.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop,
                            )
                            // Processing overlay
                            if (item.status == ProcessingStatus.NEW || item.status == ProcessingStatus.PROCESSING) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = if (item.status == ProcessingStatus.PROCESSING) {
                                                "Processing..."
                                            } else {
                                                "Waiting to process..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.openOriginal() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Original")
                        }
                        OutlinedButton(
                            onClick = { viewModel.reprocess() },
                            modifier = Modifier.weight(1f),
                            enabled = item.status != ProcessingStatus.PROCESSING,
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reprocess")
                        }
                    }

                    // Status
                    if (item.status != ProcessingStatus.DONE) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                when (item.status) {
                                    ProcessingStatus.NEW, ProcessingStatus.PROCESSING -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                    ProcessingStatus.FAILED -> {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    else -> {}
                                }
                                Column {
                                    Text(
                                        text = when (item.status) {
                                            ProcessingStatus.NEW -> "Waiting to process..."
                                            ProcessingStatus.PROCESSING -> "Processing with AI..."
                                            ProcessingStatus.FAILED -> "Processing failed"
                                            ProcessingStatus.DONE -> ""
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.status == ProcessingStatus.FAILED) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                    if (item.status == ProcessingStatus.NEW) {
                                        Text(
                                            text = "Tap a batch button in Settings to start",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    item.errorMessage?.let { error ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Essence Content
                    item.essence?.let { essence ->
                        // Title
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = essence.title,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }

                        // Summary
                        if (essence.summaryBullets.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Summary",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    essence.summaryBullets.forEach { bullet ->
                                        Text(
                                            text = "• $bullet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Topics
                        if (essence.topics.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Topics",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        essence.topics.forEach { topic ->
                                            AssistChip(
                                                onClick = { onTopicClick?.invoke(topic) },
                                                label = { Text(topic) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Entities
                        if (essence.entities.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Entities",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    essence.entities.forEach { entity ->
                                        Text(
                                            text = "${entity.kind.name}: ${entity.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Confidence
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Extraction Info",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Confidence: ${(essence.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    // Metadata
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Metadata",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

                            MetadataRow("File", item.displayName)
                            MetadataRow("Captured", dateFormat.format(Date(item.capturedAt)))
                            MetadataRow("Size", "${item.width} x ${item.height}")
                            item.domain?.let { MetadataRow("Domain", it) }
                            item.type?.let { MetadataRow("Type", it) }
                        }
                    }
                }
            }
        }
        }

        // Full-screen image viewer overlay
        if (uiState.showOriginal && uiState.screenshot != null) {
            FullScreenImageViewer(
                contentUri = uiState.screenshot!!.contentUri,
                onDismiss = { viewModel.closeOriginal() },
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FullScreenImageViewer(
    contentUri: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Uri.parse(contentUri),
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit,
        )

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
