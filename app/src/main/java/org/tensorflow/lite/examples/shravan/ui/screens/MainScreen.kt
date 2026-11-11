package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.camera.core.ImageAnalysis
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.tflite.CompositeAnalyzer
import org.tensorflow.lite.examples.shravan.tflite.YoloAnalyzer
import org.tensorflow.lite.examples.shravan.ui.components.CameraPreview
import org.tensorflow.lite.examples.shravan.utils.*

private const val PAGE_FIND = 0
private const val PAGE_OCR = 1
private const val PAGE_SETTINGS = 2

private data class BarItem(
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    feedback: FeedbackCoordinator,
    voiceCommandManager: VoiceCommandManager,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var pendingFindTarget by remember { mutableStateOf<FindTarget?>(null) }
    val density = LocalDensity.current
    val swipeThreshold = remember { with(density) { 50.dp.toPx() } }
    var totalDrag by remember { mutableStateOf(0f) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    
    // Shared state for reset reddening
    var resetCount by remember { mutableIntStateOf(0) }
    val redAlpha = (resetCount * 0.14f).coerceIn(0f, 1f)
    val navBarColor = if (resetCount > 0) Color.Red.copy(alpha = redAlpha) else Color.Black

    LaunchedEffect(currentPage) {
        if (currentPage != PAGE_SETTINGS) resetCount = 0
    }
    val ocrManager = remember { OCRManager(context) }
    val yoloAnalyzer = remember { 
        YoloAnalyzer(context, feedback, settingsManager)
    }
    
    var ocrAnalyzer by remember { mutableStateOf<ImageAnalysis.Analyzer?>(null) }

    val stableCompositeAnalyzer = remember {
        CompositeAnalyzer(
            settingsManager = settingsManager,
            onDarknessDetected = { isDark ->
                if (settingsManager.flashMode == "auto" || settingsManager.impairmentLevel == ImpairmentLevel.TotallyImpaired) {
                    isTorchEnabled = isDark
                }
            }
        )
    }
// Update the delegate reactively
LaunchedEffect(currentPage, ocrAnalyzer) {
    stableCompositeAnalyzer.delegate = when (currentPage) {
        PAGE_FIND -> yoloAnalyzer // FindScreen sets allowedClasses
        PAGE_OCR -> ocrAnalyzer
        else -> null
    }
}

val activeAnalyzer = if (currentPage != PAGE_SETTINGS) stableCompositeAnalyzer else null

    // Centralized Flash Control
    LaunchedEffect(settingsManager.flashMode) {
        isTorchEnabled = when (settingsManager.flashMode) {
            "on" -> true
            "off" -> false
            else -> isTorchEnabled // keep current auto state or default to false
        }
    }

    val screens = listOf(
        Triple(R.string.nav_find, Icons.Default.Search, "find"),
        Triple(R.string.nav_ocr, Icons.Default.TextFields, "ocr"),
        Triple(R.string.nav_settings, Icons.Default.Settings, "settings")
    )

    val helpFormat = stringResource(R.string.voice_help_commands)
    val sNotUnderstood = stringResource(R.string.voice_not_understood)
    val sUseTouch = stringResource(R.string.voice_use_touch)
    val sPaused = stringResource(R.string.voice_paused)
    val sNothingPaused = stringResource(R.string.voice_nothing_paused)
    val sNothingToRepeat = stringResource(R.string.voice_nothing_to_repeat)
    val sNoSentence = stringResource(R.string.voice_no_sentence)
    val sExploreNothing = stringResource(R.string.explore_nothing)
    val sExploreLooking = stringResource(R.string.explore_looking)
    val sAlreadyHome = stringResource(R.string.voice_already_home)
    val sNetwork = stringResource(R.string.error_network)
    val sVoiceUnavailable = stringResource(R.string.error_voice_unavailable)
    val sMicPermission = stringResource(R.string.error_mic_permission)
    val sModelError = stringResource(R.string.error_model)

    val failureCounter = remember { VoiceFailureCounter() }
    var lastCommand by remember { mutableStateOf(VoiceCommand.UNKNOWN to 0L) }
    var hasPaused by remember { mutableStateOf(false) }

    // --- 5 lệnh (PLAN 4.4): dùng chung cho giọng nói và nút chạm ---
    fun pauseOrResume() {
        if (feedback.hasPausedSpeech()) {
            if (!feedback.resumeSpeech()) feedback.say(sNothingPaused, Priority.STATE, userInitiated = true)
        } else if (feedback.pauseSpeech()) {
            feedback.vibrate(HapticPattern.SHORT, force = true)
        } else {
            feedback.say(sNothingPaused, Priority.STATE, userInitiated = true)
        }
        hasPaused = feedback.hasPausedSpeech()
    }

    fun repeatLast() {
        if (!feedback.repeatLast()) feedback.say(sNothingToRepeat, Priority.STATE, userInitiated = true)
        hasPaused = false
    }

    // --- Khám phá xung quanh: chụp kết quả YOLO một lần, đọc tối đa 3 vật kèm hướng ---
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var exploreToken by remember { mutableIntStateOf(0) }
    fun explore() {
        if (currentPage != PAGE_FIND) currentPage = PAGE_FIND // camera chuyển sang YOLO
        val token = ++exploreToken
        feedback.say(sExploreLooking, Priority.STATE, userInitiated = true)
        yoloAnalyzer.requestSnapshot { seen ->
            val text = ExploreSummarizer.summarize(seen)
            mainHandler.post {
                if (token != exploreToken) return@post
                exploreToken++
                feedback.say(text ?: sExploreNothing, Priority.STATE, userInitiated = true, queue = true)
            }
        }
        // Không có frame nào (camera chưa sẵn sàng) trong 4 s → báo không thấy.
        mainHandler.postDelayed({
            if (token == exploreToken) {
                exploreToken++
                feedback.say(sExploreNothing, Priority.STATE, userInitiated = true)
            }
        }, 4000L)
    }

    /** @return true nếu lệnh đã xử lý ở đây; false để màn hình con xử lý tiếp (vd "tìm cốc", "đọc"). */
    fun handleCommand(command: VoiceCommand, text: String = ""): Boolean = when (command) {
        VoiceCommand.READ_TEXT ->
            if (currentPage == PAGE_OCR) false else { currentPage = PAGE_OCR; true }
        VoiceCommand.FIND ->
            // Đang ở màn Tìm: để FindScreen tự đọc tên vật. Ở màn khác: chuyển trang + mang theo vật.
            if (currentPage == PAGE_FIND) false else {
                pendingFindTarget = FindTarget.fromCommand(text)
                currentPage = PAGE_FIND
                true
            }
        VoiceCommand.SETTINGS -> { currentPage = PAGE_SETTINGS; true }
        VoiceCommand.BACK -> {
            if (currentPage == PAGE_FIND) {
                feedback.say(sAlreadyHome, Priority.STATE, userInitiated = true)
            } else {
                feedback.stopSpeech()
                currentPage = PAGE_FIND
            }
            true
        }
        VoiceCommand.PAUSE -> {
            if (feedback.pauseSpeech()) {
                feedback.vibrate(HapticPattern.SHORT, force = true)
            } else {
                feedback.say(sPaused, Priority.STATE, userInitiated = true)
            }
            hasPaused = feedback.hasPausedSpeech()
            true
        }
        VoiceCommand.RESUME -> {
            if (!feedback.resumeSpeech()) feedback.say(sNothingPaused, Priority.STATE, userInitiated = true)
            hasPaused = false
            true
        }
        VoiceCommand.REPEAT -> { repeatLast(); true }
        VoiceCommand.HELP -> {
            val currentScreenName = context.getString(screens[currentPage].first)
            feedback.say(String.format(helpFormat, currentScreenName), Priority.STATE, userInitiated = true)
            feedback.vibrate(force = true)
            true
        }
        VoiceCommand.NEXT_SENTENCE -> {
            if (!feedback.nextSentence()) feedback.say(sNoSentence, Priority.STATE, userInitiated = true)
            true
        }
        VoiceCommand.PREVIOUS_SENTENCE -> {
            if (!feedback.previousSentence()) feedback.say(sNoSentence, Priority.STATE, userInitiated = true)
            true
        }
        VoiceCommand.EXPLORE -> { explore(); true }
        VoiceCommand.UNKNOWN -> false
    }

    // --- Lỗi: mạng / micro / nhận giọng nói / mô hình → nói ra (ALERT) ---
    var lastNetworkAlertAt by remember { mutableStateOf(0L) }
    DisposableEffect(Unit) {
        voiceCommandManager.onVoiceError = { error ->
            when (error) {
                VoiceError.NETWORK -> {
                    val now = System.currentTimeMillis()
                    if (now - lastNetworkAlertAt > FeedbackConfig.VOICE_NETWORK_ALERT_COOLDOWN_MS) {
                        lastNetworkAlertAt = now
                        feedback.say(sNetwork, Priority.ALERT, key = "error:network")
                        feedback.vibrate(HapticPattern.DOUBLE, force = true)
                    }
                }
                VoiceError.PERMISSION -> feedback.say(sMicPermission, Priority.ALERT, key = "error:mic")
                VoiceError.UNAVAILABLE -> feedback.say(sVoiceUnavailable, Priority.ALERT, key = "error:asr")
            }
        }
        yoloAnalyzer.onModelError = {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                feedback.say(sModelError, Priority.ALERT, key = "error:model")
                feedback.vibrate(HapticPattern.DOUBLE, force = true)
            }
        }
        onDispose {
            voiceCommandManager.onVoiceError = null
            yoloAnalyzer.onModelError = null
        }
    }

    // Single Haptic Trigger
    LaunchedEffect(currentPage) {
        if (settingsManager.hapticsEnabled) {
            hapticManager.triggerHaptic()
        }
        
        // If moving away from camera pages, reset camera ready
        if (currentPage == PAGE_SETTINGS) {
            isCameraReady = false
        }
    }

    // Centralized Voice Navigation Listener
    DisposableEffect(settingsManager.useVietnamese, currentPage) {
        voiceCommandManager.onGlobalIntent = { result, isFinal ->
            val command = VoiceCommandParser.parse(result)
            val now = System.currentTimeMillis()
            // "dừng" / "đọc lại" phải nghe được cả khi đang đọc; lệnh khác thì chờ TTS xong
            // để mic không bắt nhầm chính lời app vừa nói.
            val allowedWhileSpeaking = command == VoiceCommand.PAUSE || command == VoiceCommand.REPEAT
            val (prevCommand, prevAt) = lastCommand
            when {
                command == VoiceCommand.UNKNOWN -> {
                    // Chỉ đếm lỗi ở kết quả cuối và ở màn Đọc chữ / Cài đặt; màn Tìm tự hiểu tên vật.
                    if (isFinal && currentPage != PAGE_FIND && !feedback.isBusy()) {
                        when (failureCounter.onUnrecognized()) {
                            VoiceFailureCounter.Reaction.ASK_AGAIN ->
                                feedback.say(sNotUnderstood, Priority.STATE, userInitiated = true)
                            VoiceFailureCounter.Reaction.SUGGEST_TOUCH ->
                                feedback.say(sUseTouch, Priority.STATE, userInitiated = true)
                        }
                    }
                    false
                }
                feedback.isBusy() && !allowedWhileSpeaking -> false
                // Kết quả tạm và kết quả cuối của cùng một câu: chỉ xử lý một lần.
                command == prevCommand && now - prevAt < FeedbackConfig.VOICE_COMMAND_DEDUP_MS -> true
                else -> {
                    val handled = handleCommand(command, result)
                    if (handled) {
                        failureCounter.onRecognized()
                        lastCommand = command to now
                    }
                    handled
                }
            }
        }
        onDispose {
            voiceCommandManager.onGlobalIntent = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            yoloAnalyzer.close()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                tonalElevation = 0.dp
            ) {
                // Nút chạm dự phòng cho 5 lệnh giọng nói (PLAN 4.4): Tìm · Đọc chữ · Dừng/Tiếp · Đọc lại · Cài đặt.
                // "Quay lại" = nút Tìm hoặc nút Back của máy.
                val itemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray
                )
                val barItems = listOf(
                    BarItem(R.string.nav_find, Icons.Default.Search, currentPage == PAGE_FIND) {
                        currentPage = PAGE_FIND
                    },
                    BarItem(R.string.nav_ocr, Icons.Default.TextFields, currentPage == PAGE_OCR) {
                        currentPage = PAGE_OCR
                    },
                    if (hasPaused) {
                        BarItem(R.string.nav_resume, Icons.Default.PlayArrow, false) { pauseOrResume() }
                    } else {
                        BarItem(R.string.nav_pause, Icons.Default.Pause, false) { pauseOrResume() }
                    },
                    BarItem(R.string.nav_repeat, Icons.Default.Replay, false) { repeatLast() },
                    BarItem(R.string.nav_settings, Icons.Default.Settings, currentPage == PAGE_SETTINGS) {
                        currentPage = PAGE_SETTINGS
                    }
                )
                barItems.forEach { item ->
                    val label = stringResource(item.label)
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(32.dp)) },
                        label = { Text(label, fontSize = 13.sp, maxLines = 1) },
                        selected = item.selected,
                        colors = itemColors,
                        modifier = Modifier.semantics { contentDescription = label },
                        onClick = {
                            feedback.vibrate(HapticPattern.SHORT, force = true)
                            failureCounter.onRecognized()
                            item.onClick()
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            if (java.lang.Math.abs(totalDrag) > swipeThreshold && currentPage == PAGE_OCR && feedback.reader?.isReading == true) {
                                // Đang đọc văn bản: vuốt trái = câu tiếp, vuốt phải = câu trước (thay cho chuyển trang).
                                if (totalDrag < 0) feedback.nextSentence() else feedback.previousSentence()
                                feedback.vibrate(HapticPattern.SHORT, force = true)
                                totalDrag = 0f
                            } else if (java.lang.Math.abs(totalDrag) > swipeThreshold) {
                                val targetPage = if (totalDrag < 0) {
                                    (currentPage + 1).coerceAtMost(screens.lastIndex)
                                } else {
                                    (currentPage - 1).coerceAtLeast(0)
                                }
                                if (targetPage != currentPage) {
                                    currentPage = targetPage
                                }
                                totalDrag = 0f
                            }
                        }
                    )
                }
        ) {
            // Shared CameraPreview layer
            if (currentPage != PAGE_SETTINGS) {
                // Không thu nhỏ ở màn Tìm: vật to hơn trong ảnh thì YOLO nhận diện tốt hơn.
                val zoom = 1.0f
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    zoomRatio = zoom,
                    imageAnalyzer = activeAnalyzer,
                    onReady = { isCameraReady = true },
                    torchEnabled = isTorchEnabled
                )
            }

            // Crossfade between Spinner and Camera
            if (currentPage != PAGE_SETTINGS) {
                AnimatedVisibility(
                    visible = !isCameraReady,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(durationMillis = 500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF222222)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // Page Content with fade transitions
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                }
            ) { targetPage ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val isActive = currentPage == targetPage
                    when (targetPage) {
                        PAGE_FIND -> FindScreen(
                            feedback = feedback,
                            voiceCommandManager = voiceCommandManager,
                            settingsManager = settingsManager,
                            isActive = isActive,
                            yoloAnalyzer = yoloAnalyzer,
                            initialTarget = pendingFindTarget,
                            onInitialTargetHandled = { pendingFindTarget = null },
                            onExplore = { explore() }
                        )
                        PAGE_OCR -> OCRScreen(
                            onBack = { currentPage = PAGE_FIND },
                            feedback = feedback,
                            settingsManager = settingsManager,
                            voiceCommandManager = voiceCommandManager,
                            isActive = isActive,
                            ocrManager = ocrManager,
                            onProvideAnalyzer = { ocrAnalyzer = it }
                        )
                        PAGE_SETTINGS -> Box(modifier = Modifier.fillMaxSize().background(Color(0xFF222222)).padding(innerPadding)) {
                            SettingsScreen(
                                ttsManager = ttsManager, 
                                hapticManager = hapticManager, 
                                voiceCommandManager = voiceCommandManager, 
                                settingsManager = settingsManager, 
                                onReset = onReset, 
                                isActive = isActive,
                                resetCount = resetCount,
                                onResetClick = { resetCount++ },
                                feedback = feedback
                            )
                        }
                    }
                }
            }
        }
    }
}
