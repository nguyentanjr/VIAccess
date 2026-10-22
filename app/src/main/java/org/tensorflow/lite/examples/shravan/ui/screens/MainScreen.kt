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
import androidx.compose.material.icons.filled.Search
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

    val kFind = stringResource(R.string.voice_keyword_find).lowercase()
    val kOcr = stringResource(R.string.voice_keyword_ocr).lowercase()
    val kSettings = stringResource(R.string.voice_keyword_settings).lowercase()

    val navKeywords = remember(kFind, kOcr, kSettings) {
        listOf(
            kFind to PAGE_FIND,
            kOcr to PAGE_OCR,
            kSettings to PAGE_SETTINGS
        )
    }

    val kHelp = stringResource(R.string.voice_keyword_help).lowercase()
    val kWhereAmI = stringResource(R.string.voice_keyword_where_am_i).lowercase()

    val locationFormat = stringResource(R.string.status_location)

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
    DisposableEffect(settingsManager.useVietnamese, navKeywords, screens, currentPage) {
        voiceCommandManager.onGlobalIntent = { result ->
            if (ttsManager.isSpeaking()) {
                false
            } else {
                val trimmedResult = result.trim().lowercase()
                var handled = false
                
                // Help / orientation check
                if (trimmedResult.contains(kHelp) || trimmedResult.contains(kWhereAmI) || trimmedResult.contains("help") || trimmedResult.contains("where am i")) {
                    val currentScreenName = context.getString(screens[currentPage].first)
                    feedback.say(String.format(locationFormat, currentScreenName), Priority.STATE, userInitiated = true)
                    feedback.vibrate(force = true)
                    handled = true
                }

                if (!handled) {
                    for (entry in navKeywords) {
                        if (trimmedResult.contains(entry.first)) {
                            if (currentPage != entry.second) {
                                currentPage = entry.second
                            }
                            handled = true
                            break
                        }
                    }
                }
                handled
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
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.second, contentDescription = stringResource(screen.first), modifier = Modifier.size(32.dp)) },
                        label = null,
                        selected = currentPage == index,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        onClick = {
                            currentPage = index
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
                            if (java.lang.Math.abs(totalDrag) > swipeThreshold) {
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
                val zoom = if (currentPage == PAGE_FIND) 0.6f else 1.0f
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
                            yoloAnalyzer = yoloAnalyzer
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
                                onResetClick = { resetCount++ }
                            )
                        }
                    }
                }
            }
        }
    }
}
