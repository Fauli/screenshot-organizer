package com.screenshotvault.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.db.dao.ActionCount
import com.screenshotvault.data.db.dao.ContentTypeCount
import com.screenshotvault.data.db.dao.DomainWithCount
import com.screenshotvault.data.db.dao.TimePeriodCount
import com.screenshotvault.data.repository.InsightsRepository
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.domain.model.ScreenshotItem
import com.screenshotvault.domain.model.TimePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InsightsTab {
    ACTIONS,
    TYPES,
    TIME,
    SOURCES,
}

sealed interface InsightsSelection {
    data class Action(val action: String, val displayName: String) : InsightsSelection
    data class ContentType(val type: String, val displayName: String) : InsightsSelection
    data class Time(val period: TimePeriod) : InsightsSelection
    data class Source(val domain: String) : InsightsSelection
}

data class InsightsUiState(
    val isLoading: Boolean = true,
    val selectedTab: InsightsTab = InsightsTab.ACTIONS,
    val includeSolved: Boolean = false,
    val selection: InsightsSelection? = null,
    val selectionScreenshots: List<ScreenshotItem> = emptyList(),
    val isLoadingSelection: Boolean = false,
    val actionCounts: List<ActionCount> = emptyList(),
    val contentTypeCounts: List<ContentTypeCount> = emptyList(),
    val timePeriodCounts: List<TimePeriodCount> = emptyList(),
    val domainCounts: List<DomainWithCount> = emptyList(),
    val totalCount: Int = 0,
    val thisWeekCount: Int = 0,
    val pendingCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(InsightsTab.ACTIONS)
    private val _includeSolved = MutableStateFlow(false)
    private val _selection = MutableStateFlow<InsightsSelection?>(null)
    private val _selectionScreenshots = MutableStateFlow<List<ScreenshotItem>>(emptyList())
    private val _isLoadingSelection = MutableStateFlow(false)

    val uiState: StateFlow<InsightsUiState> = combine(
        _selectedTab,
        _includeSolved,
        _selection,
        _selectionScreenshots,
        _isLoadingSelection,
        _includeSolved.flatMapLatest { insightsRepository.observeActionCounts(it) },
        _includeSolved.flatMapLatest { insightsRepository.observeContentTypeCounts(it) },
        _includeSolved.flatMapLatest { insightsRepository.observeTimePeriodCounts(it) },
        _includeSolved.flatMapLatest { insightsRepository.observeDomainsWithCount(it) },
        _includeSolved.flatMapLatest { insightsRepository.observeTotalCount(it) },
        _includeSolved.flatMapLatest { insightsRepository.observeThisWeekCount(it) },
        insightsRepository.observePendingCount(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        InsightsUiState(
            isLoading = false,
            selectedTab = values[0] as InsightsTab,
            includeSolved = values[1] as Boolean,
            selection = values[2] as InsightsSelection?,
            selectionScreenshots = values[3] as List<ScreenshotItem>,
            isLoadingSelection = values[4] as Boolean,
            actionCounts = values[5] as List<ActionCount>,
            contentTypeCounts = values[6] as List<ContentTypeCount>,
            timePeriodCounts = values[7] as List<TimePeriodCount>,
            domainCounts = values[8] as List<DomainWithCount>,
            totalCount = values[9] as Int,
            thisWeekCount = values[10] as Int,
            pendingCount = values[11] as Int,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState(),
    )

    fun selectTab(tab: InsightsTab) {
        _selectedTab.value = tab
    }

    fun toggleIncludeSolved() {
        _includeSolved.value = !_includeSolved.value
        // Reload selection if active
        _selection.value?.let { loadSelectionScreenshots(it) }
    }

    fun selectAction(action: String, displayName: String) {
        val selection = InsightsSelection.Action(action, displayName)
        _selection.value = selection
        loadSelectionScreenshots(selection)
    }

    fun selectContentType(type: String, displayName: String) {
        val selection = InsightsSelection.ContentType(type, displayName)
        _selection.value = selection
        loadSelectionScreenshots(selection)
    }

    fun selectTimePeriod(period: TimePeriod) {
        val selection = InsightsSelection.Time(period)
        _selection.value = selection
        loadSelectionScreenshots(selection)
    }

    fun selectDomain(domain: String) {
        val selection = InsightsSelection.Source(domain)
        _selection.value = selection
        loadSelectionScreenshots(selection)
    }

    fun clearSelection() {
        _selection.value = null
        _selectionScreenshots.value = emptyList()
    }

    private fun loadSelectionScreenshots(selection: InsightsSelection) {
        viewModelScope.launch {
            _isLoadingSelection.value = true
            val includeSolved = _includeSolved.value
            _selectionScreenshots.value = when (selection) {
                is InsightsSelection.Action -> {
                    insightsRepository.getScreenshotsByAction(selection.action, includeSolved)
                }
                is InsightsSelection.ContentType -> {
                    insightsRepository.getScreenshotsByContentType(selection.type, includeSolved)
                }
                is InsightsSelection.Time -> {
                    insightsRepository.getScreenshotsByTimePeriod(selection.period, includeSolved)
                }
                is InsightsSelection.Source -> {
                    insightsRepository.getScreenshotsByDomain(selection.domain, includeSolved)
                }
            }
            _isLoadingSelection.value = false
        }
    }

    fun toggleItemSolved(id: String, solved: Boolean) {
        viewModelScope.launch {
            screenshotRepository.toggleSolved(id, solved)
            // Reload selection screenshots
            _selection.value?.let { loadSelectionScreenshots(it) }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            screenshotRepository.deleteItem(id)
            // Reload selection screenshots
            _selection.value?.let { loadSelectionScreenshots(it) }
        }
    }
}
