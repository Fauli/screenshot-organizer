package com.screenshotvault.ui.screens.domains

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
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
import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.domain.model.ScreenshotItem
import com.screenshotvault.ui.screens.feed.SwipeableScreenshotCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainsScreen(
    onItemClick: ((String) -> Unit)? = null,
    viewModel: DomainsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle system back button when viewing domain detail
    BackHandler(enabled = uiState.selectedDomain != null) {
        viewModel.clearSelectedDomain()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.selectedDomain ?: "Domains")
                },
                navigationIcon = {
                    if (uiState.selectedDomain != null) {
                        IconButton(onClick = viewModel::clearSelectedDomain) {
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
            uiState.selectedDomain != null -> {
                DomainDetailContent(
                    screenshots = uiState.domainScreenshots,
                    onItemClick = onItemClick,
                    onSolvedToggle = viewModel::toggleItemSolved,
                    onDelete = viewModel::deleteItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            uiState.domains.isEmpty() -> {
                EmptyDomainsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            else -> {
                DomainsListContent(
                    domains = uiState.domains,
                    onDomainClick = viewModel::selectDomain,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun DomainsListContent(
    domains: List<DomainWithCount>,
    onDomainClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = domains,
            key = { it.domain },
        ) { domain ->
            DomainCard(
                domain = domain,
                onClick = { onDomainClick(domain.domain) },
            )
        }
    }
}

@Composable
private fun DomainCard(
    domain: DomainWithCount,
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
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = domain.domain,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = domain.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DomainDetailContent(
    screenshots: List<ScreenshotItem>,
    onItemClick: ((String) -> Unit)?,
    onSolvedToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (screenshots.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No screenshots from this domain",
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
                )
            }
        }
    }
}

@Composable
private fun EmptyDomainsState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No domains yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Domains will appear after processing screenshots",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
