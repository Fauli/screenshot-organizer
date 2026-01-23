package com.screenshotvault.domain.model

data class Essence(
    val title: String,
    val type: ContentType,
    val domain: String?,
    val summaryBullets: List<String>,
    val topics: List<String>,
    val entities: List<EntityRef>,
    val suggestedAction: SuggestedAction,
    val confidence: Float,
)
