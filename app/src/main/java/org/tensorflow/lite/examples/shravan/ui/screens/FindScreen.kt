package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import org.tensorflow.lite.examples.shravan.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.tflite.Classifier
import org.tensorflow.lite.examples.shravan.tflite.YoloAnalyzer
import org.tensorflow.lite.examples.shravan.ui.theme.DimmedPalette
import org.tensorflow.lite.examples.shravan.ui.theme.InterFontFamily
import org.tensorflow.lite.examples.shravan.utils.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun FindScreen(
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
    
    var activeMode by remember { mutableStateOf<String?>(null) }
    val isMonotone = activeMode == null

    val scope = rememberCoroutineScope()
    var voiceSessionId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isActive, yoloAnalyzer, activeMode) {
        if (isActive) {
            yoloAnalyzer?.allowedClasses = when (activeMode) {
                "seatings & tables" -> listOf("chair", "dining table", "sofa", "couch", "bàn ăn", "ghế", "ghế sofa")
                "doors & windows" -> listOf("door", "window", "cửa")
                "person & vehicles" -> listOf("person", "car", "bus", "truck", "motorcycle", "bicycle", "người", "xe hơi", "xe buýt", "xe tải", "xe máy", "xe đạp")
                else -> emptyList()
            }
            yoloAnalyzer?.onResults = { results ->
                recognitions = results
            }
        }
    }

    val findGreeting = stringResource(R.string.find_greeting)
    val findPrompt = stringResource(R.string.find_prompt)
    val modeSeatingsTables = stringResource(R.string.mode_seatings_tables)
    val modeDoorsWindows = stringResource(R.string.mode_doors_windows)
    val modePersonVehicles = stringResource(R.string.mode_person_vehicles)
    
    LaunchedEffect(isActive, settingsManager.useVietnamese) {
        if (isActive) {
            ttsManager.speak(findGreeting, isVietnamese = settingsManager.useVietnamese)
        }
    }

    LaunchedEffect(activeMode, isActive) {
        if (isActive) {
            if (activeMode == null) {
                ttsManager.speak(findPrompt, isQueued = true, isVietnamese = settingsManager.useVietnamese)
                voiceSessionId = voiceCommandManager.startListening(isVietnamese = settingsManager.useVietnamese) { result ->
                    val lowerResult = result.lowercase()
                    if (lowerResult.contains(modeSeatingsTables.lowercase()) || lowerResult.contains("seat") || lowerResult.contains("table") || lowerResult.contains("bàn") || lowerResult.contains("ghế")) {
                        activeMode = "seatings & tables"
                        hapticManager.triggerHaptic()
                    } else if (lowerResult.contains(modeDoorsWindows.lowercase()) || lowerResult.contains("door") || lowerResult.contains("window") || lowerResult.contains("cửa")) {
                        activeMode = "doors & windows"
                        hapticManager.triggerHaptic()
                    } else if (lowerResult.contains(modePersonVehicles.lowercase()) || lowerResult.contains("person") || lowerResult.contains("vehicle") || lowerResult.contains("người") || lowerResult.contains("xe")) {
                        activeMode = "person & vehicles"
                        hapticManager.triggerHaptic()
                    }
                }
            } else {
                voiceSessionId?.let {
                    voiceCommandManager.stopListening(it)
                    voiceSessionId = null
                }
                val modeLabel = when (activeMode) {
                    "seatings & tables" -> modeSeatingsTables
                    "doors & windows" -> modeDoorsWindows
                    "person & vehicles" -> modePersonVehicles
                    else -> ""
                }
                val lookingForText = context.getString(R.string.find_looking_for, modeLabel)
                ttsManager.speak(lookingForText, isVietnamese = settingsManager.useVietnamese)
            }
        } else {
            voiceSessionId?.let {
                voiceCommandManager.stopListening(it)
                voiceSessionId = null
            }
        }
    }

    DisposableEffect(isActive) {
        onDispose {
            voiceSessionId?.let { voiceCommandManager.stopListening(it) }
        }
    }

    val seatingBgColor by animateColorAsState(
        targetValue = if (activeMode == "seatings & tables") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300)
    )
    val seatingContentColor by animateColorAsState(
        targetValue = if (activeMode == "seatings & tables") Color.Black else Color.LightGray,
        animationSpec = tween(durationMillis = 300)
    )

    val doorsBgColor by animateColorAsState(
        targetValue = if (activeMode == "doors & windows") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300)
    )
    val doorsContentColor by animateColorAsState(
        targetValue = if (activeMode == "doors & windows") Color.Black else Color.LightGray,
        animationSpec = tween(durationMillis = 300)
    )

    val personBgColor by animateColorAsState(
        targetValue = if (activeMode == "person & vehicles") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300)
    )
    val personContentColor by animateColorAsState(
        targetValue = if (activeMode == "person & vehicles") Color.Black else Color.LightGray,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(recognitions, activeMode, isActive) {
        if (isActive && activeMode != null && recognitions.isNotEmpty()) {
            while (true) {
                val currentRecognitions = recognitions
                if (currentRecognitions.isEmpty() || !isActive || activeMode == null) break
                
                // Distance estimation based on bounding box area
                // Larger area = closer object
                val closest = currentRecognitions.maxByOrNull { it.location.width() * it.location.height() }
                if (closest != null) {
                    val area = closest.location.width() * closest.location.height()
                    val maxArea = 416f * 416f
                    val normalizedArea = (area / maxArea).coerceIn(0f, 1f)
                    
                    // Interval scales from 1000ms (far) to 100ms (very close)
                    val interval = (1000 - (normalizedArea * 900)).toLong().coerceAtLeast(100L)
                    
                    hapticManager.triggerHaptic()
                    delay(interval)
                } else {
                    delay(500)
                }
            }
        }
    }

    // Voice control for buttons
    LaunchedEffect(isActive, activeMode) {
        if (isActive) {
            voiceSessionId = voiceCommandManager.startListening(isVietnamese = settingsManager.useVietnamese) { result ->
                val lowerResult = result.lowercase()
                val m1 = modeSeatingsTables.lowercase()
                val m2 = modeDoorsWindows.lowercase()
                val m3 = modePersonVehicles.lowercase()
                
                if (lowerResult.contains(m1) || lowerResult.contains("seat") || lowerResult.contains("table") || lowerResult.contains("bàn") || lowerResult.contains("ghế")) {
                    activeMode = "seatings & tables"
                } else if (lowerResult.contains(m2) || lowerResult.contains("door") || lowerResult.contains("window") || lowerResult.contains("cửa")) {
                    activeMode = "doors & windows"
                } else if (lowerResult.contains(m3) || lowerResult.contains("person") || lowerResult.contains("vehicle") || lowerResult.contains("người") || lowerResult.contains("xe")) {
                    activeMode = "person & vehicles"
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        if (isActive) {
            if (isMonotone) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.8f)))
            }

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
        
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1
            Box(
                modifier = Modifier.size(width = 80.dp, height = 50.dp).clip(RoundedCornerShape(12.dp)).background(seatingBgColor).clickable { activeMode = "seatings & tables"; hapticManager.triggerHaptic() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventSeat, contentDescription = null, tint = seatingContentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.TableRestaurant, contentDescription = null, tint = seatingContentColor, modifier = Modifier.size(20.dp))
                }
            }
            // Button 2
            Box(
                modifier = Modifier.size(width = 80.dp, height = 50.dp).clip(RoundedCornerShape(12.dp)).background(doorsBgColor).clickable { activeMode = "doors & windows"; hapticManager.triggerHaptic() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Window, contentDescription = null, tint = doorsContentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = doorsContentColor, modifier = Modifier.size(20.dp))
                }
            }
            // Button 3
            Box(
                modifier = Modifier.size(width = 80.dp, height = 50.dp).clip(RoundedCornerShape(12.dp)).background(personBgColor).clickable { activeMode = "person & vehicles"; hapticManager.triggerHaptic() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = personContentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = personContentColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
