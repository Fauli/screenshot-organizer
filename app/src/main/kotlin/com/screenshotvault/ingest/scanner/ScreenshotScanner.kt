package com.screenshotvault.ingest.scanner

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.repository.ScreenshotRepository
import com.screenshotvault.ingest.hashing.FileHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ScanResult(
    val scannedCount: Int,
    val newCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
)

@Singleton
class ScreenshotScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileHasher: FileHasher,
    private val screenshotRepository: ScreenshotRepository,
) {
    suspend fun scan(folderUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        var scannedCount = 0
        var newCount = 0
        var skippedCount = 0
        var errorCount = 0

        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
        if (documentFile == null || !documentFile.exists()) {
            return@withContext ScanResult(0, 0, 0, 1)
        }

        val imageFiles = documentFile.listFiles().filter { file ->
            file.isFile && file.type?.startsWith("image/") == true
        }

        for (file in imageFiles) {
            scannedCount++
            val uri = file.uri

            try {
                val sha256 = fileHasher.computeSha256(uri)
                if (sha256 == null) {
                    errorCount++
                    continue
                }

                if (screenshotRepository.existsBySha256(sha256)) {
                    skippedCount++
                    continue
                }

                val dimensions = getImageDimensions(uri)
                val capturedAt = extractCapturedAt(file)

                val entity = ScreenshotItemEntity(
                    id = UUID.randomUUID().toString(),
                    contentUri = uri.toString(),
                    sha256 = sha256,
                    displayName = file.name ?: "unknown",
                    mimeType = file.type ?: "image/*",
                    sizeBytes = file.length(),
                    width = dimensions.first,
                    height = dimensions.second,
                    capturedAt = capturedAt,
                    ingestedAt = System.currentTimeMillis(),
                    status = ProcessingStatus.NEW,
                )

                val result = screenshotRepository.insert(entity)
                if (result != -1L) {
                    newCount++
                } else {
                    skippedCount++ // Already exists (race condition)
                }
            } catch (e: Exception) {
                errorCount++
            }
        }

        ScanResult(scannedCount, newCount, skippedCount, errorCount)
    }

    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun extractCapturedAt(file: DocumentFile): Long {
        // Try to get last modified time from DocumentFile
        val lastModified = file.lastModified()
        if (lastModified > 0) {
            return lastModified
        }

        // Fallback: try to parse date from filename (common patterns like Screenshot_20240115_123456)
        val name = file.name ?: return System.currentTimeMillis()
        val datePattern = Regex("""(\d{4})(\d{2})(\d{2})[_-]?(\d{2})(\d{2})(\d{2})?""")
        val match = datePattern.find(name)

        return if (match != null) {
            try {
                val (year, month, day, hour, minute, second) = match.destructured
                val calendar = java.util.Calendar.getInstance().apply {
                    set(year.toInt(), month.toInt() - 1, day.toInt(),
                        hour.toInt(), minute.toInt(), second.toIntOrNull() ?: 0)
                }
                calendar.timeInMillis
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
    }
}
