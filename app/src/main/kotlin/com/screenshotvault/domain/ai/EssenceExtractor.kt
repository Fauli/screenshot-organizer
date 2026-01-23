package com.screenshotvault.domain.ai

import com.screenshotvault.domain.model.Essence

interface EssenceExtractor {
    suspend fun extract(input: ExtractionInput): ExtractionResult
}

data class ExtractionInput(
    val screenshotId: String,
    val imageBytes: ByteArray,
    val ocrText: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExtractionInput
        if (screenshotId != other.screenshotId) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false
        if (ocrText != other.ocrText) return false
        return true
    }

    override fun hashCode(): Int {
        var result = screenshotId.hashCode()
        result = 31 * result + imageBytes.contentHashCode()
        result = 31 * result + (ocrText?.hashCode() ?: 0)
        return result
    }
}

sealed interface ExtractionResult {
    data class Success(
        val essence: Essence,
        val modelName: String,
        val noContent: Boolean = false,
    ) : ExtractionResult

    data class Failure(
        val reason: String,
        val retryable: Boolean,
    ) : ExtractionResult
}
