package org.tensorflow.lite.examples.shravan.utils

/** Nối FeedbackCoordinator với TTSManager / HapticManager thật. */
fun createFeedbackCoordinator(
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    settingsManager: SettingsManager
): FeedbackCoordinator = FeedbackCoordinator(
    speech = object : SpeechOutput {
        override fun speak(text: String, isVietnamese: Boolean, onDone: (() -> Unit)?) =
            ttsManager.speak(text, isQueued = false, isVietnamese = isVietnamese, onComplete = onDone)
        override fun isSpeaking(): Boolean = ttsManager.isSpeaking()
        override fun stop() = ttsManager.stopAll()
    },
    haptics = hapticManager,
    defaultVietnamese = { settingsManager.useVietnamese }
)
