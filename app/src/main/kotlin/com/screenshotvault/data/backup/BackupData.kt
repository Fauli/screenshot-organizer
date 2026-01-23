package com.screenshotvault.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val screenshots: List<ScreenshotBackup>,
    val essences: List<EssenceBackup>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class ScreenshotBackup(
    val id: String,
    val contentUri: String,
    val sha256: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val capturedAt: Long,
    val ingestedAt: Long,
    val status: String,
    val errorMessage: String?,
    val solved: Boolean,
    val noContent: Boolean = false,
    val domain: String?,
    val type: String?,
    val ocrText: String?,
)

@Serializable
data class EssenceBackup(
    val screenshotId: String,
    val title: String,
    val summaryBulletsJson: String,
    val topicsJson: String,
    val entitiesJson: String,
    val suggestedAction: String?,
    val confidence: Float,
    val modelName: String,
    val createdAt: Long,
)
