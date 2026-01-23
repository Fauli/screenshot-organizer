package com.screenshotvault.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.screenshotvault.data.prefs.UserPreferences
import com.screenshotvault.data.repository.PreferencesRepository
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.domain.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProcessingFilter {
    ALL,        // Show all items
    PROCESSED,  // Only show processed (DONE)
    PENDING,    // Only show unprocessed (NEW, PROCESSING, FAILED)
}

data class FeedUiState(
    val showSolved: Boolean = false,
    val showNoContent: Boolean = false,
    val processingFilter: ProcessingFilter = ProcessingFilter.ALL,
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val pendingCount: Int = 0,
    val noContentCount: Int = 0,
    val feedCount: Int = 0, // Current visible count based on filters
    val hasFolderSelected: Boolean = false,
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val screenshotRepository: ScreenshotRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    // These will be initialized from preferences, defaulting to hide
    private val _showSolved = MutableStateFlow(false)
    private val _showNoContent = MutableStateFlow(false)
    private val _processingFilter = MutableStateFlow(ProcessingFilter.ALL)

    init {
        // Initialize filter states from preferences (one-time read)
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            _showSolved.value = !prefs.hideSolvedByDefault
            _showNoContent.value = !prefs.hideNoContentByDefault
        }
    }

    val uiState: StateFlow<FeedUiState> = combine(
        _showSolved,
        _showNoContent,
        _processingFilter,
        screenshotRepository.observeTotalCount(),
        screenshotRepository.observeProcessedCount(),
        screenshotRepository.observePendingCount(),
        screenshotRepository.observeNoContentCount(),
        preferencesRepository.userPreferences,
    ) { values ->
        val showSolved = values[0] as Boolean
        val showNoContent = values[1] as Boolean
        val processingFilter = values[2] as ProcessingFilter
        val totalCount = values[3] as Int
        val processedCount = values[4] as Int
        val pendingCount = values[5] as Int
        val noContentCount = values[6] as Int
        val preferences = values[7] as com.screenshotvault.data.prefs.UserPreferences
        FeedUiState(
            showSolved = showSolved,
            showNoContent = showNoContent,
            processingFilter = processingFilter,
            totalCount = totalCount,
            processedCount = processedCount,
            pendingCount = pendingCount,
            noContentCount = noContentCount,
            hasFolderSelected = preferences.selectedFolderUri != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val screenshots: Flow<PagingData<ScreenshotItem>> = combine(
        _showSolved,
        _showNoContent,
        _processingFilter,
    ) { showSolved, showNoContent, processingFilter ->
        Triple(showSolved, showNoContent, processingFilter)
    }.flatMapLatest { (showSolved, showNoContent, processingFilter) ->
        screenshotRepository.getFeedPagingData(
            includeSolved = showSolved,
            includeNoContent = showNoContent,
            processingFilter = processingFilter,
        )
    }.cachedIn(viewModelScope)

    fun toggleShowSolved() {
        _showSolved.value = !_showSolved.value
    }

    fun toggleShowNoContent() {
        _showNoContent.value = !_showNoContent.value
    }

    fun setProcessingFilter(filter: ProcessingFilter) {
        _processingFilter.value = filter
    }

    fun toggleItemSolved(id: String, solved: Boolean) {
        viewModelScope.launch {
            screenshotRepository.toggleSolved(id, solved)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            screenshotRepository.deleteItem(id)
        }
    }
}
