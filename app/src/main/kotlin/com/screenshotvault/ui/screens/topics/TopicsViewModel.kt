package com.screenshotvault.ui.screens.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.data.repository.TopicWithCount
import com.screenshotvault.data.repository.TopicsRepository
import com.screenshotvault.domain.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicsUiState(
    val topics: List<TopicWithCount> = emptyList(),
    val selectedTopic: String? = null,
    val topicScreenshots: List<ScreenshotItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val topicsRepository: TopicsRepository,
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val _selectedTopic = MutableStateFlow<String?>(null)
    private val _topicScreenshots = MutableStateFlow<List<ScreenshotItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<TopicsUiState> = combine(
        topicsRepository.getTopicsWithCounts(),
        _selectedTopic,
        _topicScreenshots,
        _isLoading,
    ) { topics, selectedTopic, screenshots, isLoading ->
        TopicsUiState(
            topics = topics,
            selectedTopic = selectedTopic,
            topicScreenshots = screenshots,
            isLoading = isLoading && topics.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TopicsUiState(),
    )

    init {
        viewModelScope.launch {
            // Initial load will mark as not loading once topics arrive
            topicsRepository.getTopicsWithCounts().collect {
                _isLoading.value = false
            }
        }
    }

    fun selectTopic(topic: String) {
        _selectedTopic.value = topic
        loadTopicScreenshots(topic)
    }

    fun clearSelectedTopic() {
        _selectedTopic.value = null
        _topicScreenshots.value = emptyList()
    }

    fun toggleItemSolved(id: String, solved: Boolean) {
        viewModelScope.launch {
            screenshotRepository.toggleSolved(id, solved)
            // Refresh topic screenshots
            _selectedTopic.value?.let { loadTopicScreenshots(it) }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            screenshotRepository.deleteItem(id)
            // Refresh topic screenshots
            _selectedTopic.value?.let { loadTopicScreenshots(it) }
        }
    }

    private fun loadTopicScreenshots(topic: String) {
        viewModelScope.launch {
            val screenshots = topicsRepository.getScreenshotsByTopic(topic)
            _topicScreenshots.value = screenshots
        }
    }
}
