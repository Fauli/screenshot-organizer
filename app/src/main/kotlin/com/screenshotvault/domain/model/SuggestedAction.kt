package com.screenshotvault.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SuggestedAction {
    @SerialName("read")
    READ,

    @SerialName("buy")
    BUY,

    @SerialName("try")
    TRY,

    @SerialName("reference")
    REFERENCE,

    @SerialName("decide")
    DECIDE,

    @SerialName("idea")
    IDEA,

    @SerialName("unknown")
    UNKNOWN,
}
