package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager,
    settingsManager: SettingsManager,
    onReset: () -> Unit,
    isActive: Boolean = true,
    resetCount: Int = 0,
    onResetClick: () -> Unit = {}
) {
    var languageExpanded by remember { mutableStateOf(false) }
    var flashExpanded by remember { mutableStateOf(false) }
    var languageTextFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var flashFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val langEn = stringResource(R.string.lang_en)
    val langVi = stringResource(R.string.lang_vi)
    val languages = listOf(langEn to false, langVi to true)
    val flashes = listOf(
        stringResource(R.string.flash_auto) to "auto",
        stringResource(R.string.flash_on) to "on",
        stringResource(R.string.flash_off) to "off"
    )

    DisposableEffect(isActive) {
        var sessionId: Int? = null
        if (isActive) {
            sessionId = voiceCommandManager.startListening(isVietnamese = settingsManager.useVietnamese) {}
        }
        onDispose {
            sessionId?.let { voiceCommandManager.stopListening(it) }
        }
    }

    val redAlpha = (resetCount * 0.14f).coerceIn(0f, 1f)
    val bgColor = if (resetCount > 0) Color.Red.copy(alpha = redAlpha) else Color(0xFF222222)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings), 
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.5.sp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Language
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (settingsManager.useVietnamese) langVi else langEn,
                onValueChange = {},
                readOnly = true,
                enabled = false, // Use Box for click
                label = { Text(stringResource(R.string.settings_language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { languageTextFieldSize = it.size.toSize() }
                    .clickable { 
                        hapticManager.triggerHaptic()
                        languageExpanded = true 
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.White,
                    disabledBorderColor = Color.White,
                    disabledLabelColor = Color.White,
                    disabledTrailingIconColor = Color.White
                )
            )
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { languageTextFieldSize.width.toDp() })
                    .background(Color(0xFF333333))
            ) {
                languages.forEach { (label, isVi) ->
                    DropdownMenuItem(
                        text = { Text(label, color = if (settingsManager.useVietnamese == isVi) Color.Yellow else Color.White, fontSize = 18.sp) },
                        onClick = {
                            hapticManager.triggerHaptic()
                            settingsManager.useVietnamese = isVi
                            ttsManager.setLanguage(isVi)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        // Haptics
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.settings_haptics), color = Color.White, fontSize = 18.sp)
            Switch(
                checked = settingsManager.hapticsEnabled,
                onCheckedChange = { 
                    settingsManager.hapticsEnabled = it
                    hapticManager.triggerHaptic()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Gray,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }

        // Speech Rate
        Column {
            Text(stringResource(R.string.settings_speech_rate) + ": ${"%.1f".format(settingsManager.speechRate)}x", color = Color.White, fontSize = 18.sp)
            Slider(
                value = settingsManager.speechRate,
                onValueChange = { 
                    settingsManager.speechRate = it
                    ttsManager.setSpeechRate(it)
                    // Haptic omitted here to avoid continuous buzzing on slide, or we can add it?
                    // "trigger haptic feedback for every user-initiated navigation, swipe, or confirmed intent"
                    // Let's add a light one or omit. I will omit for continuous sliding.
                },
                valueRange = 0.5f..2.0f,
                steps = 14
            )
        }

        // Flash
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = flashes.find { it.second == settingsManager.flashMode }?.first ?: stringResource(R.string.flash_auto),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.settings_flash)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flashExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { flashFieldSize = it.size.toSize() }
                    .clickable { 
                        hapticManager.triggerHaptic()
                        flashExpanded = true 
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.White,
                    disabledBorderColor = Color.White,
                    disabledLabelColor = Color.White,
                    disabledTrailingIconColor = Color.White
                )
            )
            DropdownMenu(
                expanded = flashExpanded,
                onDismissRequest = { flashExpanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { flashFieldSize.width.toDp() })
                    .background(Color(0xFF333333))
            ) {
                flashes.forEach { (label, mode) ->
                    DropdownMenuItem(
                        text = { Text(label, color = if (settingsManager.flashMode == mode) Color.Yellow else Color.White, fontSize = 18.sp) },
                        onClick = {
                            hapticManager.triggerHaptic()
                            settingsManager.flashMode = mode
                            flashExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Reset
        Button(
            onClick = {
                onResetClick()
                if (resetCount + 1 >= 7) {
                    hapticManager.triggerHaptic()
                    onReset()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (resetCount > 0) Color.Transparent else MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.settings_reset), fontSize = 18.sp, color = Color.White)
        }
    }
}
