package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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

@Composable
fun SetupHomeScreen(
    navController: NavController,
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager
) {
    var setupStep by remember { mutableIntStateOf(0) } // 0: Language, 1: Impairment
    var interactionsEnabled by remember { mutableStateOf(true) } // Always enable clicking
    var isScreenDimmed by remember { mutableStateOf(true) }
    
    // State for styling selected item
    var selectedLanguage by remember { mutableStateOf<Boolean?>(null) }
    var selectedLevel by remember { mutableStateOf<ImpairmentLevel?>(null) }
    
    val scope = rememberCoroutineScope()
    val voiceSessionId = remember { mutableStateOf<Int?>(null) }

    // String resources
    val langPromptEn = "Please choose your language: English or Vietnamese?"
    val langPromptVi = "Vui lòng chọn ngôn ngữ: Tiếng Anh hay Tiếng Việt?"
    val langEn = stringResource(R.string.lang_choice_en)
    val langVi = stringResource(R.string.lang_choice_vi)
    val kwEn = stringResource(R.string.voice_keyword_en).lowercase()
    val kwVi = stringResource(R.string.voice_keyword_vi).lowercase()

    val greeting = stringResource(R.string.welcome_text)
    val welcomePartial = stringResource(R.string.welcome_partial)
    val welcomeTotal = stringResource(R.string.welcome_total)
    val labelPartial = stringResource(R.string.label_partial)
    val labelTotal = stringResource(R.string.label_total)
    val keywordPartial = stringResource(R.string.voice_keyword_partial).lowercase()
    val keywordTotal = stringResource(R.string.voice_keyword_total).lowercase()

    LaunchedEffect(setupStep) {
        isScreenDimmed = true
        
        if (setupStep == 0) {
            ttsManager.speak(langPromptEn, isVietnamese = false) {
                ttsManager.speak(langPromptVi, isVietnamese = true, isQueued = true) {
                    scope.launch {
                        isScreenDimmed = false
                    }
                }
            }
        } else {
            ttsManager.speak(
                greeting,
                isVietnamese = settingsManager.useVietnamese,
                onComplete = {
                    scope.launch {
                        isScreenDimmed = false
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceSessionId.value?.let { voiceCommandManager.stopListening(it) }
        }
    }

    LaunchedEffect(setupStep) {
        // Voice listener only starts after a short delay to avoid hearing the prompt itself
        delay(1000)
        voiceSessionId.value = voiceCommandManager.startListening(
            isVietnamese = settingsManager.useVietnamese,
            partialCallback = { partial ->
                val lowerPartial = partial.trim().lowercase()
                if (setupStep == 0) {
                    if (lowerPartial.contains(kwEn) || lowerPartial.contains("english")) {
                        interactionsEnabled = false
                        selectedLanguage = false
                        scope.launch { handleLanguageSelection(false, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                    } else if (lowerPartial.contains(kwVi) || lowerPartial.contains("vietnamese") || lowerPartial.contains("tiếng việt")) {
                        interactionsEnabled = false
                        selectedLanguage = true
                        scope.launch { handleLanguageSelection(true, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                    }
                } else {
                    if (lowerPartial.contains(keywordPartial)) {
                        interactionsEnabled = false
                        selectedLevel = ImpairmentLevel.PartiallyImpaired
                        scope.launch {
                            handleSelection(ImpairmentLevel.PartiallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomePartial, settingsManager.useVietnamese)
                        }
                    } else if (lowerPartial.contains(keywordTotal)) {
                        interactionsEnabled = false
                        selectedLevel = ImpairmentLevel.TotallyImpaired
                        scope.launch {
                            handleSelection(ImpairmentLevel.TotallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomeTotal, settingsManager.useVietnamese)
                        }
                    }
                }
            }
        ) { result ->
            val lowerResult = result.trim().lowercase()
            if (setupStep == 0) {
                if (lowerResult.contains(kwEn) || lowerResult.contains("english")) {
                    interactionsEnabled = false
                    selectedLanguage = false
                    scope.launch { handleLanguageSelection(false, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                } else if (lowerResult.contains(kwVi) || lowerResult.contains("vietnamese") || lowerResult.contains("tiếng việt")) {
                    interactionsEnabled = false
                    selectedLanguage = true
                    scope.launch { handleLanguageSelection(true, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                }
            } else {
                if (lowerResult.contains(keywordPartial)) {
                    interactionsEnabled = false
                    selectedLevel = ImpairmentLevel.PartiallyImpaired
                    scope.launch {
                        handleSelection(ImpairmentLevel.PartiallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomePartial, settingsManager.useVietnamese)
                    }
                } else if (lowerResult.contains(keywordTotal)) {
                    interactionsEnabled = false
                    selectedLevel = ImpairmentLevel.TotallyImpaired
                    scope.launch {
                        handleSelection(ImpairmentLevel.TotallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomeTotal, settingsManager.useVietnamese)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF333333))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (setupStep == 0) {
                val colorEn = if (selectedLanguage == false) Color.Yellow else if (selectedLanguage == null) Color.White else Color.Gray
                val colorVi = if (selectedLanguage == true) Color.Yellow else if (selectedLanguage == null) Color.White else Color.Gray

                AccessibleButton(
                    label = langEn,
                    speakLabel = langEn,
                    enabled = interactionsEnabled,
                    onClick = {
                        interactionsEnabled = false
                        selectedLanguage = false
                        scope.launch { handleLanguageSelection(false, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                    },
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    hapticManager = hapticManager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    backgroundColor = Color(0xFF222222),
                    textColor = colorEn,
                    fontSizeScale = 3f
                )
                Spacer(modifier = Modifier.height(4.dp))
                AccessibleButton(
                    label = langVi,
                    speakLabel = langVi,
                    enabled = interactionsEnabled,
                    onClick = {
                        interactionsEnabled = false
                        selectedLanguage = true
                        scope.launch { handleLanguageSelection(true, settingsManager, ttsManager) { setupStep = 1; interactionsEnabled = true } }
                    },
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    hapticManager = hapticManager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    backgroundColor = Color(0xFF222222),
                    textColor = colorVi,
                    fontSizeScale = 3f
                )
            } else {
                val colorPartial = if (selectedLevel == ImpairmentLevel.PartiallyImpaired) Color.Yellow else if (selectedLevel == null) Color.White else Color.Gray
                val colorTotal = if (selectedLevel == ImpairmentLevel.TotallyImpaired) Color.Yellow else if (selectedLevel == null) Color.White else Color.Gray

                AccessibleButton(
                    label = labelPartial,
                    speakLabel = labelPartial,
                    enabled = interactionsEnabled,
                    onClick = {
                        interactionsEnabled = false
                        selectedLevel = ImpairmentLevel.PartiallyImpaired
                        scope.launch {
                            handleSelection(ImpairmentLevel.PartiallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomePartial, settingsManager.useVietnamese)
                        }
                    },
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    hapticManager = hapticManager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    backgroundColor = Color(0xFF222222),
                    textColor = colorPartial,
                    fontSizeScale = 3f
                )
                Spacer(modifier = Modifier.height(4.dp))
                AccessibleButton(
                    label = labelTotal,
                    speakLabel = labelTotal,
                    enabled = interactionsEnabled,
                    onClick = {
                        interactionsEnabled = false
                        selectedLevel = ImpairmentLevel.TotallyImpaired
                        scope.launch {
                            handleSelection(ImpairmentLevel.TotallyImpaired, settingsManager, navController, ttsManager, hapticManager, welcomeTotal, settingsManager.useVietnamese)
                        }
                    },
                    ttsManager = ttsManager,
                    settingsManager = settingsManager,
                    hapticManager = hapticManager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    backgroundColor = Color(0xFF222222),
                    textColor = colorTotal,
                    fontSizeScale = 3f
                )
            }
        }
        
        // Dimming overlay
        if (isScreenDimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    // Do NOT consume touch if we want interactionsEnabled to work
                    // Or keep it simple and just show buttons through alpha
            )
        }
    }
}

private suspend fun handleLanguageSelection(
    isVietnamese: Boolean,
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    onComplete: () -> Unit
) {
    settingsManager.useVietnamese = isVietnamese
    ttsManager.setLanguage(isVietnamese)
    delay(1000)
    onComplete()
}

private suspend fun handleSelection(
    level: ImpairmentLevel,
    settingsManager: SettingsManager,
    navController: NavController,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    confirmationText: String,
    useVietnamese: Boolean
) {
    settingsManager.updateImpairmentLevel(level)
    delay(1000)
    
    hapticManager.triggerHaptic()
    
    ttsManager.speak(confirmationText, isVietnamese = useVietnamese) {
        navController.navigate("tutorial") {
            popUpTo("setup") { inclusive = true }
        }
    }
}
