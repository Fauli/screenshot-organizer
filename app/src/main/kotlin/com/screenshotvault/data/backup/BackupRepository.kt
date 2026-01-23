package com.screenshotvault.data.backup

import android.content.Context
import android.net.Uri
import com.screenshotvault.data.db.dao.EssenceDao
import com.screenshotvault.data.db.dao.ScreenshotItemDao
import com.screenshotvault.data.db.entities.EssenceEntity
import com.screenshotvault.data.db.entities.ProcessingStatus
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.repository.SearchRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupResult {
    data class Success(val count: Int) : BackupResult
    data class Error(val message: String) : BackupResult
}

sealed interface ImportResult {
    data class Success(val imported: Int, val skipped: Int) : ImportResult
    data class Error(val message: String) : ImportResult
}

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotDao: ScreenshotItemDao,
    private val essenceDao: EssenceDao,
    private val searchRepository: SearchRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Get all screenshots and essences
            val screenshots = screenshotDao.getAll()
            val essences = essenceDao.getAll()

            val backupData = BackupData(
                screenshots = screenshots.map { it.toBackup() },
                essences = essences.map { it.toBackup() },
            )

            val jsonString = json.encodeToString(backupData)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext BackupResult.Error("Could not open file for writing")

            BackupResult.Success(screenshots.size)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error during backup")
        }
    }

    suspend fun importBackup(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext ImportResult.Error("Could not open file for reading")

            val backupData = json.decodeFromString<BackupData>(jsonString)

            var imported = 0
            var skipped = 0

            // Import screenshots
            for (screenshotBackup in backupData.screenshots) {
                // Check if already exists by sha256
                if (screenshotDao.existsBySha256(screenshotBackup.sha256)) {
                    skipped++
                    continue
                }

                val entity = screenshotBackup.toEntity()
                val result = screenshotDao.insert(entity)
                if (result != -1L) {
                    imported++
                } else {
                    skipped++
                }
            }

            // Import essences
            for (essenceBackup in backupData.essences) {
                // Check if screenshot exists
                val screenshot = screenshotDao.getById(essenceBackup.screenshotId)
                if (screenshot == null) {
                    continue
                }

                // Check if essence already exists
                val existingEssence = essenceDao.getByScreenshotId(essenceBackup.screenshotId)
                if (existingEssence != null) {
                    continue
                }

                val entity = essenceBackup.toEntity()
                essenceDao.insert(entity)

                // Re-index for search
                reindexScreenshot(screenshot, entity)
            }

            ImportResult.Success(imported, skipped)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error during import")
        }
    }

    private suspend fun reindexScreenshot(screenshot: ScreenshotItemEntity, essence: EssenceEntity) {
        try {
            val summaryBullets = json.decodeFromString<List<String>>(essence.summaryBulletsJson)
            val topics = json.decodeFromString<List<String>>(essence.topicsJson)

            @kotlinx.serialization.Serializable
            data class EntityRef(val kind: String, val name: String)
            val entities = try {
                json.decodeFromString<List<EntityRef>>(essence.entitiesJson).map { it.name }
            } catch (e: Exception) {
                emptyList()
            }

            searchRepository.indexScreenshot(
                screenshotId = screenshot.id,
                title = essence.title,
                summaryBullets = summaryBullets,
                topics = topics,
                entities = entities,
                ocrText = screenshot.ocrText,
                domain = screenshot.domain,
                type = screenshot.type,
            )
        } catch (e: Exception) {
            // Ignore indexing errors
        }
    }

    private fun ScreenshotItemEntity.toBackup() = ScreenshotBackup(
        id = id,
        contentUri = contentUri,
        sha256 = sha256,
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        capturedAt = capturedAt,
        ingestedAt = ingestedAt,
        status = status.name,
        errorMessage = errorMessage,
        solved = solved,
        noContent = noContent,
        domain = domain,
        type = type,
        ocrText = ocrText,
    )

    private fun EssenceEntity.toBackup() = EssenceBackup(
        screenshotId = screenshotId,
        title = title,
        summaryBulletsJson = summaryBulletsJson,
        topicsJson = topicsJson,
        entitiesJson = entitiesJson,
        suggestedAction = suggestedAction,
        confidence = confidence,
        modelName = modelName,
        createdAt = createdAt,
    )

    private fun ScreenshotBackup.toEntity() = ScreenshotItemEntity(
        id = id,
        contentUri = contentUri,
        sha256 = sha256,
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        capturedAt = capturedAt,
        ingestedAt = ingestedAt,
        status = try { ProcessingStatus.valueOf(status) } catch (e: Exception) { ProcessingStatus.NEW },
        errorMessage = errorMessage,
        solved = solved,
        noContent = noContent,
        domain = domain,
        type = type,
        ocrText = ocrText,
    )

    private fun EssenceBackup.toEntity() = EssenceEntity(
        screenshotId = screenshotId,
        title = title,
        summaryBulletsJson = summaryBulletsJson,
        topicsJson = topicsJson,
        entitiesJson = entitiesJson,
        suggestedAction = suggestedAction,
        confidence = confidence,
        modelName = modelName,
        createdAt = createdAt,
    )
}
