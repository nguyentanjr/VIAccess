package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.*

/** Màu tương phản cao (≥ 7:1 trên nền tối). */
private val Bg = Color(0xFF121212)
private val Fg = Color.White
private val Accent = Color(0xFFFFD600)
private val TextSize = 24.sp
private val MinTouch = 56.dp

/**
 * Cài đặt (PLAN 9.4): tốc độ đọc, rung, flash, mức chi tiết phản hồi, chế độ so sánh A/B.
 * Chữ ≥ 24sp, vùng chạm ≥ 48dp, mỗi thay đổi được đọc lại để người dùng biết đã chọn gì.
 */
@Composable
fun SettingsScreen(
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager,
    settingsManager: SettingsManager,
    onReset: () -> Unit,
    isActive: Boolean = true,
    resetCount: Int = 0,
    onResetClick: () -> Unit = {},
    feedback: FeedbackCoordinator? = null
) {
    val sOn = stringResource(R.string.flash_on)
    val sOff = stringResource(R.string.flash_off)
    val flashes = listOf(
        stringResource(R.string.flash_auto) to "auto",
        sOn to "on",
        sOff to "off"
    )

    fun announce(text: String) {
        hapticManager.triggerHaptic()
        if (feedback != null) feedback.say(text, Priority.STATE, userInitiated = true)
        else ttsManager.speak(text, isVietnamese = true)
    }

    // Giữ một phiên nghe để lệnh chung (quay lại, đọc chữ...) vẫn hoạt động ở màn này.
    DisposableEffect(isActive) {
        var sessionId: Int? = null
        if (isActive) sessionId = voiceCommandManager.startListening(isVietnamese = true) {}
        onDispose { sessionId?.let { voiceCommandManager.stopListening(it) } }
    }

    val redAlpha = (resetCount * 0.14f).coerceIn(0f, 1f)
    val bgColor = if (resetCount > 0) Color.Red.copy(alpha = redAlpha) else Bg

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            color = Fg,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )

        // Tốc độ đọc: hai nút lớn thay cho thanh trượt (dễ dùng với TalkBack).
        val rateLabel = stringResource(R.string.settings_speech_rate)
        val rateText = "%.1f".format(settingsManager.speechRate) + "x"
        Text("$rateLabel: $rateText", color = Fg, fontSize = TextSize)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val slower = stringResource(R.string.speech_slower)
            val faster = stringResource(R.string.speech_faster)
            BigButton(slower, Modifier.weight(1f)) {
                settingsManager.speechRate = (settingsManager.speechRate - 0.1f).coerceAtLeast(0.5f)
                ttsManager.setSpeechRate(settingsManager.speechRate)
                announce("$rateLabel " + "%.1f".format(settingsManager.speechRate))
            }
            BigButton(faster, Modifier.weight(1f)) {
                settingsManager.speechRate = (settingsManager.speechRate + 0.1f).coerceAtMost(2.0f)
                ttsManager.setSpeechRate(settingsManager.speechRate)
                announce("$rateLabel " + "%.1f".format(settingsManager.speechRate))
            }
        }

        val hapticsLabel = stringResource(R.string.settings_haptics)
        ToggleRow(hapticsLabel, settingsManager.hapticsEnabled, sOn, sOff) {
            settingsManager.hapticsEnabled = it
            announce("$hapticsLabel ${if (it) sOn else sOff}")
        }

        val flashLabel = stringResource(R.string.settings_flash)
        ChoiceGroup(flashLabel, flashes, settingsManager.flashMode) { label, mode ->
            settingsManager.flashMode = mode
            announce("$flashLabel $label")
        }

        val detailLabel = stringResource(R.string.settings_detail)
        val detailOptions = listOf(
            stringResource(R.string.detail_full) to false,
            stringResource(R.string.detail_brief) to true
        )
        ChoiceGroup(detailLabel, detailOptions, settingsManager.briefFeedback) { label, brief ->
            settingsManager.briefFeedback = brief
            announce("$detailLabel $label")
        }

        val baselineLabel = stringResource(R.string.settings_baseline)
        ToggleRow(baselineLabel, settingsManager.baselineMode, sOn, sOff) {
            settingsManager.baselineMode = it
            announce("$baselineLabel ${if (it) sOn else sOff}")
        }
        Text(stringResource(R.string.settings_baseline_hint), color = Color(0xFFDDDDDD), fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))

        // Khôi phục cài đặt gốc: chạm 7 lần để tránh bấm nhầm.
        Button(
            onClick = {
                onResetClick()
                if (resetCount + 1 >= 7) {
                    hapticManager.triggerHaptic()
                    onReset()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (resetCount > 0) Color.Transparent else MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.settings_reset), fontSize = TextSize, color = Fg)
        }
    }
}

@Composable
private fun BigButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouch),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Fg)
    ) {
        Text(label, fontSize = TextSize)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onText: String, offText: String, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouch)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics { stateDescription = if (checked) onText else offText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Fg, fontSize = TextSize, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Fg,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun <T> ChoiceGroup(title: String, options: List<Pair<String, T>>, current: T, onSelect: (String, T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Fg, fontSize = TextSize)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, value) ->
                val isSelected = value == current
                Button(
                    onClick = { onSelect(label, value) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouch)
                        .semantics {
                            selected = isSelected
                            contentDescription = "$title: $label"
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Accent else Color(0xFF2A2A2A),
                        contentColor = if (isSelected) Color.Black else Fg
                    )
                ) {
                    Text(label, fontSize = 20.sp, maxLines = 1)
                }
            }
        }
    }
}
