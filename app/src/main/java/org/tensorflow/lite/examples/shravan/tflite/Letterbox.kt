package org.tensorflow.lite.examples.shravan.tflite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Co ảnh vào khung vuông [size]×[size] mà giữ tỉ lệ (letterbox), phần thừa tô xám 114 như YOLOv5.
 * Phần tính toán không phụ thuộc Android để unit test được.
 */
data class Letterbox(
    val size: Int,
    val padX: Float,
    val padY: Float,
    val contentW: Float,
    val contentH: Float
) {
    /** Toạ độ trong ảnh letterbox → toạ độ khi cả ảnh gốc được co về size×size (cách cũ). */
    fun toFullFrame(x: Float, y: Float): Pair<Float, Float> =
        ((x - padX) / contentW * size).coerceIn(0f, size.toFloat()) to
            ((y - padY) / contentH * size).coerceIn(0f, size.toFloat())

    fun toFullFrame(r: RectF): RectF {
        val (l, t) = toFullFrame(r.left, r.top)
        val (rr, b) = toFullFrame(r.right, r.bottom)
        return RectF(l, t, rr, b)
    }

    companion object {
        fun of(srcW: Int, srcH: Int, size: Int): Letterbox {
            val scale = minOf(size.toFloat() / srcW, size.toFloat() / srcH)
            val w = srcW * scale
            val h = srcH * scale
            return Letterbox(size, (size - w) / 2f, (size - h) / 2f, w, h)
        }

        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        fun apply(src: Bitmap, box: Letterbox): Bitmap {
            val out = Bitmap.createBitmap(box.size, box.size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.rgb(114, 114, 114))
            val dst = Rect(
                box.padX.toInt(), box.padY.toInt(),
                (box.padX + box.contentW).toInt(), (box.padY + box.contentH).toInt()
            )
            canvas.drawBitmap(src, null, dst, paint)
            return out
        }
    }
}
