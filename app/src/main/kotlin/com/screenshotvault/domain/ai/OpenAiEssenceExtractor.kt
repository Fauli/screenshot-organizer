package com.screenshotvault.domain.ai

import android.util.Base64
import com.screenshotvault.domain.model.ContentType
import com.screenshotvault.domain.model.EntityKind
import com.screenshotvault.domain.model.EntityRef
import com.screenshotvault.domain.model.Essence
import com.screenshotvault.domain.model.SuggestedAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiEssenceExtractor(
    private val apiKey: String,
) : EssenceExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun extract(input: ExtractionInput): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            val base64Image = Base64.encodeToString(input.imageBytes, Base64.NO_WRAP)
            val response = callOpenAi(base64Image, input.ocrText)

            if (response != null) {
                ExtractionResult.Success(
                    essence = response.toEssence(),
                    modelName = "gpt-5.2",
                    noContent = response.noContent,
                )
            } else {
                ExtractionResult.Failure(
                    reason = "Failed to parse OpenAI response",
                    retryable = true,
                )
            }
        } catch (e: Exception) {
            ExtractionResult.Failure(
                reason = e.message ?: "Unknown error",
                retryable = e !is IllegalArgumentException,
            )
        }
    }

    private fun callOpenAi(base64Image: String, ocrText: String?): OpenAiEssenceResponse? {
        val systemPrompt = """
You are analyzing a screenshot to extract its essence for organizing and searching later. Respond with ONLY valid JSON (no markdown, no explanation).

STEP 1: Determine if this screenshot has meaningful content.

Set "no_content" to TRUE if:
- Plain wallpaper, background image, solid color, gradient
- Lock screen without notifications
- Empty home screen with just app icons
- Blank, loading, or error screens
- Status bar / navigation bar only
- Abstract images, patterns, decorative graphics
- Photos with no text or informational value

Set "no_content" to FALSE if there's actual content worth saving.

STEP 2: If no_content is FALSE, extract the MOST IMPORTANT information:

- **title**: The main subject - what would you search for to find this? Be specific!
  Good: "iPhone 15 Pro Max pricing comparison"
  Bad: "Product page" or "Screenshot"

- **summary_bullets**: The KEY facts someone saved this screenshot for. What's the actionable info?
  - Prices, dates, names, addresses, instructions, key quotes
  - What would someone need to remember from this?

- **topics**: Specific, searchable categories (3-5 max)
  Good: ["iPhone", "Apple", "pricing", "comparison"]
  Bad: ["technology", "phone", "screen"]

- **entities**: Important names that help organize
  - People, companies, products, places, brands mentioned

- **domain**: Website if visible (helps grouping)

- **suggested_action**: What will the user likely do with this info?

JSON schema:
{
  "no_content": false,
  "title": "Specific descriptive title (max 60 chars)",
  "type": "article|product|social|chat|code|recipe|map|unknown",
  "domain": "example.com or null",
  "summary_bullets": ["Most important fact 1", "Key detail 2", "Actionable info 3"],
  "topics": ["specific", "searchable", "topics"],
  "entities": [{"kind": "person|place|product|company|other", "name": "Name"}],
  "suggested_action": "read|buy|try|reference|decide|idea|unknown",
  "confidence": 0.0 to 1.0
}

If no_content is true, provide a brief descriptive title but leave arrays empty.
""".trimIndent()

        val userContent = buildString {
            append("Analyze this screenshot and extract the essence.")
            if (!ocrText.isNullOrBlank()) {
                append("\n\nOCR text detected:\n$ocrText")
            }
        }

        val requestBody = """
{
  "model": "gpt-5.2",
  "messages": [
    {
      "role": "system",
      "content": ${json.encodeToString(kotlinx.serialization.serializer(), systemPrompt)}
    },
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": ${json.encodeToString(kotlinx.serialization.serializer(), userContent)}
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,$base64Image",
            "detail": "low"
          }
        }
      ]
    }
  ],
  "max_completion_tokens": 1000
}
""".trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw RuntimeException("OpenAI API error ${response.code}: $errorBody")
        }

        val responseBody = response.body?.string() ?: return null
        val chatResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
        val content = chatResponse.choices.firstOrNull()?.message?.content ?: return null

        // Clean up the content - remove markdown code blocks if present
        val cleanedContent = content
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        return json.decodeFromString<OpenAiEssenceResponse>(cleanedContent)
    }
}

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice>,
)

@Serializable
private data class Choice(
    val message: Message,
)

@Serializable
private data class Message(
    val content: String,
)

@Serializable
private data class OpenAiEssenceResponse(
    val no_content: Boolean = false,
    val title: String = "No content",
    val type: String = "unknown",
    val domain: String? = null,
    val summary_bullets: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val entities: List<EntityResponse> = emptyList(),
    val suggested_action: String = "unknown",
    val confidence: Float = 0.8f,
) {
    val noContent: Boolean get() = no_content
    fun toEssence() = Essence(
        title = title,
        type = parseContentType(type),
        domain = domain,
        summaryBullets = summary_bullets,
        topics = topics,
        entities = entities.map { EntityRef(kind = parseEntityKind(it.kind), name = it.name) },
        suggestedAction = parseSuggestedAction(suggested_action),
        confidence = confidence,
    )

    private fun parseContentType(type: String): ContentType {
        return try {
            ContentType.valueOf(type.uppercase())
        } catch (e: Exception) {
            ContentType.UNKNOWN
        }
    }

    private fun parseSuggestedAction(action: String): SuggestedAction {
        return try {
            SuggestedAction.valueOf(action.uppercase())
        } catch (e: Exception) {
            SuggestedAction.UNKNOWN
        }
    }

    private fun parseEntityKind(kind: String): EntityKind {
        return when (kind.lowercase()) {
            "person" -> EntityKind.PERSON
            "org", "company", "organization" -> EntityKind.ORG
            "product" -> EntityKind.PRODUCT
            "place", "location" -> EntityKind.PLACE
            else -> EntityKind.OTHER
        }
    }
}

@Serializable
private data class EntityResponse(
    val kind: String,
    val name: String,
)
