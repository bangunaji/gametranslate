package com.example.rpgtranslator.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    private var lastExtractedText = ""
    private var lastTextTimestamp = 0L

    suspend fun processBitmap(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val result = recognizer.process(image).await()
            val currentText = result.text.trim().replace("\\n", " ")

            if (currentText.isEmpty()) {
                return OcrResult(currentText, isStable = false, isNew = false)
            }

            val isNew = currentText != lastExtractedText
            
            if (isNew) {
                lastExtractedText = currentText
                lastTextTimestamp = System.currentTimeMillis()
            }

            val timeStable = System.currentTimeMillis() - lastTextTimestamp
            return OcrResult(
                text = currentText,
                isStable = timeStable > 500,
                isNew = isNew
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return OcrResult("", isStable = false, isNew = false)
        }
    }
}

data class OcrResult(
    val text: String,
    val isStable: Boolean,
    val isNew: Boolean
)
