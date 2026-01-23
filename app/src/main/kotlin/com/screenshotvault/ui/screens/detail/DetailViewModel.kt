package com.screenshotvault.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.domain.model.ScreenshotItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val screenshot: ScreenshotItem? = null,
    val showOriginal: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val screenshotId: String = checkNotNull(savedStateHandle["screenshotId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadScreenshot()
    }

    private fun loadScreenshot() {
        viewModelScope.launch {
            screenshotRepository.observeById(screenshotId).collect { item ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        screenshot = item,
                    )
                }
            }
        }
    }

    fun toggleSolved() {
        val current = _uiState.value.screenshot ?: return
        viewModelScope.launch {
            screenshotRepository.toggleSolved(current.id, !current.solved)
        }
    }

    fun openOriginal() {
        _uiState.update { it.copy(showOriginal = true) }
    }

    fun closeOriginal() {
        _uiState.update { it.copy(showOriginal = false) }
    }

    fun reprocess() {
        viewModelScope.launch {
            screenshotRepository.reprocessItem(screenshotId)
        }
    }
}
