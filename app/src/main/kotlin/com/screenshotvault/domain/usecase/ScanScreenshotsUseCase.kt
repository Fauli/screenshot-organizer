package com.screenshotvault.domain.usecase

import android.net.Uri
import com.screenshotvault.data.repository.PreferencesRepository
import com.screenshotvault.ingest.scanner.ScanResult
import com.screenshotvault.ingest.scanner.ScreenshotScanner
import javax.inject.Inject

sealed interface ScanOutcome {
    data class Success(val result: ScanResult) : ScanOutcome
    data object NoFolderSelected : ScanOutcome
    data object PermissionLost : ScanOutcome
    data class Error(val message: String) : ScanOutcome
}

class ScanScreenshotsUseCase @Inject constructor(
    private val screenshotScanner: ScreenshotScanner,
    private val selectFolderUseCase: SelectFolderUseCase,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(): ScanOutcome {
        val folderUri = selectFolderUseCase.getSelectedFolderUri()
            ?: return ScanOutcome.NoFolderSelected

        if (!selectFolderUseCase.hasValidPermission(folderUri)) {
            return ScanOutcome.PermissionLost
        }

        return try {
            val result = screenshotScanner.scan(folderUri)
            preferencesRepository.setLastScanAt(System.currentTimeMillis())
            ScanOutcome.Success(result)
        } catch (e: Exception) {
            ScanOutcome.Error(e.message ?: "Unknown error during scan")
        }
    }
}
