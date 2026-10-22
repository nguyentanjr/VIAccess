package org.tensorflow.lite.examples.shravan.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.examples.shravan.utils.FeedbackConfig
import org.tensorflow.lite.examples.shravan.utils.FeedbackCoordinator
import org.tensorflow.lite.examples.shravan.utils.Priority
import org.tensorflow.lite.examples.shravan.utils.ImageUtils
import org.tensorflow.lite.examples.shravan.utils.SettingsManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class YoloAnalyzer(
    private val context: Context,
    private val feedback: FeedbackCoordinator,
    private val settingsManager: SettingsManager,
    private val modelName: String = "yolov5s-fp16.tflite",
    var onResults: ((List<Classifier.Recognition>) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    var allowedClasses: List<String>? = null

    private var currentDetector: YoloV5Classifier? = null
    private var currentLabels: List<String>? = null
    private var lastUsedLanguage: Boolean? = null

    private fun getDetector(): YoloV5Classifier {
        val useVi = settingsManager.useVietnamese
        if (currentDetector == null || lastUsedLanguage != useVi) {
            val labelFile = when {
                useVi -> "file:///android_asset/coco_vi.txt"
                else -> "file:///android_asset/coco.txt"
            }
            currentDetector?.close()
            currentDetector = DetectorFactory.getDetector(context.assets, modelName, labelFile)
            lastUsedLanguage = useVi
            // Refresh labels as well
            currentLabels = loadLabels()
        }
        return currentDetector!!
    }

    private fun loadLabels(): List<String> {
        val labelsList = mutableListOf<String>()
        val useVi = settingsManager.useVietnamese
        val filename = when {
            useVi -> "coco_vi.txt"
            else -> "coco.txt"
        }
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open(filename)))
            var line: String? = reader.readLine()
            while (line != null) {
                labelsList.add(line)
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return labelsList
    }

    private val labels: List<String> get() = currentLabels ?: loadLabels().also { currentLabels = it }

    private val lastSeenTime = mutableMapOf<String, Long>()
    private var isClosed = false
    private val lock = Any()
    
    // Persistence state
    private var persistentRecognitions = mutableListOf<Classifier.Recognition>()
    private val frameCountMap = mutableMapOf<String, Int>()
    private val PERSISTENCE_THRESHOLD = FeedbackConfig.YOLO_PERSISTENCE_FRAMES
    private var frameCounter = 0

    override fun analyze(image: ImageProxy) {
        synchronized(lock) {
            if (isClosed) {
                image.close()
                return
            }
            frameCounter++
            if (frameCounter % FeedbackConfig.YOLO_FRAME_SKIP != 0) {
                image.close()
                return
            }
            try {
                val detector = getDetector()
                val bitmap = ImageUtils.toBitmap(image)
                if (bitmap == null) {
                    return
                }

                val matrix = Matrix()
                matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                val scaledBitmap = Bitmap.createScaledBitmap(
                    rotatedBitmap, detector.inputSize, detector.inputSize, true
                )

                val results = detector.recognizeImage(scaledBitmap)
                val baseFiltered = results.filter { it.confidence > FeedbackConfig.YOLO_MIN_CONFIDENCE }
                
                // Totally Blind Optimization: dynamic debounce
                val currentDebounce = if (settingsManager.impairmentLevel == org.tensorflow.lite.examples.shravan.utils.ImpairmentLevel.TotallyImpaired) {
                    FeedbackConfig.YOLO_DEBOUNCE_TOTALLY_BLIND_MS
                } else {
                    FeedbackConfig.YOLO_DEBOUNCE_PARTIAL_MS
                }

                val currentLabelsList = labels
                val currentFiltered = if (allowedClasses != null) {
                    baseFiltered.filter { result ->
                        val title = if (result.detectedClass < currentLabelsList.size) currentLabelsList[result.detectedClass] else result.title
                        allowedClasses!!.contains(title.lowercase())
                    }
                } else {
                    baseFiltered
                }

                // Apply Persistence Logic
                val newPersistentList = mutableListOf<Classifier.Recognition>()
                val activeTitles = currentFiltered.map { it.title }.toSet()
                
                // 1. Add current frame results
                currentFiltered.forEach { recognition ->
                    newPersistentList.add(recognition)
                    frameCountMap[recognition.title] = PERSISTENCE_THRESHOLD
                }
                
                // 2. Carry over missing objects that are still within the threshold
                persistentRecognitions.forEach { old ->
                    if (!activeTitles.contains(old.title)) {
                        val count = frameCountMap[old.title] ?: 0
                        if (count > 0) {
                            newPersistentList.add(old)
                            frameCountMap[old.title] = count - 1
                        }
                    }
                }
                
                persistentRecognitions = newPersistentList

                val currentTime = System.currentTimeMillis()
                persistentRecognitions.forEach { result ->
                    val detectedClass = result.detectedClass
                    val title = if (detectedClass < currentLabelsList.size) currentLabelsList[detectedClass] else result.title
                    
                    val announcement = title

                    val lastSeen = lastSeenTime[announcement] ?: 0L
                    if (currentTime - lastSeen > currentDebounce) {
                        feedback.say(announcement, Priority.INFO, key = "object:$announcement")
                    }
                    lastSeenTime[announcement] = currentTime
                }

                // Clean up old entries to prevent memory leak
                lastSeenTime.entries.removeIf { currentTime - it.value > FeedbackConfig.YOLO_SEEN_CACHE_MS }

                onResults?.invoke(persistentRecognitions)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }
    }

    fun close() {
        synchronized(lock) {
            isClosed = true
            try {
                currentDetector?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
