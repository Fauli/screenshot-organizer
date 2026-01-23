package com.screenshotvault.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityRef(
    val kind: EntityKind,
    val name: String,
)

@Serializable
enum class EntityKind {
    @SerialName("person")
    PERSON,

    @SerialName("org")
    ORG,

    @SerialName("product")
    PRODUCT,

    @SerialName("place")
    PLACE,

    @SerialName("other")
    OTHER,
}
