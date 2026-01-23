package com.screenshotvault.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    @SerialName("article")
    ARTICLE,

    @SerialName("product")
    PRODUCT,

    @SerialName("social")
    SOCIAL,

    @SerialName("chat")
    CHAT,

    @SerialName("code")
    CODE,

    @SerialName("recipe")
    RECIPE,

    @SerialName("map")
    MAP,

    @SerialName("unknown")
    UNKNOWN,
}
