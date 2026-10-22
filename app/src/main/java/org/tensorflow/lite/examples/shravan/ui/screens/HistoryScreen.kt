package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    historyManager: HistoryManager,
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager,
    isActive: Boolean = true
) {
    val useVietnamese = settingsManager.useVietnamese
    var historyItems by remember { mutableStateOf(historyManager.getHistory()) }
    var speakingItemId by remember { mutableStateOf<Long?>(null) }
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    var interactionsEnabled by remember { mutableStateOf(false) }
    val voiceSessionId = remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(isActive) {
        onDispose {
            voiceSessionId.value?.let {
                voiceCommandManager.stopListening(it)
            }
        }
    }

    val historyGreeting = stringResource(R.string.history_greeting)
    val historyGreetingTts = stringResource(R.string.history_greeting_tts)
    val backCommand = stringResource(R.string.back_command)
    val historyEmpty = stringResource(R.string.history_empty)
    val historyCleared = stringResource(R.string.history_cleared)
    val historyClearDesc = stringResource(R.string.history_clear_description)
    val speakDesc = stringResource(R.string.speak_description)

    LaunchedEffect(isActive) {
        if (isActive) {
            ttsManager.speak(
                historyGreetingTts,
                isVietnamese = useVietnamese,
                onComplete = {
                    scope.launch {
                        delay(1000)
                        interactionsEnabled = true
                    }
                }
            )
            
            voiceSessionId.value = voiceCommandManager.startListening(isVietnamese = useVietnamese) { result ->
                val lowerResult = result.lowercase()
                if (lowerResult.contains("quay lại") || lowerResult.contains("back")) {
                    ttsManager.speak(backCommand, isVietnamese = useVietnamese)
                    hapticManager.triggerHaptic()
                    onBack()
                } else if (lowerResult.contains("xóa") || lowerResult.contains("clear") || lowerResult.contains("delete")) {
                    hapticManager.triggerHaptic()
                    historyManager.clearHistory()
                    historyItems = emptyList()
                    ttsManager.speak(historyCleared, isVietnamese = useVietnamese)
                }
            }
        } else {
            interactionsEnabled = false
            ttsManager.stop()
            voiceSessionId.value?.let {
                voiceCommandManager.stopListening(it)
                voiceSessionId.value = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = historyGreeting,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.5.sp),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = {
                    hapticManager.triggerHaptic()
                    historyManager.clearHistory()
                    historyItems = emptyList()
                    ttsManager.speak(historyCleared, isVietnamese = useVietnamese)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = historyClearDesc, tint = Color.White)
            }
        }

        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(historyEmpty, fontSize = 20.sp, color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF444444), RoundedCornerShape(12.dp))
                            .clickable {
                                hapticManager.triggerHaptic()
                                speakingItemId = item.timestamp
                                ttsManager.speak(item.content, isVietnamese = useVietnamese) {
                                    if (speakingItemId == item.timestamp) {
                                        speakingItemId = null
                                    }
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (item.type.lowercase()) {
                            "object", "explore" -> Icons.Default.CameraAlt
                            "find" -> Icons.Default.Search
                            "ocr" -> Icons.Default.TextFields
                            else -> Icons.Default.History
                        }
                        val itemColor = if (speakingItemId == item.timestamp) Color.Yellow else Color.White
                        Icon(icon, contentDescription = null, tint = itemColor, modifier = Modifier.size(32.dp))
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.content,
                                color = itemColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(Icons.Default.VolumeUp, contentDescription = speakDesc, tint = itemColor, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
