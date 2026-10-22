package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.media.audiofx.LoudnessEnhancer
import java.util.Locale

class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val localeVi = Locale("vi", "VN")
    private var lastSpokenText: String = ""
    private var lastIsVietnamese: Boolean = false
    private var currentSpeechRate: Float = 1.0f
    private var onCompletionListener: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioSessionId: Int = -1

    private data class PendingSpeakRequest(
        val text: String,
        val isQueued: Boolean,
        val isVietnamese: Boolean,
        val onComplete: (() -> Unit)?
    )
    private val pendingRequests = mutableListOf<PendingSpeakRequest>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Attempt to set default language, but don't block initialization if it fails
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTSManager", "Locale.US not supported, using default")
                tts?.setLanguage(Locale.getDefault())
            }
            
            tts?.setSpeechRate(currentSpeechRate)
            setupProgressListener()
            isInitialized = true
            
            // Process any requests that came in before initialization
            mainHandler.post {
                val requestsToPlay = ArrayList(pendingRequests)
                pendingRequests.clear()
                for (request in requestsToPlay) {
                    speak(request.text, request.isQueued, request.isVietnamese, request.onComplete)
                }
            }
        } else {
            Log.e("TTSManager", "Initialization Failed!")
            // Fail gracefully by calling onComplete for pending requests
            mainHandler.post {
                val requestsToPlay = ArrayList(pendingRequests)
                pendingRequests.clear()
                for (request in requestsToPlay) {
                    request.onComplete?.invoke()
                }
            }
        }
    }

    private var lastSpeechEndTime: Long = 0

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                lastSpeechEndTime = System.currentTimeMillis()
                mainHandler.post {
                    onCompletionListener?.invoke()
                    onCompletionListener = null
                }
            }
            override fun onError(utteranceId: String?) {
                Log.e("TTSManager", "Utterance error: $utteranceId")
                lastSpeechEndTime = System.currentTimeMillis()
                mainHandler.post {
                    onCompletionListener?.invoke()
                    onCompletionListener = null
                }
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                lastSpeechEndTime = System.currentTimeMillis()
                mainHandler.post {
                    onCompletionListener?.invoke()
                    onCompletionListener = null
                }
            }
        })
    }

    fun setLanguage(isVietnamese: Boolean) {
        lastIsVietnamese = isVietnamese
        if (!isInitialized) return

        val locale = if (isVietnamese) localeVi else Locale.US
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTSManager", "Language not supported: $locale")
            if (isVietnamese) {
                // Fallback attempt with generic "vi"
                tts?.setLanguage(Locale("vi"))
            }
        }
        // Re-apply speech rate as setLanguage can sometimes reset it
        tts?.setSpeechRate(currentSpeechRate)
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun speak(text: String, isQueued: Boolean = false, isVietnamese: Boolean = false, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onComplete?.invoke()
            return
        }
        if (isInitialized) {
            this.onCompletionListener = onComplete
            lastSpokenText = text
            lastIsVietnamese = isVietnamese
            setLanguage(isVietnamese)
            
            val queueMode = if (isQueued) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
            
            val params = Bundle()
            try {
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id")
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            } catch (e: Exception) {
                Log.e("TTSManager", "Error setting audio parameters", e)
            }
            
            try {
                val result = tts?.speak(text, queueMode, params, "id")
                if (result == TextToSpeech.ERROR) {
                    Log.e("TTSManager", "speak call returned TextToSpeech.ERROR")
                    mainHandler.post {
                        onComplete?.invoke()
                        onCompletionListener = null
                    }
                }
            } catch (e: Exception) {
                Log.e("TTSManager", "Error in speak call", e)
                mainHandler.post {
                    onComplete?.invoke()
                    onCompletionListener = null
                }
            }
        } else {
            pendingRequests.add(PendingSpeakRequest(text, isQueued, isVietnamese, onComplete))
        }
    }

    fun stopAll() {
        pendingRequests.clear()
        tts?.stop()
    }

    fun repeatLast() {
        if (lastSpokenText.isNotBlank()) {
            speak(lastSpokenText, isQueued = false, isVietnamese = lastIsVietnamese)
        }
    }

    fun isSpeaking(): Boolean {
        val currentlySpeaking = tts?.isSpeaking ?: false
        if (currentlySpeaking) return true
        
        val timeSinceLastSpeech = System.currentTimeMillis() - lastSpeechEndTime
        return timeSinceLastSpeech < 500 // 500ms buffer to prevent voice command interference
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}
