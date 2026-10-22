package org.tensorflow.lite.examples.shravan.tflite

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.examples.shravan.utils.DarknessDetector
import org.tensorflow.lite.examples.shravan.utils.SettingsManager

class CompositeAnalyzer(
    private val settingsManager: SettingsManager,
    private val onDarknessDetected: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    var delegate: ImageAnalysis.Analyzer? = null
    private var isTorchCurrentlyOn = false

    override fun analyze(image: ImageProxy) {
        try {
            // 1. Darkness detection (always runs when camera is active)
            if (settingsManager.flashMode == "auto") {
                val luminance = DarknessDetector.calculateLuminance(image)
                
                // Hysteresis logic:
                // If torch is OFF: turn ON if luminance < 40
                // If torch is ON: turn OFF only if luminance > 120 (it sees its own light)
                val isDark = if (!isTorchCurrentlyOn) {
                    luminance < 40.0
                } else {
                    luminance < 120.0 
                }
                
                if (isDark != isTorchCurrentlyOn) {
                    isTorchCurrentlyOn = isDark
                    onDarknessDetected(isDark)
                }
            } else {
                isTorchCurrentlyOn = settingsManager.flashMode == "on"
            }
            
            // 2. Delegate to screen-specific analyzer
            val currentDelegate = delegate
            if (currentDelegate != null) {
                currentDelegate.analyze(image)
            } else {
                image.close()
            }
        } catch (e: Exception) {
            Log.e("CompositeAnalyzer", "Error in analyze", e)
            image.close()
        }
    }
}
