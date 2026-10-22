package org.tensorflow.lite.examples.shravan.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.examples.shravan.utils.HapticManager
import org.tensorflow.lite.examples.shravan.utils.SettingsManager
import org.tensorflow.lite.examples.shravan.utils.TTSManager

@Composable
fun AccessibleButton(
    label: String,
    speakLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    ttsManager: TTSManager,
    settingsManager: SettingsManager,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color = Color.White,
    fontSizeScale: Float = 1f
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onTap = {
                            ttsManager.speak(speakLabel, isVietnamese = settingsManager.useVietnamese)
                            hapticManager.triggerHaptic()
                            onClick()
                        },
                        onLongPress = {
                            ttsManager.speak(speakLabel, isVietnamese = settingsManager.useVietnamese)
                            hapticManager.triggerHaptic()
                            onLongClick?.invoke()
                        }
                    )
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor ?: MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val fontSize = (24 * fontSizeScale).sp
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.2f
                ),
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
