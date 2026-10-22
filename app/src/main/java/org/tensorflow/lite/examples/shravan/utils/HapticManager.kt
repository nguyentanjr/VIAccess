package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(private val context: Context, private val settingsManager: SettingsManager) : HapticOutput {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun triggerHaptic() {
        if (!settingsManager.hapticsEnabled) return
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(FeedbackConfig.HAPTIC_DEFAULT_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(FeedbackConfig.HAPTIC_DEFAULT_MS)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HapticManager", "Failed to trigger haptic", e)
        }
    }

    override fun vibrate(pattern: HapticPattern) {
        if (!settingsManager.hapticsEnabled) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (pattern) {
                    HapticPattern.SHORT -> VibrationEffect.createOneShot(FeedbackConfig.HAPTIC_SHORT_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.DEFAULT -> VibrationEffect.createOneShot(FeedbackConfig.HAPTIC_DEFAULT_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.LONG -> VibrationEffect.createOneShot(FeedbackConfig.HAPTIC_LONG_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.DOUBLE -> VibrationEffect.createWaveform(
                        longArrayOf(0, FeedbackConfig.HAPTIC_DEFAULT_MS, FeedbackConfig.HAPTIC_DEFAULT_MS, FeedbackConfig.HAPTIC_DEFAULT_MS), -1
                    )
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (pattern) {
                    HapticPattern.SHORT -> v.vibrate(FeedbackConfig.HAPTIC_SHORT_MS)
                    HapticPattern.DEFAULT -> v.vibrate(FeedbackConfig.HAPTIC_DEFAULT_MS)
                    HapticPattern.LONG -> v.vibrate(FeedbackConfig.HAPTIC_LONG_MS)
                    HapticPattern.DOUBLE -> v.vibrate(
                        longArrayOf(0, FeedbackConfig.HAPTIC_DEFAULT_MS, FeedbackConfig.HAPTIC_DEFAULT_MS, FeedbackConfig.HAPTIC_DEFAULT_MS), -1
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HapticManager", "Failed to vibrate $pattern", e)
        }
    }
}
