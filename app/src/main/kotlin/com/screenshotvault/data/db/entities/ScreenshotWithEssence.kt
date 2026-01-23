package com.screenshotvault.data.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ScreenshotWithEssence(
    @Embedded
    val screenshot: ScreenshotItemEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "screenshot_id",
    )
    val essence: EssenceEntity?,
)
