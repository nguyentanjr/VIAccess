package org.tensorflow.lite.examples.shravan.utils

import androidx.camera.core.ImageProxy

object DarknessDetector {
    fun calculateLuminance(image: ImageProxy): Double {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var sum = 0L
        for (b in data) {
            sum += b.toInt() and 0xFF
        }
        return sum.toDouble() / data.size
    }

    fun isDark(image: ImageProxy, threshold: Double = 40.0): Boolean {
        return calculateLuminance(image) < threshold
    }
}
