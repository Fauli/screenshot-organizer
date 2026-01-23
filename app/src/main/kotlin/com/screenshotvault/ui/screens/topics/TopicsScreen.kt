package com.screenshotvault.ui.screens.topics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenshotvault.data.repository.TopicWithCount
import com.screenshotvault.ui.screens.feed.SwipeableScreenshotCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    onItemClick: ((String) -> Unit)? = null,
    onTopicClick: ((String) -> Unit)? = null,
    initialTopic: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: TopicsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Select initial topic if provided
    androidx.compose.runtime.LaunchedEffect(initialTopic) {
        if (initialTopic != null) {
            viewModel.selectTopic(initialTopic)
        }
    }

    // Handle system back button when viewing topic detail
    BackHandler(enabled = uiState.selectedTopic != null) {
        if (initialTopic != null && onNavigateBack != null) {
            // If we came from a deep link, go back in nav stack
            onNavigateBack()
        } else {
            viewModel.clearSelectedTopic()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.selectedTopic ?: "Topics")
                },
                navigationIcon = {
                    if (uiState.selectedTopic != null) {
                        IconButton(
                            onClick = {
                                if (initialTopic != null && onNavigateBack != null) {
                                    onNavigateBack()
                                } else {
                                    viewModel.clearSelectedTopic()
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            uiState.selectedTopic != null -> {
                // Show screenshots for selected topic
                TopicDetailContent(
                    screenshots = uiState.topicScreenshots,
                    onItemClick = onItemClick,
                    onSolvedToggle = viewModel::toggleItemSolved,
                    onDelete = viewModel::deleteItem,
                    onTopicClick = onTopicClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            uiState.topics.isEmpty() -> {
                EmptyTopicsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            else -> {
                TopicsListContent(
                    topics = uiState.topics,
                    onTopicClick = viewModel::selectTopic,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsListContent(
    topics: List<TopicWithCount>,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = topics,
            key = { it.name },
        ) { topic ->
            TopicCard(
                topic = topic,
                onClick = { onTopicClick(topic.name) },
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: TopicWithCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Tag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = topic.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun TopicDetailContent(
    screenshots: List<com.screenshotvault.domain.model.ScreenshotItem>,
    onItemClick: ((String) -> Unit)?,
    onSolvedToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTopicClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (screenshots.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No screenshots with this topic",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = screenshots,
                key = { it.id },
            ) { item ->
                SwipeableScreenshotCard(
                    item = item,
                    onClick = { onItemClick?.invoke(item.id) },
                    onSolvedToggle = { solved -> onSolvedToggle(item.id, solved) },
                    onDelete = { onDelete(item.id) },
                    onTopicClick = onTopicClick,
                )
            }
        }
    }
}

@Composable
private fun EmptyTopicsState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No topics yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Topics will appear after processing screenshots",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
