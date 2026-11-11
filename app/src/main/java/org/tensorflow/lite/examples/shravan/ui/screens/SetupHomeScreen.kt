package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.ui.components.AccessibleButton
import org.tensorflow.lite.examples.shravan.utils.*

/**
 * Lần đầu mở app: chỉ hỏi mức thị lực (app chỉ có tiếng Việt nên bỏ bước chọn ngôn ngữ).
 * Trả lời bằng giọng nói ("một phần" / "hoàn toàn") hoặc chạm một trong hai nút lớn.
 */
@Composable
fun SetupHomeScreen(
    navController: NavController,
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager
) {
    var interactionsEnabled by remember { mutableStateOf(true) }
    var isScreenDimmed by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf<ImpairmentLevel?>(null) }

    val scope = rememberCoroutineScope()
    val voiceSessionId = remember { mutableStateOf<Int?>(null) }

    val greeting = stringResource(R.string.welcome_text)
    val welcomePartial = stringResource(R.string.welcome_partial)
    val welcomeTotal = stringResource(R.string.welcome_total)
    val labelPartial = stringResource(R.string.label_partial)
    val labelTotal = stringResource(R.string.label_total)
    val keywordPartial = VoiceCommandParser.normalize(stringResource(R.string.voice_keyword_partial))
    val keywordTotal = VoiceCommandParser.normalize(stringResource(R.string.voice_keyword_total))

    fun choose(level: ImpairmentLevel) {
        if (!interactionsEnabled) return
        interactionsEnabled = false
        selectedLevel = level
        val text = if (level == ImpairmentLevel.PartiallyImpaired) welcomePartial else welcomeTotal
        scope.launch { handleSelection(level, settingsManager, navController, ttsManager, hapticManager, text) }
    }

    fun onSpeech(text: String) {
        val said = VoiceCommandParser.normalize(text)
        when {
            said.contains(keywordPartial) -> choose(ImpairmentLevel.PartiallyImpaired)
            said.contains(keywordTotal) -> choose(ImpairmentLevel.TotallyImpaired)
        }
    }

    LaunchedEffect(Unit) {
        ttsManager.speak(greeting, isVietnamese = true, onComplete = { scope.launch { isScreenDimmed = false } })
        // Chờ một chút để mic không nghe lại lời chào.
        delay(1000)
        voiceSessionId.value = voiceCommandManager.startListening(
            isVietnamese = true,
            partialCallback = { onSpeech(it) }
        ) { onSpeech(it) }
    }

    DisposableEffect(Unit) {
        onDispose { voiceSessionId.value?.let { voiceCommandManager.stopListening(it) } }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF333333))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            listOf(
                ImpairmentLevel.PartiallyImpaired to labelPartial,
                ImpairmentLevel.TotallyImpaired to labelTotal
            ).forEachIndexed { index, (level, label) ->
                if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                val color = when (selectedLevel) {
                    level -> Color.Yellow
                    null -> Color.White
                    else -> Color.Gray
                }
                AccessibleButton(
                    label = label,
                    speakLabel = label,
                    enabled = interactionsEnabled,
                    onClick = { choose(level) },
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    hapticManager = hapticManager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    backgroundColor = Color(0xFF222222),
                    textColor = color,
                    fontSizeScale = 3f
                )
            }
        }
        if (isScreenDimmed) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }
    }
}

private suspend fun handleSelection(
    level: ImpairmentLevel,
    settingsManager: SettingsManager,
    navController: NavController,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    confirmationText: String
) {
    settingsManager.updateImpairmentLevel(level)
    delay(500)
    hapticManager.triggerHaptic()
    ttsManager.speak(confirmationText, isVietnamese = true) {
        navController.navigate("tutorial") {
            popUpTo("setup") { inclusive = true }
        }
    }
}
