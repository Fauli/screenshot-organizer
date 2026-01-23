package com.screenshotvault.ui.screens.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenshotvault.data.db.dao.ActionCount
import com.screenshotvault.data.db.dao.ContentTypeCount
import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.data.db.dao.TimePeriodCount
import com.screenshotvault.domain.model.ScreenshotItem
import com.screenshotvault.domain.model.TimePeriod
import com.screenshotvault.ui.screens.feed.SwipeableScreenshotCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onItemClick: ((String) -> Unit)? = null,
    onTopicClick: ((String) -> Unit)? = null,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.selection != null) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(getSelectionTitle(uiState.selection) ?: "Insights")
                },
                navigationIcon = {
                    if (uiState.selection != null) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (uiState.selection == null) {
                        FilterChip(
                            selected = uiState.includeSolved,
                            onClick = viewModel::toggleIncludeSolved,
                            label = { Text("Inc. Solved") },
                            modifier = Modifier.padding(end = 8.dp),
                        )
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
            uiState.selection != null -> {
                SelectionDetailContent(
                    screenshots = uiState.selectionScreenshots,
                    isLoading = uiState.isLoadingSelection,
                    onItemClick = onItemClick,
                    onSolvedToggle = viewModel::toggleItemSolved,
                    onDelete = viewModel::deleteItem,
                    onTopicClick = onTopicClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            else -> {
                InsightsDashboardContent(
                    uiState = uiState,
                    onTabSelect = viewModel::selectTab,
                    onActionClick = viewModel::selectAction,
                    onContentTypeClick = viewModel::selectContentType,
                    onTimePeriodClick = viewModel::selectTimePeriod,
                    onDomainClick = viewModel::selectDomain,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

private fun getSelectionTitle(selection: InsightsSelection?): String? = when (selection) {
    is InsightsSelection.Action -> selection.displayName
    is InsightsSelection.ContentType -> selection.displayName
    is InsightsSelection.Time -> selection.period.displayName()
    is InsightsSelection.Source -> selection.domain
    null -> null
}

@Composable
private fun InsightsDashboardContent(
    uiState: InsightsUiState,
    onTabSelect: (InsightsTab) -> Unit,
    onActionClick: (String, String) -> Unit,
    onContentTypeClick: (String, String) -> Unit,
    onTimePeriodClick: (TimePeriod) -> Unit,
    onDomainClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Stats row
        StatsRow(
            totalCount = uiState.totalCount,
            thisWeekCount = uiState.thisWeekCount,
            pendingCount = uiState.pendingCount,
            modifier = Modifier.fillMaxWidth(),
        )

        // Inner tabs
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
        ) {
            InsightsTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelect(tab) },
                    text = { Text(tab.displayName()) },
                )
            }
        }

        // Tab content
        when (uiState.selectedTab) {
            InsightsTab.ACTIONS -> {
                ActionsTabContent(
                    actions = uiState.actionCounts,
                    onActionClick = onActionClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            InsightsTab.TYPES -> {
                TypesTabContent(
                    types = uiState.contentTypeCounts,
                    onTypeClick = onContentTypeClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            InsightsTab.TIME -> {
                TimeTabContent(
                    periods = uiState.timePeriodCounts,
                    onPeriodClick = onTimePeriodClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            InsightsTab.SOURCES -> {
                SourcesTabContent(
                    domains = uiState.domainCounts,
                    onDomainClick = onDomainClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    totalCount: Int,
    thisWeekCount: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                value = totalCount.toString(),
                label = "total",
            )
            StatItem(
                value = thisWeekCount.toString(),
                label = "this week",
            )
            StatItem(
                value = if (pendingCount > 0) pendingCount.toString() else "-",
                label = "pending",
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionsTabContent(
    actions: List<ActionCount>,
    onActionClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Default.Dashboard,
            message = "No actions yet",
            description = "Actions will appear after processing screenshots",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = actions,
                key = { it.action },
            ) { action ->
                val displayInfo = getActionDisplayInfo(action.action)
                CategoryCard(
                    icon = displayInfo.icon,
                    title = displayInfo.displayName,
                    count = action.count,
                    onClick = { onActionClick(action.action, displayInfo.displayName) },
                )
            }
        }
    }
}

@Composable
private fun TypesTabContent(
    types: List<ContentTypeCount>,
    onTypeClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (types.isEmpty()) {
        EmptyTabContent(
            icon = Icons.AutoMirrored.Filled.Article,
            message = "No content types yet",
            description = "Content types will appear after processing screenshots",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = types,
                key = { it.contentType },
            ) { type ->
                val displayInfo = getContentTypeDisplayInfo(type.contentType)
                CategoryCard(
                    icon = displayInfo.icon,
                    title = displayInfo.displayName,
                    count = type.count,
                    onClick = { onTypeClick(type.contentType, displayInfo.displayName) },
                )
            }
        }
    }
}

@Composable
private fun TimeTabContent(
    periods: List<TimePeriodCount>,
    onPeriodClick: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (periods.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Default.AccessTime,
            message = "No screenshots yet",
            description = "Time periods will appear after adding screenshots",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort periods in logical order
            val sortedPeriods = periods.sortedBy {
                when (it.period) {
                    "TODAY" -> 0
                    "THIS_WEEK" -> 1
                    "THIS_MONTH" -> 2
                    else -> 3
                }
            }
            items(
                items = sortedPeriods,
                key = { it.period },
            ) { period ->
                val timePeriod = TimePeriod.fromString(period.period)
                val displayInfo = getTimePeriodDisplayInfo(timePeriod)
                CategoryCard(
                    icon = displayInfo.icon,
                    title = displayInfo.displayName,
                    count = period.count,
                    onClick = { onPeriodClick(timePeriod) },
                )
            }
        }
    }
}

@Composable
private fun SourcesTabContent(
    domains: List<DomainWithCount>,
    onDomainClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (domains.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Default.Language,
            message = "No sources yet",
            description = "Sources will appear after processing screenshots",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = domains,
                key = { it.domain },
            ) { domain ->
                CategoryCard(
                    icon = Icons.Default.Language,
                    title = domain.domain,
                    count = domain.count,
                    onClick = { onDomainClick(domain.domain) },
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    count: Int,
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyTabContent(
    icon: ImageVector,
    message: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun SelectionDetailContent(
    screenshots: List<ScreenshotItem>,
    isLoading: Boolean,
    onItemClick: ((String) -> Unit)?,
    onSolvedToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTopicClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        screenshots.isEmpty() -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No screenshots in this category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
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
}

// Display info helpers

private data class DisplayInfo(
    val displayName: String,
    val icon: ImageVector,
)

private fun getActionDisplayInfo(action: String): DisplayInfo = when (action.lowercase()) {
    "buy" -> DisplayInfo("Things to Buy", Icons.Default.ShoppingCart)
    "read" -> DisplayInfo("Things to Read", Icons.AutoMirrored.Filled.MenuBook)
    "try" -> DisplayInfo("Things to Try", Icons.Default.Science)
    "decide" -> DisplayInfo("Decisions to Make", Icons.Default.Balance)
    "reference" -> DisplayInfo("References", Icons.Default.Bookmark)
    "idea" -> DisplayInfo("Ideas", Icons.Default.Lightbulb)
    else -> DisplayInfo(action.replaceFirstChar { it.uppercase() }, Icons.Default.Dashboard)
}

private fun getContentTypeDisplayInfo(type: String): DisplayInfo = when (type.lowercase()) {
    "article" -> DisplayInfo("Articles", Icons.AutoMirrored.Filled.Article)
    "product" -> DisplayInfo("Products", Icons.Default.ShoppingCart)
    "social" -> DisplayInfo("Social Media", Icons.Default.Share)
    "chat" -> DisplayInfo("Conversations", Icons.AutoMirrored.Filled.Chat)
    "code" -> DisplayInfo("Code Snippets", Icons.Default.Code)
    "recipe" -> DisplayInfo("Recipes", Icons.Default.Restaurant)
    "map" -> DisplayInfo("Maps & Places", Icons.Default.Map)
    else -> DisplayInfo(type.replaceFirstChar { it.uppercase() }, Icons.AutoMirrored.Filled.Article)
}

private fun getTimePeriodDisplayInfo(period: TimePeriod): DisplayInfo = when (period) {
    TimePeriod.TODAY -> DisplayInfo("Today", Icons.Default.AccessTime)
    TimePeriod.THIS_WEEK -> DisplayInfo("This Week", Icons.Default.AccessTime)
    TimePeriod.THIS_MONTH -> DisplayInfo("This Month", Icons.Default.AccessTime)
    TimePeriod.OLDER -> DisplayInfo("Older", Icons.Default.AccessTime)
}

private fun InsightsTab.displayName(): String = when (this) {
    InsightsTab.ACTIONS -> "Actions"
    InsightsTab.TYPES -> "Types"
    InsightsTab.TIME -> "Time"
    InsightsTab.SOURCES -> "Sources"
}
