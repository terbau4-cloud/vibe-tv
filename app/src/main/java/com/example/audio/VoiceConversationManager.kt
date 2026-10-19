package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceConversationManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isVoiceEnabled: Boolean = true

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error initializing TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.language = Locale.US
                isInitialized = true
            } catch (e: Exception) {
                Log.e("VoiceManager", "Language set failed: ${e.message}")
            }
        }
    }

    fun speak(text: String, countryCode: String = "US") {
        if (!isVoiceEnabled || !isInitialized || tts == null) return

        try {
            val targetLocale = when (countryCode.uppercase(Locale.US)) {
                "US" -> Locale.US
                "GB" -> Locale.UK
                "FR" -> Locale.FRANCE
                "DE" -> Locale.GERMANY
                "IT" -> Locale.ITALY
                "JP" -> Locale.JAPAN
                "KR" -> Locale.KOREA
                "BR" -> Locale("pt", "BR")
                "ES", "MX" -> Locale("es", "ES")
                "IN" -> Locale("en", "IN")
                else -> Locale.US
            }

            tts?.language = targetLocale
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.05f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "partner_speech_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceManager", "TTS speak error: ${e.message}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
