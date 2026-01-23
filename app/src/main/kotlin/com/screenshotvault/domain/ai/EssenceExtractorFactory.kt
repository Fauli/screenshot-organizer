package com.screenshotvault.domain.ai

import com.screenshotvault.data.prefs.AiMode
import com.screenshotvault.ingest.ocr.MlKitOcrEssenceExtractor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EssenceExtractorFactory @Inject constructor(
    private val mlKitExtractor: MlKitOcrEssenceExtractor,
) {
    fun create(aiMode: AiMode, openAiApiKey: String?): EssenceExtractor {
        return when (aiMode) {
            AiMode.OPENAI -> {
                if (openAiApiKey.isNullOrBlank()) {
                    // Fallback to OCR if no API key
                    mlKitExtractor
                } else {
                    OpenAiEssenceExtractor(openAiApiKey)
                }
            }
            AiMode.OCR_ONLY -> mlKitExtractor
        }
    }
}
