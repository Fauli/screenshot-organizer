package com.screenshotvault.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "essences",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["screenshot_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["screenshot_id"], unique = true),
    ],
)
data class EssenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "screenshot_id")
    val screenshotId: String,

    val title: String,

    @ColumnInfo(name = "summary_bullets_json")
    val summaryBulletsJson: String,

    @ColumnInfo(name = "topics_json")
    val topicsJson: String,

    @ColumnInfo(name = "entities_json")
    val entitiesJson: String,

    @ColumnInfo(name = "suggested_action")
    val suggestedAction: String? = null,

    val confidence: Float,

    @ColumnInfo(name = "model_name")
    val modelName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
