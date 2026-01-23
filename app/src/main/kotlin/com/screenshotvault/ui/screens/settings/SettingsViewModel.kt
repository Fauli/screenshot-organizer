package com.screenshotvault.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotvault.data.backup.BackupRepository
import com.screenshotvault.data.backup.BackupResult
import com.screenshotvault.data.backup.ImportResult
import com.screenshotvault.data.prefs.AiMode
import com.screenshotvault.data.prefs.PreferencesDataStore
import com.screenshotvault.data.prefs.UserPreferences
import com.screenshotvault.data.repository.PreferencesRepository
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.domain.usecase.ProcessScreenshotsUseCase
import com.screenshotvault.domain.usecase.ScanOutcome
import com.screenshotvault.domain.usecase.ScanScreenshotsUseCase
import com.screenshotvault.domain.usecase.SelectFolderUseCase
import com.screenshotvault.ingest.scanner.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isScanning: Boolean = false,
    val isBackingUp: Boolean = false,
    val isImporting: Boolean = false,
    val lastScanResult: ScanResult? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val totalScreenshots: Int = 0,
    val pendingProcessing: Int = 0,
    val currentlyProcessing: Int = 0,
    val processedCount: Int = 0,
    val noContentCount: Int = 0,
    val batchTotal: Int = 0,
    val batchRemaining: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val screenshotRepository: ScreenshotRepository,
    private val backupRepository: BackupRepository,
    private val selectFolderUseCase: SelectFolderUseCase,
    private val scanScreenshotsUseCase: ScanScreenshotsUseCase,
    private val processScreenshotsUseCase: ProcessScreenshotsUseCase,
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    private val _isBackingUp = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _batchTotal = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.userPreferences,
        _isScanning,
        _isBackingUp,
        _isImporting,
        _lastScanResult,
        _errorMessage,
        _successMessage,
        screenshotRepository.observeTotalCount(),
        screenshotRepository.observeNewCount(),
        screenshotRepository.observeProcessingCount(),
        screenshotRepository.observeProcessedCount(),
        screenshotRepository.observeNoContentCount(),
        _batchTotal,
    ) { values ->
        val preferences = values[0] as UserPreferences
        val isScanning = values[1] as Boolean
        val isBackingUp = values[2] as Boolean
        val isImporting = values[3] as Boolean
        val lastScanResult = values[4] as ScanResult?
        val errorMessage = values[5] as String?
        val successMessage = values[6] as String?
        val totalScreenshots = values[7] as Int
        val pendingProcessing = values[8] as Int
        val currentlyProcessing = values[9] as Int
        val processedCount = values[10] as Int
        val noContentCount = values[11] as Int
        val batchTotal = values[12] as Int

        // Calculate batch progress based on how many have been processed since batch started
        val batchCompleted = if (batchTotal > 0) {
            (processedCount - batchStartProcessedCount).coerceIn(0, batchTotal)
        } else 0
        val batchRemaining = batchTotal - batchCompleted

        // Clear batch when done (all completed or no longer processing)
        val isActive = currentlyProcessing > 0 || (batchTotal > 0 && batchRemaining > 0)

        SettingsUiState(
            preferences = preferences,
            isScanning = isScanning,
            isBackingUp = isBackingUp,
            isImporting = isImporting,
            lastScanResult = lastScanResult,
            errorMessage = errorMessage,
            successMessage = successMessage,
            totalScreenshots = totalScreenshots,
            pendingProcessing = pendingProcessing,
            currentlyProcessing = currentlyProcessing,
            processedCount = processedCount,
            noContentCount = noContentCount,
            batchTotal = if (isActive) batchTotal else 0,
            batchRemaining = if (isActive) batchRemaining else 0,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
    )

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            selectFolderUseCase.saveSelectedFolder(uri)
            _errorMessage.value = null
        }
    }

    fun clearFolder() {
        viewModelScope.launch {
            selectFolderUseCase.clearSelectedFolder()
        }
    }

    fun scanNow() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _errorMessage.value = null
            _lastScanResult.value = null

            when (val outcome = scanScreenshotsUseCase()) {
                is ScanOutcome.Success -> {
                    _lastScanResult.value = outcome.result
                    // Don't auto-process - let user trigger manually
                }
                is ScanOutcome.NoFolderSelected -> {
                    _errorMessage.value = "No folder selected. Please select a screenshot folder."
                }
                is ScanOutcome.PermissionLost -> {
                    _errorMessage.value = "Folder permission lost. Please re-select the folder."
                }
                is ScanOutcome.Error -> {
                    _errorMessage.value = outcome.message
                }
            }

            _isScanning.value = false
        }
    }

    private var batchStartProcessedCount = 0

    fun processNow() {
        val pending = uiState.value.pendingProcessing
        _batchTotal.value = pending
        batchStartProcessedCount = uiState.value.processedCount
        processScreenshotsUseCase()
    }

    fun processBatch(count: Int) {
        val actualCount = minOf(count, uiState.value.pendingProcessing)
        _batchTotal.value = actualCount
        batchStartProcessedCount = uiState.value.processedCount
        processScreenshotsUseCase(batchSize = count)
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun setAiMode(mode: AiMode) {
        viewModelScope.launch {
            preferencesDataStore.setAiMode(mode)
        }
    }

    fun setOpenAiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesDataStore.setOpenAiApiKey(apiKey.ifBlank { null })
        }
    }

    fun exportBackup(uri: Uri) {
        if (_isBackingUp.value) return

        viewModelScope.launch {
            _isBackingUp.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = backupRepository.exportBackup(uri)) {
                is BackupResult.Success -> {
                    _successMessage.value = "Exported ${result.count} screenshots"
                }
                is BackupResult.Error -> {
                    _errorMessage.value = "Backup failed: ${result.message}"
                }
            }

            _isBackingUp.value = false
        }
    }

    fun importBackup(uri: Uri) {
        if (_isImporting.value) return

        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = backupRepository.importBackup(uri)) {
                is ImportResult.Success -> {
                    _successMessage.value = "Imported ${result.imported} screenshots (${result.skipped} skipped)"
                }
                is ImportResult.Error -> {
                    _errorMessage.value = "Import failed: ${result.message}"
                }
            }

            _isImporting.value = false
        }
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    fun setAutoProcessOnStartup(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoProcessOnStartup(enabled)
        }
    }

    fun resetForReprocessing() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            val count = screenshotRepository.resetAllForReprocessing()
            _successMessage.value = "Reset $count screenshots for reprocessing"
        }
    }

    fun resetStuckProcessing() {
        viewModelScope.launch {
            _errorMessage.value = null
            _successMessage.value = null
            val count = screenshotRepository.resetStuckProcessing()
            _successMessage.value = "Reset $count stuck items"
        }
    }
}
