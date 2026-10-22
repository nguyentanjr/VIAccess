package org.tensorflow.lite.examples.shravan.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun toBitmap(image: ImageProxy): Bitmap? {
        val imageBytes = toJpegBytes(image)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun toRotatedBitmap(image: ImageProxy): Bitmap? {
        val bitmap = toBitmap(image) ?: return null
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    fun toJpegBytes(image: ImageProxy): ByteArray {
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 80, out)
        return out.toByteArray()
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.width * image.height
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        val bufferY = image.planes[0].buffer
        val bufferU = image.planes[1].buffer
        val bufferV = image.planes[2].buffer

        val rowStrideY = image.planes[0].rowStride
        val rowStrideU = image.planes[1].rowStride
        val rowStrideV = image.planes[2].rowStride

        val pixelStrideY = image.planes[0].pixelStride
        val pixelStrideU = image.planes[1].pixelStride
        val pixelStrideV = image.planes[2].pixelStride

        var pos = 0
        for (row in 0 until image.height) {
            for (col in 0 until image.width) {
                outputBuffer[pos++] = bufferY.get(row * rowStrideY + col * pixelStrideY)
            }
        }

        for (row in 0 until image.height / 2) {
            for (col in 0 until image.width / 2) {
                outputBuffer[pos++] = bufferV.get(row * rowStrideV + col * pixelStrideV)
                outputBuffer[pos++] = bufferU.get(row * rowStrideU + col * pixelStrideU)
            }
        }

        return outputBuffer
    }
}
