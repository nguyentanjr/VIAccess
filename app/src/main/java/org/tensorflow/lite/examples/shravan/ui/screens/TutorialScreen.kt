package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.HapticManager
import org.tensorflow.lite.examples.shravan.utils.SettingsManager
import org.tensorflow.lite.examples.shravan.utils.TTSManager

@Composable
fun TutorialScreen(
    navController: NavController,
    settingsManager: SettingsManager,
    ttsManager: TTSManager,
    hapticManager: HapticManager,
    onRequestPermissions: () -> Unit,
    hasPermissions: Boolean
) {
    val useVietnamese = settingsManager.useVietnamese
    val scope = rememberCoroutineScope()
    var isTutorialFinished by remember { mutableStateOf(false) }
    var requestingPermissions by remember { mutableStateOf(false) }
    
    val tutorialText = stringResource(R.string.tutorial_text)
    val skipInstruction = stringResource(R.string.tutorial_skip_instruction)
    
    var interactionsEnabled by remember { mutableStateOf(false) }

    fun finishTutorial() {
        if (!isTutorialFinished) {
            ttsManager.stopAll()
            isTutorialFinished = true
            requestingPermissions = true
            hapticManager.triggerHaptic()
            // Only request if needed, otherwise the LaunchedEffect will handle navigation
            if (!hasPermissions) {
                onRequestPermissions()
            }
        }
    }

    LaunchedEffect(hasPermissions, requestingPermissions) {
        if (hasPermissions && requestingPermissions) {
            navController.navigate("main") {
                popUpTo("tutorial") { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(800) // Slightly longer delay for stability
        interactionsEnabled = true
        ttsManager.speak(tutorialText, isVietnamese = useVietnamese)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
            .then(
                if (interactionsEnabled && !isTutorialFinished) {
                    Modifier.clickable { finishTutorial() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (requestingPermissions && !hasPermissions) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (useVietnamese) "Vui lòng cấp quyền để tiếp tục..." else "Please grant permissions to continue...",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(
                    text = if (isTutorialFinished) skipInstruction else tutorialText,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                if (!isTutorialFinished) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = skipInstruction,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}