package com.example.rpgtranslator.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TranslationClient {

    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        delay(300)
        
        if (text.contains("こんにちは")) return@withContext "Halo"
        if (text.contains("ありがとう")) return@withContext "Terima kasih"
        if (text.contains("RPG")) return@withContext "Game RPG"
        
        return@withContext "Translated: [ $text ]"
    }
}
