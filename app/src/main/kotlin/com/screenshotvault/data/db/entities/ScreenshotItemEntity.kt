package com.screenshotvault.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshot_items",
    indices = [
        Index(value = ["content_uri"], unique = true),
        Index(value = ["sha256"], unique = true),
        Index(value = ["status"]),
        Index(value = ["solved"]),
        Index(value = ["no_content"]),
        Index(value = ["captured_at"]),
    ],
)
data class ScreenshotItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "content_uri")
    val contentUri: String,

    val sha256: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    val width: Int,

    val height: Int,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,

    @ColumnInfo(name = "ingested_at")
    val ingestedAt: Long,

    val status: ProcessingStatus = ProcessingStatus.NEW,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    val solved: Boolean = false,

    @ColumnInfo(name = "no_content")
    val noContent: Boolean = false,

    val domain: String? = null,

    val type: String? = null,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String? = null,
)
