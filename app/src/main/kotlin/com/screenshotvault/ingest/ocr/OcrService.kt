package com.screenshotvault.ingest.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val blocks: List<TextBlock>,
)

data class TextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val lines: List<String>,
)

@Singleton
class OcrService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(uri: Uri): OcrResult? = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(uri) ?: return@withContext null
        extractTextFromBitmap(bitmap)
    }

    suspend fun extractText(imageBytes: ByteArray): OcrResult? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return@withContext null
        extractTextFromBitmap(bitmap)
    }

    private suspend fun extractTextFromBitmap(bitmap: Bitmap): OcrResult? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = recognizeText(inputImage) ?: return null
            parseResult(result)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun recognizeText(inputImage: InputImage): Text? =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener { _ ->
                    continuation.resume(null)
                }
        }

    private fun parseResult(text: Text): OcrResult {
        val blocks = text.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox,
                lines = block.lines.map { it.text },
            )
        }
        return OcrResult(
            fullText = text.text,
            blocks = blocks,
        )
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
