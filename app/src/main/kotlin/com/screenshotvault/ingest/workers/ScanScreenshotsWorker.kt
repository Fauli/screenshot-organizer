package com.screenshotvault.ingest.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.screenshotvault.data.repository.PreferencesRepository
import com.screenshotvault.domain.usecase.ScanOutcome
import com.screenshotvault.domain.usecase.ScanScreenshotsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScanScreenshotsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scanScreenshotsUseCase: ScanScreenshotsUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (val outcome = scanScreenshotsUseCase()) {
            is ScanOutcome.Success -> {
                val data = Data.Builder()
                    .putInt(KEY_SCANNED_COUNT, outcome.result.scannedCount)
                    .putInt(KEY_NEW_COUNT, outcome.result.newCount)
                    .putInt(KEY_SKIPPED_COUNT, outcome.result.skippedCount)
                    .putInt(KEY_ERROR_COUNT, outcome.result.errorCount)
                    .build()
                Result.success(data)
            }
            is ScanOutcome.NoFolderSelected -> {
                // No folder selected - don't retry
                Result.failure(
                    Data.Builder()
                        .putString(KEY_ERROR, "No folder selected")
                        .build(),
                )
            }
            is ScanOutcome.PermissionLost -> {
                // Permission lost - don't retry
                Result.failure(
                    Data.Builder()
                        .putString(KEY_ERROR, "Folder permission lost")
                        .build(),
                )
            }
            is ScanOutcome.Error -> {
                // Retry on errors
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure(
                        Data.Builder()
                            .putString(KEY_ERROR, outcome.message)
                            .build(),
                    )
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "scan_screenshots"
        const val KEY_SCANNED_COUNT = "scanned_count"
        const val KEY_NEW_COUNT = "new_count"
        const val KEY_SKIPPED_COUNT = "skipped_count"
        const val KEY_ERROR_COUNT = "error_count"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3
    }
}
