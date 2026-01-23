package com.screenshotvault.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.data.repository.SearchRepository
import com.screenshotvault.domain.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<ScreenshotItem> = emptyList(),
    val isSearching: Boolean = false,
    val showSolved: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _showSolved = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _results = MutableStateFlow<List<ScreenshotItem>>(emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _results,
        _isSearching,
        _showSolved,
    ) { query, results, isSearching, showSolved ->
        SearchUiState(
            query = query,
            results = results,
            isSearching = isSearching,
            showSolved = showSolved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(),
    )

    init {
        // Debounced search
        viewModelScope.launch {
            _query
                .debounce(300)
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun toggleShowSolved() {
        _showSolved.value = !_showSolved.value
        // Re-run search with new filter
        viewModelScope.launch {
            performSearch(_query.value)
        }
    }

    fun toggleItemSolved(id: String, solved: Boolean) {
        viewModelScope.launch {
            screenshotRepository.toggleSolved(id, solved)
            // Refresh results
            performSearch(_query.value)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            screenshotRepository.deleteItem(id)
            // Refresh results
            performSearch(_query.value)
        }
    }

    fun clearQuery() {
        _query.value = ""
        _results.value = emptyList()
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }

        _isSearching.value = true
        try {
            val results = searchRepository.search(
                query = query,
                includeSolved = _showSolved.value,
            )
            _results.value = results
        } catch (e: Exception) {
            _results.value = emptyList()
        }
        _isSearching.value = false
    }
}
