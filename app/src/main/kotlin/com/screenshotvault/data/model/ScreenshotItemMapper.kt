package com.screenshotvault.data.model

import com.screenshotvault.data.db.entities.EssenceEntity
import com.screenshotvault.data.db.entities.ScreenshotItemEntity
import com.screenshotvault.data.db.entities.ScreenshotWithEssence
import com.screenshotvault.domain.model.ScreenshotItem

fun ScreenshotItemEntity.toDomain(essence: EssenceEntity? = null): ScreenshotItem = ScreenshotItem(
    id = id,
    contentUri = contentUri,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    capturedAt = capturedAt,
    ingestedAt = ingestedAt,
    status = status,
    errorMessage = errorMessage,
    solved = solved,
    noContent = noContent,
    domain = domain,
    type = type,
    essence = essence?.toDomain(domain = domain, type = type),
)

fun ScreenshotWithEssence.toDomain(): ScreenshotItem = ScreenshotItem(
    id = screenshot.id,
    contentUri = screenshot.contentUri,
    displayName = screenshot.displayName,
    mimeType = screenshot.mimeType,
    sizeBytes = screenshot.sizeBytes,
    width = screenshot.width,
    height = screenshot.height,
    capturedAt = screenshot.capturedAt,
    ingestedAt = screenshot.ingestedAt,
    status = screenshot.status,
    errorMessage = screenshot.errorMessage,
    solved = screenshot.solved,
    noContent = screenshot.noContent,
    domain = screenshot.domain,
    type = screenshot.type,
    essence = essence?.toDomain(domain = screenshot.domain, type = screenshot.type),
)
