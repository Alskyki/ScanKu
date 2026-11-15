package com.scanku.app.core

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** OCR di perangkat (offline) memakai ML Kit. Mendukung huruf Latin: Indonesia, Inggris, dll. */
object OcrHelper {

    suspend fun recognize(file: File, rotationDegrees: Int): String {
        val bitmap = withContext(Dispatchers.IO) { ImageProcessor.decode(file, 2400) }
        val image = InputImage.fromBitmap(bitmap, ((rotationDegrees % 360) + 360) % 360)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(image).await().text
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }
}
