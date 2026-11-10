package org.tensorflow.lite.examples.shravan.utils

import androidx.camera.core.ImageProxy

/** Khung chữ, toạ độ chuẩn hoá 0..1 theo ảnh đã xoay đúng chiều. */
data class TextBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2f
    val area: Float get() = ((right - left) * (bottom - top)).coerceIn(0f, 1f)
    /** Khoảng cách nhỏ nhất từ khung tới mép ảnh. */
    val edgeMargin: Float get() = minOf(left, top, 1f - right, 1f - bottom)
}

/** Ảnh xám thu nhỏ lấy từ kênh Y của camera. */
class GrayImage(val width: Int, val height: Int, val pixels: IntArray) {
    operator fun get(x: Int, y: Int): Int = pixels[y * width + x]
}

/** Đo chất lượng ảnh cho OCR (PLAN 4.2): độ sáng, độ nét (Laplacian), khung chữ. */
object ImageQuality {

    /** Lấy mẫu kênh Y (mỗi [step] điểm ảnh) mà không làm dịch vị trí buffer (ML Kit còn đọc tiếp). */
    fun sampleLuma(image: ImageProxy, step: Int = FeedbackConfig.QUALITY_SAMPLE_STEP): GrayImage {
        val plane = image.planes[0]
        val buffer = plane.buffer.duplicate().apply { rewind() }
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val w = image.width / step
        val h = image.height / step
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * step * rowStride
            for (x in 0 until w) {
                val idx = row + x * step * pixelStride
                out[y * w + x] = if (idx < buffer.limit()) buffer.get(idx).toInt() and 0xFF else 0
            }
        }
        return GrayImage(w, h, out)
    }

    fun meanLuminance(img: GrayImage): Double =
        if (img.pixels.isEmpty()) 0.0 else img.pixels.average()

    /**
     * Phương sai của Laplacian (nhân 4-lân cận). Ảnh mờ → giá trị nhỏ.
     * Tính trên ảnh đã thu nhỏ nên ngưỡng [FeedbackConfig.BLUR_LAPLACIAN_MIN] cần hiệu chỉnh trên máy thật.
     */
    fun laplacianVariance(img: GrayImage): Double {
        if (img.width < 3 || img.height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until img.height - 1) {
            for (x in 1 until img.width - 1) {
                val lap = (img[x - 1, y] + img[x + 1, y] + img[x, y - 1] + img[x, y + 1] - 4 * img[x, y]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    /**
     * Gộp các khung chữ (pixel, theo ảnh đã xoay) thành một khung chuẩn hoá.
     * @param rects danh sách (left, top, right, bottom)
     */
    fun unionBox(rects: List<IntArray>, imageWidth: Int, imageHeight: Int): TextBox? {
        if (rects.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return null
        val l = rects.minOf { it[0] }
        val t = rects.minOf { it[1] }
        val r = rects.maxOf { it[2] }
        val b = rects.maxOf { it[3] }
        return TextBox(
            (l.toFloat() / imageWidth).coerceIn(0f, 1f),
            (t.toFloat() / imageHeight).coerceIn(0f, 1f),
            (r.toFloat() / imageWidth).coerceIn(0f, 1f),
            (b.toFloat() / imageHeight).coerceIn(0f, 1f)
        )
    }
}
