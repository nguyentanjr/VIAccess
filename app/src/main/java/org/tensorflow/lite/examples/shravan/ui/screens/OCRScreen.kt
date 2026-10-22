package org.tensorflow.lite.examples.shravan.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.shravan.R
import org.tensorflow.lite.examples.shravan.utils.*

@OptIn(ExperimentalFoundationApi::class, androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun OCRScreen(
    onBack: () -> Unit,
    ttsManager: TTSManager,
    settingsManager: SettingsManager,
    historyManager: HistoryManager,
    hapticManager: HapticManager,
    voiceCommandManager: VoiceCommandManager,
    isActive: Boolean = true,
    ocrManager: OCRManager? = null,
    onProvideAnalyzer: (androidx.camera.core.ImageAnalysis.Analyzer?) -> Unit = {}
) {
    val recognizer = remember(isActive) { 
        if (isActive) TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) else null 
    }
    
    val useVietnamese = settingsManager.useVietnamese
    var isProcessing by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }

    LaunchedEffect(isActive, recognizer) {
        if (isActive) {
            onProvideAnalyzer { imageProxy ->
                if (isProcessing) {
                    imageProxy.close()
                    return@onProvideAnalyzer
                }

                val mediaImage = imageProxy.image
                val rec = recognizer
                if (mediaImage != null && rec != null) {
                    isProcessing = true
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    rec.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            if (text.isNotBlank()) {
                                currentText = text
                                // Automatic reading logic
                                if (ocrManager?.shouldSpeak(text) == true) {
                                    ttsManager.speak(text, isVietnamese = containsVietnamese(text))
                                    historyManager.addHistory("OCR", text)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("OCRScreen", "OCR Failed", e)
                        }
                        .addOnCompleteListener {
                            isProcessing = false
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }
        } else {
            onProvideAnalyzer(null)
        }
    }

    DisposableEffect(recognizer) {
        onDispose {
            recognizer?.close()
        }
    }

    val scope = rememberCoroutineScope()
    var interactionsEnabled by remember { mutableStateOf(false) }
    val voiceSessionId = remember { mutableStateOf<Int?>(null) }

    BackHandler {
        hapticManager.triggerHaptic()
        onBack()
    }

    val ocrGreeting = stringResource(R.string.ocr_greeting)
    val backCommand = stringResource(R.string.back_command)

    LaunchedEffect(isActive) {
        if (isActive) {
            ocrManager?.clear()
            ttsManager.speak(
                ocrGreeting,
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
                if (lowerResult.contains(backCommand.lowercase())) {
                    ttsManager.speak(backCommand, isVietnamese = useVietnamese)
                    hapticManager.triggerHaptic()
                    onBack()
                } else if (lowerResult.contains("đọc") || lowerResult.contains("read")) {
                    hapticManager.triggerHaptic()
                    if (currentText.isBlank()) {
                        ttsManager.speak(if (useVietnamese) "Không có văn bản nào" else "No text to read", isVietnamese = useVietnamese)
                    } else {
                        ttsManager.speak(currentText, isVietnamese = containsVietnamese(currentText))
                        historyManager.addHistory("OCR", currentText)
                    }
                }
            }
        } else {
            interactionsEnabled = false
            voiceSessionId.value?.let {
                voiceCommandManager.stopListening(it)
                voiceSessionId.value = null
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (interactionsEnabled) {
                        hapticManager.triggerHaptic()
                        if (currentText.isBlank()) {
                            ttsManager.speak(if (useVietnamese) "Không có văn bản nào" else "No text to read", isVietnamese = useVietnamese)
                        } else {
                            ttsManager.speak(currentText, isVietnamese = containsVietnamese(currentText))
                            historyManager.addHistory("OCR", currentText)
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (currentText.isNotBlank()) {
                Text(
                    text = currentText,
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                )
            }
        }
    }
}

private fun containsVietnamese(text: String): Boolean {
    val viChars = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ"
    return text.lowercase().any { it in viChars }
}
