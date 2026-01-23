package com.screenshotvault.ingest.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.screenshotvault.R
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.data.model.toEntity
import com.screenshotvault.data.prefs.AiMode
import com.screenshotvault.data.prefs.PreferencesDataStore
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.data.repository.SearchRepository
import com.screenshotvault.domain.ai.EssenceExtractorFactory
import com.screenshotvault.domain.ai.ExtractionInput
import com.screenshotvault.domain.ai.ExtractionResult
import com.screenshotvault.ingest.ocr.OcrService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class ProcessScreenshotsWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val screenshotRepository: ScreenshotRepository,
    private val searchRepository: SearchRepository,
    private val ocrService: OcrService,
    private val extractorFactory: EssenceExtractorFactory,
    private val preferencesDataStore: PreferencesDataStore,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Start as foreground service to keep running when phone is locked
        setForeground(createForegroundInfo("Starting processing..."))

        var processedCount = 0
        var successCount = 0
        var failedCount = 0
        var noContentCount = 0

        // Get preferences for AI mode
        val prefs = preferencesDataStore.userPreferences.first()
        val aiMode = prefs.aiMode
        val apiKey = prefs.openAiApiKey

        // Create the appropriate extractor
        val essenceExtractor = extractorFactory.create(aiMode, apiKey)

        // Check for custom batch size from input data, otherwise use default
        val inputBatchSize = inputData.getInt(KEY_BATCH_SIZE, 0)
        val batchSize = when {
            inputBatchSize > 0 -> inputBatchSize
            aiMode == AiMode.OPENAI && !apiKey.isNullOrBlank() -> 1
            else -> BATCH_SIZE
        }

        // Get batch of items to process
        val items = screenshotRepository.getItemsForProcessing(batchSize)

        if (items.isEmpty()) {
            return@withContext Result.success(
                Data.Builder()
                    .putInt(KEY_PROCESSED_COUNT, 0)
                    .putInt(KEY_SUCCESS_COUNT, 0)
                    .putInt(KEY_FAILED_COUNT, 0)
                    .build(),
            )
        }

        val totalItems = items.size
        for (item in items) {
            processedCount++

            // Update notification with progress
            setForeground(createForegroundInfo("Processing $processedCount of $totalItems"))

            try {
                // Mark as processing
                screenshotRepository.updateStatus(item.id, ProcessingStatus.PROCESSING)

                // Load image bytes
                val uri = Uri.parse(item.contentUri)
                val imageBytes = loadImageBytes(uri)

                if (imageBytes == null) {
                    screenshotRepository.updateStatusWithError(
                        id = item.id,
                        status = ProcessingStatus.FAILED,
                        error = "Failed to load image file",
                    )
                    failedCount++
                    continue
                }

                // Run OCR first
                val ocrResult = ocrService.extractText(imageBytes)
                val ocrText = ocrResult?.fullText

                // Run essence extraction
                val input = ExtractionInput(
                    screenshotId = item.id,
                    imageBytes = imageBytes,
                    ocrText = ocrText,
                )

                when (val result = essenceExtractor.extract(input)) {
                    is ExtractionResult.Success -> {
                        val essence = result.essence

                        // Save essence
                        val essenceEntity = essence.toEntity(
                            screenshotId = item.id,
                            modelName = result.modelName,
                            createdAt = System.currentTimeMillis(),
                        )
                        screenshotRepository.saveEssence(essenceEntity)

                        // Update item with processed data
                        screenshotRepository.updateProcessingResult(
                            id = item.id,
                            status = ProcessingStatus.DONE,
                            domain = essence.domain,
                            type = essence.type.name.lowercase(),
                            ocrText = ocrText,
                        )

                        // Mark as no-content if detected
                        if (result.noContent) {
                            screenshotRepository.updateNoContent(item.id, true)
                            noContentCount++
                        } else {
                            // Only index items with content for search
                            searchRepository.indexScreenshot(
                                screenshotId = item.id,
                                title = essence.title,
                                summaryBullets = essence.summaryBullets,
                                topics = essence.topics,
                                entities = essence.entities.map { it.name },
                                ocrText = ocrText,
                                domain = essence.domain,
                                type = essence.type.name.lowercase(),
                            )
                        }

                        successCount++
                    }
                    is ExtractionResult.Failure -> {
                        screenshotRepository.updateStatusWithError(
                            id = item.id,
                            status = ProcessingStatus.FAILED,
                            error = result.reason,
                        )
                        failedCount++
                    }
                }
            } catch (e: Exception) {
                screenshotRepository.updateStatusWithError(
                    id = item.id,
                    status = ProcessingStatus.FAILED,
                    error = e.message ?: "Unknown error",
                )
                failedCount++
            }
        }

        // If there are more items to process, indicate we should run again
        val remainingItems = screenshotRepository.getItemsForProcessing(1)
        val hasMore = remainingItems.isNotEmpty()

        Result.success(
            Data.Builder()
                .putInt(KEY_PROCESSED_COUNT, processedCount)
                .putInt(KEY_SUCCESS_COUNT, successCount)
                .putInt(KEY_FAILED_COUNT, failedCount)
                .putInt(KEY_NO_CONTENT_COUNT, noContentCount)
                .putBoolean(KEY_HAS_MORE, hasMore)
                .build(),
        )
    }

    private fun loadImageBytes(uri: Uri): ByteArray? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Processing Screenshots")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Processing",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress when processing screenshots"
            }
            val notificationManager = appContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "screenshot_processing"
        private const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "process_screenshots"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_PROCESSED_COUNT = "processed_count"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_NO_CONTENT_COUNT = "no_content_count"
        const val KEY_HAS_MORE = "has_more"
        private const val BATCH_SIZE = 10
    }
}
