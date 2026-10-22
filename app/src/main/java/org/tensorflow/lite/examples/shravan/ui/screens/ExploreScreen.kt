package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.tflite.Classifier
import org.tensorflow.lite.examples.shravan.tflite.YoloAnalyzer
import org.tensorflow.lite.examples.shravan.ui.theme.DimmedPalette
import org.tensorflow.lite.examples.shravan.ui.theme.InterFontFamily
import org.tensorflow.lite.examples.shravan.utils.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun ExploreScreen(
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager,
    settingsManager: SettingsManager,
    historyManager: HistoryManager,
    isActive: Boolean = true,
    yoloAnalyzer: YoloAnalyzer? = null
) {
    val context = LocalContext.current
    var recognitions by remember { mutableStateOf(emptyList<Classifier.Recognition>()) }
    val textMeasurer = rememberTextMeasurer()

    val exploreGreeting = stringResource(R.string.explore_greeting)

    LaunchedEffect(isActive) {
        if (isActive) {
            ttsManager.speak(
                exploreGreeting,
                isVietnamese = settingsManager.useVietnamese
            )
        }
    }

    LaunchedEffect(isActive, yoloAnalyzer) {
        if (isActive) {
            yoloAnalyzer?.onResults = { results ->
                recognitions = results
            }
        }
    }

    DisposableEffect(isActive, settingsManager.useVietnamese) {
        var sessionId: Int? = null
        if (isActive) {
            sessionId = voiceCommandManager.startListening(isVietnamese = settingsManager.useVietnamese) {}
        }
        onDispose {
            sessionId?.let { voiceCommandManager.stopListening(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                recognitions.forEach { recognition ->
                    val rect = recognition.location
                    val scaleX = size.width / 416f 
                    val scaleY = size.height / 416f
                    val color = DimmedPalette[recognition.detectedClass % DimmedPalette.size]
                    
                    val topLeft = Offset(rect.left * scaleX, rect.top * scaleY)
                    val boxSize = Size(rect.width() * scaleX, rect.height() * scaleY)

                    drawRect(
                        color = color,
                        topLeft = topLeft,
                        size = boxSize,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = recognition.title,
                        topLeft = Offset(topLeft.x, topLeft.y - 20.dp.toPx()),
                        style = TextStyle(
                            color = color, 
                            fontSize = 16.sp,
                            fontFamily = InterFontFamily
                        )
                    )
                }
            }
        }
    }
}
