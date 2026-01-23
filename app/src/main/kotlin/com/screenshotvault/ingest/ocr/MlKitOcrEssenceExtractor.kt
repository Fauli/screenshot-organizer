package com.screenshotvault.ingest.ocr

import com.screenshotvault.domain.ai.EssenceExtractor
import com.screenshotvault.domain.ai.ExtractionInput
import com.screenshotvault.domain.ai.ExtractionResult
import com.screenshotvault.domain.model.ContentType
import com.screenshotvault.domain.model.EntityRef
import com.screenshotvault.domain.model.Essence
import com.screenshotvault.domain.model.SuggestedAction
import javax.inject.Inject

class MlKitOcrEssenceExtractor @Inject constructor(
    private val ocrService: OcrService,
) : EssenceExtractor {

    override suspend fun extract(input: ExtractionInput): ExtractionResult {
        // Use provided OCR text or extract from image
        val ocrResult = if (input.ocrText != null) {
            OcrResult(fullText = input.ocrText, blocks = emptyList())
        } else {
            ocrService.extractText(input.imageBytes)
        }

        if (ocrResult == null || ocrResult.fullText.isBlank()) {
            return ExtractionResult.Failure(
                reason = "Failed to extract text from image",
                retryable = true,
            )
        }

        val text = ocrResult.fullText
        val lines = text.lines().filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExtractionResult.Failure(
                reason = "No text content found in image",
                retryable = false,
            )
        }

        val domain = extractDomain(text)
        val contentType = inferContentType(text, domain)
        val title = extractTitle(lines, ocrResult.blocks)
        val summaryBullets = extractSummaryBullets(lines, title)
        val topics = extractTopics(text)
        val entities = extractEntities(text)
        val suggestedAction = inferSuggestedAction(contentType, text)

        val essence = Essence(
            title = title,
            type = contentType,
            domain = domain,
            summaryBullets = summaryBullets,
            topics = topics,
            entities = entities,
            suggestedAction = suggestedAction,
            confidence = 0.4f, // Low confidence for OCR-only extraction
        )

        return ExtractionResult.Success(
            essence = essence,
            modelName = "mlkit-ocr-heuristic-v1",
        )
    }

    private fun extractDomain(text: String): String? {
        // Look for URLs in text
        val urlPattern = Regex(
            """(?:https?://)?(?:www\.)?([a-zA-Z0-9][-a-zA-Z0-9]*\.[a-zA-Z]{2,})(?:/[^\s]*)?""",
            RegexOption.IGNORE_CASE,
        )
        val match = urlPattern.find(text)
        return match?.groupValues?.getOrNull(1)?.lowercase()
    }

    private fun inferContentType(text: String, domain: String?): ContentType {
        val lowerText = text.lowercase()

        // Check for code patterns
        if (containsCodePatterns(lowerText)) {
            return ContentType.CODE
        }

        // Check for chat patterns
        if (containsChatPatterns(lowerText)) {
            return ContentType.CHAT
        }

        // Check for recipe patterns
        if (containsRecipePatterns(lowerText)) {
            return ContentType.RECIPE
        }

        // Check for product patterns
        if (containsProductPatterns(lowerText)) {
            return ContentType.PRODUCT
        }

        // Check for social media patterns
        if (containsSocialPatterns(lowerText, domain)) {
            return ContentType.SOCIAL
        }

        // Check for map patterns
        if (containsMapPatterns(lowerText, domain)) {
            return ContentType.MAP
        }

        // Default to article if there's substantial text
        if (text.length > 200) {
            return ContentType.ARTICLE
        }

        return ContentType.UNKNOWN
    }

    private fun containsCodePatterns(text: String): Boolean {
        val codeIndicators = listOf(
            "function", "class ", "def ", "import ", "const ", "let ", "var ",
            "public ", "private ", "return ", "if (", "for (", "while (",
            "=>", "->", "::", "==", "!=", "&&", "||",
            "{", "}", "[]", "()", "//", "/*", "*/",
        )
        return codeIndicators.count { text.contains(it) } >= 3
    }

    private fun containsChatPatterns(text: String): Boolean {
        val chatIndicators = listOf(
            "sent", "delivered", "read", "typing",
            "am", "pm", "today", "yesterday",
        )
        // Check for timestamp-like patterns
        val hasTimestamps = Regex("""\d{1,2}:\d{2}""").containsMatchIn(text)
        return hasTimestamps && chatIndicators.count { text.contains(it) } >= 2
    }

    private fun containsRecipePatterns(text: String): Boolean {
        val recipeIndicators = listOf(
            "ingredients", "instructions", "recipe", "serves", "prep time",
            "cook time", "tablespoon", "teaspoon", "cup", "ounce",
            "bake", "fry", "boil", "simmer", "preheat",
        )
        return recipeIndicators.count { text.contains(it) } >= 3
    }

    private fun containsProductPatterns(text: String): Boolean {
        val productIndicators = listOf(
            "add to cart", "buy now", "price", "$", "€", "£",
            "in stock", "out of stock", "shipping", "reviews",
            "rating", "stars", "wishlist",
        )
        return productIndicators.count { text.contains(it) } >= 2
    }

    private fun containsSocialPatterns(text: String, domain: String?): Boolean {
        val socialDomains = listOf(
            "twitter", "x.com", "facebook", "instagram", "tiktok",
            "linkedin", "reddit", "threads", "mastodon",
        )
        if (domain != null && socialDomains.any { domain.contains(it) }) {
            return true
        }

        val socialIndicators = listOf(
            "like", "retweet", "share", "comment", "follow",
            "followers", "following", "reply", "quote",
        )
        return socialIndicators.count { text.contains(it) } >= 3
    }

    private fun containsMapPatterns(text: String, domain: String?): Boolean {
        val mapDomains = listOf("maps.google", "maps.apple", "waze", "openstreetmap")
        if (domain != null && mapDomains.any { domain.contains(it) }) {
            return true
        }

        val mapIndicators = listOf(
            "directions", "navigate", "route", "miles", "km",
            "min drive", "traffic", "eta",
        )
        return mapIndicators.count { text.contains(it) } >= 2
    }

    private fun extractTitle(lines: List<String>, blocks: List<TextBlock>): String {
        // If we have blocks with bounding boxes, prefer the largest text
        if (blocks.isNotEmpty()) {
            val blocksBySize = blocks
                .filter { it.boundingBox != null }
                .sortedByDescending { block ->
                    block.boundingBox?.let { it.width() * it.height() } ?: 0
                }

            // Get the first substantial block that's not too long
            for (block in blocksBySize) {
                val text = block.text.trim()
                if (text.length in 5..150 && !text.contains("\n")) {
                    return text
                }
                // Check lines within block
                for (line in block.lines) {
                    val trimmed = line.trim()
                    if (trimmed.length in 5..150) {
                        return trimmed
                    }
                }
            }
        }

        // Fallback: use first meaningful line
        for (line in lines) {
            val trimmed = line.trim()
            // Skip very short lines, URLs, or lines that look like metadata
            if (trimmed.length in 5..150 &&
                !trimmed.startsWith("http") &&
                !trimmed.matches(Regex("""^\d+[:/]\d+.*""")) && // timestamps
                !trimmed.matches(Regex("""^[\d\s\-\(\)]+$""")) // phone numbers
            ) {
                return trimmed
            }
        }

        // Last resort: truncate first line
        return lines.firstOrNull()?.take(100) ?: "Untitled Screenshot"
    }

    private fun extractSummaryBullets(lines: List<String>, title: String): List<String> {
        val bullets = mutableListOf<String>()
        val titleLower = title.lowercase()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip the title, very short lines, URLs
            if (trimmed.lowercase() == titleLower ||
                trimmed.length < 10 ||
                trimmed.startsWith("http")
            ) {
                continue
            }

            // Look for meaningful content
            if (trimmed.length in 10..200) {
                // Clean up the line
                val cleaned = trimmed
                    .removePrefix("•")
                    .removePrefix("-")
                    .removePrefix("*")
                    .trim()

                if (cleaned.length >= 10 && cleaned !in bullets) {
                    bullets.add(cleaned)
                    if (bullets.size >= 3) break
                }
            }
        }

        return bullets
    }

    private fun extractTopics(text: String): List<String> {
        val words = text.lowercase()
            .replace(Regex("""[^\w\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 4 }

        // Count word frequency
        val frequency = words.groupingBy { it }.eachCount()

        // Comprehensive stop words list - common English words that aren't useful topics
        val stopWords = setOf(
            // Articles, pronouns, prepositions
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had",
            "her", "his", "him", "she", "was", "one", "our", "out", "has", "have",
            "been", "were", "they", "this", "that", "with", "will", "your", "from",
            "more", "when", "some", "what", "there", "which", "their", "about",
            "would", "these", "other", "into", "just", "also", "than", "then",
            "only", "come", "its", "over", "such", "make", "like", "being", "here",
            "could", "after", "first", "where", "those", "does", "didn", "don",
            "each", "even", "much", "many", "most", "same", "both", "well", "back",
            "them", "very", "should", "because", "through", "before", "between",
            "under", "again", "further", "once", "during", "while", "above", "below",
            "until", "against", "down", "off", "own", "why", "how", "any", "every",

            // Common verbs
            "need", "want", "know", "think", "take", "get", "give", "go", "see",
            "look", "find", "use", "tell", "ask", "work", "seem", "feel", "try",
            "leave", "call", "keep", "let", "begin", "show", "hear", "play", "run",
            "move", "live", "believe", "hold", "bring", "happen", "write", "provide",
            "sit", "stand", "lose", "pay", "meet", "include", "continue", "set",
            "learn", "change", "lead", "understand", "watch", "follow", "stop",
            "create", "speak", "read", "allow", "add", "spend", "grow", "open",
            "walk", "win", "offer", "remember", "love", "consider", "appear", "buy",
            "wait", "serve", "die", "send", "expect", "build", "stay", "fall",
            "cut", "reach", "kill", "remain", "using", "getting", "making", "going",
            "having", "looking", "coming", "taking", "seeing", "saying", "doing",

            // Common adjectives/adverbs
            "good", "new", "used", "better", "best", "great", "little", "right",
            "still", "high", "different", "small", "large", "next", "early", "young",
            "important", "few", "public", "bad", "same", "able", "really", "already",
            "sure", "real", "long", "last", "full", "special", "free", "clear",
            "another", "always", "never", "often", "less", "actually", "probably",
            "maybe", "perhaps", "however", "though", "although", "almost", "enough",
            "quite", "rather", "something", "anything", "everything", "nothing",
            "someone", "anyone", "everyone", "else", "whether", "either", "neither",

            // Time-related
            "time", "year", "years", "day", "days", "today", "now", "week", "month",
            "hour", "hours", "minute", "minutes", "second", "seconds", "yesterday",
            "tomorrow", "morning", "night", "tonight",

            // Numbers as words
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "hundred", "thousand", "million",

            // Common UI/web words
            "click", "tap", "press", "button", "page", "link", "view", "share",
            "save", "edit", "delete", "close", "open", "menu", "home", "settings",
            "back", "next", "previous", "submit", "cancel", "okay", "done", "loading",
            "please", "enter", "select", "choose", "option", "options",

            // Common words that aren't meaningful topics
            "people", "person", "thing", "things", "place", "way", "ways", "part",
            "case", "point", "fact", "number", "group", "problem", "world", "area",
            "company", "system", "program", "question", "government", "night", "line",
            "word", "words", "text", "info", "information",
        )

        return frequency
            .filter { (word, count) ->
                count >= 2 &&
                    word !in stopWords &&
                    word.length >= 5 && // Increased from 4 to 5
                    !word.matches(Regex("""\d+""")) &&
                    !word.matches(Regex("""^[^aeiou]+$""")) && // Must have vowels (not acronyms/gibberish)
                    word.any { it in "aeiou" } // Ensure it has vowels
            }
            .entries
            .sortedByDescending { it.value }
            .take(5) // Reduced from 8 to 5 for quality over quantity
            .map { it.key.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun extractEntities(text: String): List<EntityRef> {
        // Simple heuristic: look for capitalized words that might be names/orgs
        val entities = mutableListOf<EntityRef>()

        // Look for potential names (consecutive capitalized words)
        val namePattern = Regex("""[A-Z][a-z]+(?:\s+[A-Z][a-z]+)+""")
        namePattern.findAll(text).take(5).forEach { match ->
            val name = match.value
            if (name.split(" ").size <= 4) { // Reasonable name length
                entities.add(EntityRef(
                    kind = com.screenshotvault.domain.model.EntityKind.PERSON,
                    name = name,
                ))
            }
        }

        // Look for @mentions
        val mentionPattern = Regex("""@(\w+)""")
        mentionPattern.findAll(text).take(3).forEach { match ->
            entities.add(EntityRef(
                kind = com.screenshotvault.domain.model.EntityKind.PERSON,
                name = match.groupValues[1],
            ))
        }

        return entities.distinctBy { it.name.lowercase() }.take(5)
    }

    private fun inferSuggestedAction(contentType: ContentType, text: String): SuggestedAction {
        val lowerText = text.lowercase()

        return when (contentType) {
            ContentType.ARTICLE -> SuggestedAction.READ
            ContentType.PRODUCT -> {
                if (lowerText.contains("compare") || lowerText.contains("vs")) {
                    SuggestedAction.DECIDE
                } else {
                    SuggestedAction.BUY
                }
            }
            ContentType.RECIPE -> SuggestedAction.TRY
            ContentType.CODE -> SuggestedAction.REFERENCE
            ContentType.SOCIAL -> SuggestedAction.REFERENCE
            ContentType.MAP -> SuggestedAction.REFERENCE
            ContentType.CHAT -> SuggestedAction.REFERENCE
            ContentType.UNKNOWN -> {
                when {
                    lowerText.contains("idea") || lowerText.contains("thought") ->
                        SuggestedAction.IDEA
                    lowerText.contains("decide") || lowerText.contains("choose") ->
                        SuggestedAction.DECIDE
                    else -> SuggestedAction.UNKNOWN
                }
            }
        }
    }
}
