package com.screenshotvault.domain.model

import com.screenshotvault.data.db.entities.ProcessingStatus

data class ScreenshotItem(
    val id: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val capturedAt: Long,
    val ingestedAt: Long,
    val status: ProcessingStatus,
    val errorMessage: String?,
    val solved: Boolean,
    val noContent: Boolean,
    val domain: String?,
    val type: String?,
    val essence: Essence?,
)
