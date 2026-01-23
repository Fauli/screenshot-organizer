package com.screenshotvault.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.screenshotvault.domain.model.ScreenshotItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToSettings: (() -> Unit)? = null,
    onItemClick: ((String) -> Unit)? = null,
    onTopicClick: ((String) -> Unit)? = null,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenshots = viewModel.screenshots.collectAsLazyPagingItems()

    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Feed (${screenshots.itemCount})")
                },
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        // Processing filter dropdown
                        Box {
                            FilterChip(
                                selected = uiState.processingFilter != ProcessingFilter.ALL,
                                onClick = { showFilterMenu = true },
                                label = {
                                    Text(
                                        when (uiState.processingFilter) {
                                            ProcessingFilter.ALL -> "All"
                                            ProcessingFilter.PROCESSED -> "Processed (${uiState.processedCount})"
                                            ProcessingFilter.PENDING -> "Pending (${uiState.pendingCount})"
                                        }
                                    )
                                },
                            )
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All (${uiState.totalCount})") },
                                    onClick = {
                                        viewModel.setProcessingFilter(ProcessingFilter.ALL)
                                        showFilterMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Processed (${uiState.processedCount})") },
                                    onClick = {
                                        viewModel.setProcessingFilter(ProcessingFilter.PROCESSED)
                                        showFilterMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Pending (${uiState.pendingCount})") },
                                    onClick = {
                                        viewModel.setProcessingFilter(ProcessingFilter.PENDING)
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = uiState.showSolved,
                            onClick = viewModel::toggleShowSolved,
                            label = { Text("Solved") },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = uiState.showNoContent,
                            onClick = viewModel::toggleShowNoContent,
                            label = {
                                Text(
                                    if (uiState.noContentCount > 0) "Empty (${uiState.noContentCount})"
                                    else "Empty"
                                )
                            },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            !uiState.hasFolderSelected -> {
                EmptyFolderState(
                    onSelectFolder = onNavigateToSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            screenshots.loadState.refresh is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            screenshots.itemCount == 0 -> {
                EmptyFeedState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            else -> {
                ScreenshotList(
                    screenshots = screenshots,
                    onItemClick = onItemClick ?: {},
                    onSolvedToggle = viewModel::toggleItemSolved,
                    onDelete = viewModel::deleteItem,
                    onTopicClick = onTopicClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun ScreenshotList(
    screenshots: LazyPagingItems<ScreenshotItem>,
    onItemClick: (String) -> Unit,
    onSolvedToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTopicClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = screenshots.itemCount,
            key = { index -> screenshots[index]?.id ?: index },
        ) { index ->
            val item = screenshots[index]
            if (item != null) {
                SwipeableScreenshotCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    onSolvedToggle = { solved -> onSolvedToggle(item.id, solved) },
                    onDelete = { onDelete(item.id) },
                    onTopicClick = onTopicClick,
                )
            }
        }

        // Loading more indicator
        if (screenshots.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderState(
    onSelectFolder: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No folder selected",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a screenshot folder in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onSelectFolder != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSelectFolder) {
                Text("Go to Settings")
            }
        }
    }
}

@Composable
private fun EmptyFeedState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No screenshots yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your folder in Settings to add screenshots",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
