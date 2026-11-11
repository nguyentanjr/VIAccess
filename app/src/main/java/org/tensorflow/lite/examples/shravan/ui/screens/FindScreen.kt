package org.tensorflow.lite.examples.shravan.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.tflite.Classifier
import org.tensorflow.lite.examples.shravan.tflite.YoloAnalyzer
import org.tensorflow.lite.examples.shravan.ui.theme.DimmedPalette
import org.tensorflow.lite.examples.shravan.ui.theme.InterFontFamily
import org.tensorflow.lite.examples.shravan.utils.FeedbackConfig
import org.tensorflow.lite.examples.shravan.utils.FeedbackCoordinator
import org.tensorflow.lite.examples.shravan.utils.FindDirection
import org.tensorflow.lite.examples.shravan.utils.FindEvent
import org.tensorflow.lite.examples.shravan.utils.FindFrameResult
import org.tensorflow.lite.examples.shravan.utils.FindObservation
import org.tensorflow.lite.examples.shravan.utils.FindState
import org.tensorflow.lite.examples.shravan.utils.FindStateMachine
import org.tensorflow.lite.examples.shravan.utils.FindTarget
import org.tensorflow.lite.examples.shravan.utils.HapticPattern
import org.tensorflow.lite.examples.shravan.utils.MetricLogger
import org.tensorflow.lite.examples.shravan.utils.Priority
import org.tensorflow.lite.examples.shravan.utils.SettingsManager
import org.tensorflow.lite.examples.shravan.utils.VoiceCommandManager
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTextApi::class)
@Composable
fun FindScreen(
    feedback: FeedbackCoordinator,
    voiceCommandManager: VoiceCommandManager,
    settingsManager: SettingsManager,
    isActive: Boolean = true,
    yoloAnalyzer: YoloAnalyzer? = null,
    initialTarget: FindTarget? = null,
    onInitialTargetHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageContext = remember(context, settingsManager.useVietnamese) {
        context.withLanguage(settingsManager.useVietnamese)
    }
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val textMeasurer = rememberTextMeasurer()
    val stateMachine = remember { FindStateMachine() }
    val targetReference = remember { AtomicReference<FindTarget?>(null) }

    var activeTarget by remember { mutableStateOf<FindTarget?>(null) }
    var recognitions by remember { mutableStateOf(emptyList<Classifier.Recognition>()) }
    var frameResult by remember { mutableStateOf(FindFrameResult(FindState.SEARCHING)) }

    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            activeTarget = initialTarget
            onInitialTargetHandled()
        }
    }

    DisposableEffect(isActive, settingsManager.useVietnamese) {
        if (!isActive) {
            onDispose { }
        } else {
            val sessionId = voiceCommandManager.startListening(
                isVietnamese = settingsManager.useVietnamese
            ) { command ->
                if (!feedback.isBusy()) {
                    FindTarget.fromCommand(command)?.let { activeTarget = it }
                }
            }
            onDispose { voiceCommandManager.stopListening(sessionId) }
        }
    }

    LaunchedEffect(isActive, settingsManager.useVietnamese) {
        if (isActive) {
            feedback.say(
                languageContext.getString(R.string.find_greeting),
                Priority.STATE,
                userInitiated = true
            )
            feedback.say(
                languageContext.getString(R.string.find_prompt),
                Priority.STATE,
                queue = true
            )
        }
    }

    LaunchedEffect(isActive, activeTarget, settingsManager.useVietnamese) {
        if (isActive && activeTarget != null) {
            MetricLogger.log(
                event = "find_target_selected",
                classId = activeTarget?.classId,
                baselineMode = settingsManager.baselineMode
            )
            feedback.say(
                languageContext.getString(
                    R.string.find_looking_for,
                    activeTarget?.displayLabel(languageContext).orEmpty()
                ),
                Priority.STATE,
                userInitiated = true
            )
        }
    }

    LaunchedEffect(
        isActive,
        yoloAnalyzer,
        activeTarget,
        settingsManager.useVietnamese,
        settingsManager.baselineMode
    ) {
        val analyzer = yoloAnalyzer
        if (!isActive || analyzer == null) {
            targetReference.set(null)
            analyzer?.allowedClasses = emptyList()
            analyzer?.onResults = null
            analyzer?.onFrameResults = null
            synchronized(stateMachine) { stateMachine.reset() }
            recognitions = emptyList()
            frameResult = FindFrameResult(FindState.SEARCHING)
            return@LaunchedEffect
        }

        targetReference.set(activeTarget)
        synchronized(stateMachine) { stateMachine.reset() }
        val baselineMode = settingsManager.baselineMode
        val isVietnamese = settingsManager.useVietnamese
        recognitions = emptyList()
        frameResult = FindFrameResult(FindState.SEARCHING)
        analyzer.announcementsEnabled = false
        analyzer.allowedClasses = activeTarget?.let {
            listOf(if (isVietnamese) it.vietnameseLabel else it.englishLabel)
        } ?: emptyList()
        // Vẽ hộp từ kết quả đã qua persistence (đỡ nháy); máy trạng thái dùng kết quả từng frame.
        analyzer.onResults = { persistent ->
            mainHandler.post { if (targetReference.get() != null) recognitions = persistent }
        }
        analyzer.onFrameResults = { frameId, results ->
            val target = targetReference.get()
            val inputSize = analyzer.inputSize
            val observations = if (target == null || inputSize <= 0) {
                emptyList()
            } else {
                results.asSequence()
                    .filter { it.detectedClass == target.classId && it.location != null }
                    .map { recognition ->
                        val bounds = recognition.location
                        FindObservation(
                            classId = recognition.detectedClass,
                            confidence = recognition.confidence,
                            centerX = ((bounds.left + bounds.right) / 2f / inputSize)
                                .coerceIn(0f, 1f),
                            areaFraction = (
                                bounds.width() * bounds.height() /
                                    (inputSize.toFloat() * inputSize)
                                ).coerceIn(0f, 1f)
                        )
                    }
                    .toList()
            }

            val result = synchronized(stateMachine) {
                if (target == null) {
                    FindFrameResult(FindState.SEARCHING)
                } else if (baselineMode) {
                    val observation = observations.maxByOrNull { it.confidence }
                    FindFrameResult(
                        state = if (observation == null) {
                            FindState.SEARCHING
                        } else {
                            FindState.TRACKING
                        },
                        observation = observation,
                        hapticIntervalMs = observation?.trackingIntervalMs()
                    )
                } else {
                    stateMachine.update(observations, target.classId, android.os.SystemClock.elapsedRealtime(), target.minConfidence)
                }
            }

            mainHandler.post {
                if (targetReference.get() != target) return@post
                frameResult = result

                if (target != null && baselineMode) {
                    if (result.observation != null) {
                        MetricLogger.log(
                            event = "baseline_detection",
                            frameId = frameId,
                            classId = target.classId,
                            confidence = result.observation.confidence,
                            baselineMode = true
                        )
                        MetricLogger.log(
                            event = "tts_request",
                            frameId = frameId,
                            classId = target.classId,
                            confidence = result.observation.confidence,
                            baselineMode = true,
                            detail = "baseline_detection"
                        )
                        feedback.say(
                            target.displayLabel(languageContext),
                            Priority.INFO,
                            key = "baseline-find:${target.classId}"
                        )
                    }
                } else if (target != null) {
                    handleFindEvent(
                        result = result,
                        target = target,
                        frameId = frameId,
                        context = languageContext,
                        feedback = feedback
                    )
                }
            }
        }
    }

    DisposableEffect(isActive, yoloAnalyzer) {
        onDispose {
            yoloAnalyzer?.onResults = null
            yoloAnalyzer?.onFrameResults = null
            yoloAnalyzer?.allowedClasses = emptyList()
            yoloAnalyzer?.announcementsEnabled = false
        }
    }

    LaunchedEffect(
        isActive,
        activeTarget,
        frameResult.state,
        settingsManager.baselineMode
    ) {
        if (!isActive || activeTarget == null) return@LaunchedEffect

        while (true) {
            val currentResult = frameResult
            val pulseInterval = when {
                settingsManager.baselineMode && currentResult.observation != null ->
                    currentResult.hapticIntervalMs ?: FeedbackConfig.HAPTIC_INTERVAL_FAR_MS
                !settingsManager.baselineMode && currentResult.state == FindState.SEARCHING ->
                    FeedbackConfig.FIND_SEARCH_PULSE_MS
                !settingsManager.baselineMode &&
                    (currentResult.state == FindState.TRACKING ||
                        currentResult.state == FindState.CENTERED) ->
                    currentResult.hapticIntervalMs ?: FeedbackConfig.HAPTIC_INTERVAL_FAR_MS
                else -> return@LaunchedEffect
            }
            feedback.vibrate(HapticPattern.SHORT)
            delay(pulseInterval)
        }
    }

    val targetLabel = activeTarget?.displayLabel(languageContext)
    val stateLabel = when {
        activeTarget == null -> languageContext.getString(R.string.find_choose_target)
        settingsManager.baselineMode -> languageContext.getString(R.string.find_baseline_mode)
        frameResult.state == FindState.SEARCHING ->
            languageContext.getString(R.string.find_searching, targetLabel.orEmpty())
        frameResult.state == FindState.CENTERED ->
            languageContext.getString(R.string.find_centered)
        else -> languageContext.getString(R.string.find_tracking, targetLabel.orEmpty())
    }

    // Nền trong suốt: camera được vẽ ở lớp dưới (MainScreen).
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inputSize = yoloAnalyzer?.inputSize?.takeIf { it > 0 } ?: return@Canvas
            recognitions.forEach { recognition ->
                if (recognition.detectedClass != activeTarget?.classId) return@forEach
                val bounds = recognition.location
                val scaleX = size.width / inputSize
                val scaleY = size.height / inputSize
                val color = DimmedPalette[recognition.detectedClass % DimmedPalette.size]
                val topLeft = Offset(bounds.left * scaleX, bounds.top * scaleY)
                val boxSize = Size(bounds.width() * scaleX, bounds.height() * scaleY)
                drawRect(color, topLeft, boxSize, style = Stroke(width = 3.dp.toPx()))
                drawText(
                    textMeasurer = textMeasurer,
                    text = recognition.title,
                    topLeft = Offset(topLeft.x, topLeft.y - 22.dp.toPx()),
                    style = TextStyle(
                        color = color,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.78f))
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Công tắc chế độ so sánh và nút xuất số liệu đã chuyển vào Cài đặt.
            Text(
                text = stateLabel,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 17 vật: lưới 3 cột, cuộn được, cao tối đa ~3 hàng để không che camera.
            // 6 vật gốc xếp trước.
            Column(
                modifier = Modifier
                    .heightIn(max = 196.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FindTarget.entries.chunked(3).forEach { rowTargets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTargets.forEach { target ->
                            val selected = activeTarget == target
                            Button(
                                onClick = { activeTarget = target },
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    contentColor = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            ) {
                                Text(
                                    text = target.displayName,
                                    fontSize = 18.sp,
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        repeat(3 - rowTargets.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private fun handleFindEvent(
    result: FindFrameResult,
    target: FindTarget,
    frameId: Long,
    context: Context,
    feedback: FeedbackCoordinator
) {
    val observation = result.observation
    when (result.event) {
        FindEvent.FOUND -> {
            val direction = directionResource(result.direction)
            MetricLogger.log(
                event = "find_found",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                latencyMs = null,
                baselineMode = false,
                detail = result.direction?.name.orEmpty()
            )
            val message = context.getString(
                R.string.find_found,
                target.displayLabel(context),
                context.getString(direction)
            )
            MetricLogger.log(
                event = "tts_request",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                baselineMode = false,
                detail = "found"
            )
            feedback.say(
                message,
                Priority.STATE,
                key = "find-found:${target.classId}",
                onDone = { feedback.vibrate(HapticPattern.LONG) }
            )
        }

        FindEvent.DIRECTION_CHANGED -> {
            val direction = directionResource(result.direction)
            MetricLogger.log(
                event = "find_direction_changed",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                baselineMode = false,
                detail = result.direction?.name.orEmpty()
            )
            MetricLogger.log(
                event = "tts_request",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                baselineMode = false,
                detail = "direction"
            )
            feedback.say(
                context.getString(direction),
                Priority.STATE,
                key = "find-direction:${target.classId}:${result.direction}"
            )
        }

        FindEvent.CENTERED -> {
            MetricLogger.log(
                event = "find_centered",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                baselineMode = false
            )
            MetricLogger.log(
                event = "tts_request",
                frameId = frameId,
                classId = target.classId,
                confidence = observation?.confidence,
                baselineMode = false,
                detail = "centered"
            )
            feedback.say(
                context.getString(R.string.find_centered),
                Priority.STATE,
                key = "find-centered:${target.classId}"
            )
        }

        FindEvent.LOST -> {
            MetricLogger.log(
                event = "find_lost",
                frameId = frameId,
                classId = target.classId,
                baselineMode = false
            )
            MetricLogger.log(
                event = "tts_request",
                frameId = frameId,
                classId = target.classId,
                baselineMode = false,
                detail = "lost"
            )
            feedback.say(
                context.getString(
                    R.string.find_lost,
                    target.displayLabel(context)
                ),
                Priority.STATE,
                key = "find-lost:${target.classId}",
                onDone = { feedback.vibrate(HapticPattern.DOUBLE) }
            )
        }

        FindEvent.NONE -> Unit
    }
}

private fun directionResource(direction: FindDirection?): Int = when (direction) {
    FindDirection.LEFT -> R.string.find_direction_left
    FindDirection.CENTER -> R.string.find_direction_center
    FindDirection.RIGHT -> R.string.find_direction_right
    null -> R.string.find_direction_center
}

@Suppress("UNUSED_PARAMETER")
/** Tên vật dùng trong câu nói / dòng trạng thái ("Đang tìm chuột"); nút dùng [FindTarget.displayName]. */
private fun FindTarget.displayLabel(context: Context): String = displayName.replaceFirstChar { it.lowercase() }

private fun Context.withLanguage(isVietnamese: Boolean): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(if (isVietnamese) Locale("vi", "VN") else Locale.US)
    return createConfigurationContext(configuration)
}

private fun FindObservation.trackingIntervalMs(): Long =
    (FeedbackConfig.HAPTIC_INTERVAL_FAR_MS -
        FeedbackConfig.FIND_HAPTIC_AREA_SCALE_MS * areaFraction.coerceIn(0f, 1f))
        .toLong()
        .coerceIn(
            FeedbackConfig.HAPTIC_INTERVAL_NEAR_MS,
            FeedbackConfig.HAPTIC_INTERVAL_FAR_MS
        )
