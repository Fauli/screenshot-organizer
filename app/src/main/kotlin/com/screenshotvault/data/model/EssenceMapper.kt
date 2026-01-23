package com.screenshotvault.data.model

import com.screenshotvault.data.db.entities.EssenceEntity
import com.screenshotvault.domain.model.ContentType
import com.screenshotvault.domain.model.EntityRef
import com.screenshotvault.domain.model.Essence
import com.screenshotvault.domain.model.SuggestedAction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun EssenceEntity.toDomain(domain: String? = null, type: String? = null): Essence = Essence(
    title = title,
    type = parseContentType(type),
    domain = domain,
    summaryBullets = parseStringList(summaryBulletsJson),
    topics = parseStringList(topicsJson),
    entities = parseEntities(entitiesJson),
    suggestedAction = parseSuggestedAction(suggestedAction),
    confidence = confidence,
)

fun Essence.toEntity(
    screenshotId: String,
    modelName: String,
    createdAt: Long,
): EssenceEntity = EssenceEntity(
    screenshotId = screenshotId,
    title = title,
    summaryBulletsJson = json.encodeToString(summaryBullets),
    topicsJson = json.encodeToString(topics),
    entitiesJson = json.encodeToString(entities),
    suggestedAction = suggestedAction.name.lowercase(),
    confidence = confidence,
    modelName = modelName,
    createdAt = createdAt,
)

private fun parseStringList(jsonString: String): List<String> = try {
    json.decodeFromString<List<String>>(jsonString)
} catch (e: Exception) {
    emptyList()
}

private fun parseEntities(jsonString: String): List<EntityRef> = try {
    json.decodeFromString<List<EntityRef>>(jsonString)
} catch (e: Exception) {
    emptyList()
}

private fun parseContentType(value: String?): ContentType = try {
    value?.let { ContentType.valueOf(it.uppercase()) } ?: ContentType.UNKNOWN
} catch (e: Exception) {
    ContentType.UNKNOWN
}

private fun parseSuggestedAction(value: String?): SuggestedAction = try {
    value?.let { SuggestedAction.valueOf(it.uppercase()) } ?: SuggestedAction.UNKNOWN
} catch (e: Exception) {
    SuggestedAction.UNKNOWN
}
