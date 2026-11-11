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
    feedback: FeedbackCoordinator,
    settingsManager: SettingsManager,
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val motionMonitor = remember { MotionMonitor(context) }
    val advisor = remember { AlignmentAdvisor() }
    val reader = remember { ReadingController(feedback) }
    val adviceText = mapOf(
        AlignmentAdvice.DARK to stringResource(R.string.align_dark),
        AlignmentAdvice.SHAKY to stringResource(R.string.align_shaky),
        AlignmentAdvice.BLURRY to stringResource(R.string.align_blurry),
        AlignmentAdvice.TOO_CLOSE to stringResource(R.string.align_too_close),
        AlignmentAdvice.OFF_LEFT to stringResource(R.string.align_off_left),
        AlignmentAdvice.OFF_RIGHT to stringResource(R.string.align_off_right),
        AlignmentAdvice.TOO_SMALL to stringResource(R.string.align_too_small),
        AlignmentAdvice.NO_TEXT to stringResource(R.string.align_no_text)
    )

    // Gyroscope chỉ chạy khi màn Đọc chữ đang mở.
    DisposableEffect(isActive) {
        if (isActive) motionMonitor.start()
        onDispose { motionMonitor.stop() }
    }

    // --- QR / mã vạch (ML Kit Barcode, chạy trên máy) ---
    val barcodeScanner = remember { com.google.mlkit.vision.barcode.BarcodeScanning.getClient() }
    DisposableEffect(barcodeScanner) { onDispose { barcodeScanner.close() } }
    val lastBarcode = remember { mutableMapOf<String, Long>() }
    val qrStrings = mapOf(
        "text" to stringResource(R.string.qr_text),
        "link" to stringResource(R.string.qr_link),
        "wifi" to stringResource(R.string.qr_wifi),
        "wifiOpen" to stringResource(R.string.qr_wifi_open),
        "phone" to stringResource(R.string.qr_phone),
        "vietqr" to stringResource(R.string.qr_vietqr),
        "vqName" to stringResource(R.string.qr_vietqr_name),
        "vqAcc" to stringResource(R.string.qr_vietqr_account),
        "vqAmt" to stringResource(R.string.qr_vietqr_amount),
        "product" to stringResource(R.string.barcode_product)
    )
    fun announceBarcode(code: com.google.mlkit.vision.barcode.common.Barcode) {
        val raw = code.rawValue ?: return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - (lastBarcode[raw] ?: -FeedbackConfig.BARCODE_REPEAT_MS) < FeedbackConfig.BARCODE_REPEAT_MS) return
        lastBarcode[raw] = now
        val product = code.format in setOf(
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13,
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8,
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A,
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
        )
        fun s(k: String, vararg a: Any) = String.format(qrStrings.getValue(k), *a)
        val text = when (val info = BarcodeInfo.parse(raw, product)) {
            is BarcodeInfo.Link -> s("link", info.host)
            is BarcodeInfo.Wifi -> if (info.password != null) s("wifi", info.ssid, info.password) else s("wifiOpen", info.ssid)
            is BarcodeInfo.Phone -> s("phone", info.number)
            is BarcodeInfo.Product -> s("product", info.digits.toCharArray().joinToString(" "))
            is BarcodeInfo.VietQr -> listOfNotNull(
                qrStrings.getValue("vietqr"),
                info.name?.let { s("vqName", it) },
                info.account?.let { s("vqAcc", it.toCharArray().joinToString(" ")) },
                info.amount?.let { s("vqAmt", it) }
            ).joinToString(", ")
            is BarcodeInfo.Text -> s("text", info.text)
        }
        feedback.vibrate(HapticPattern.SHORT, force = true)
        feedback.say(text, Priority.STATE, key = "barcode:$raw", userInitiated = true)
    }

    LaunchedEffect(isActive, recognizer) {
        if (isActive) {
            advisor.reset()
            onProvideAnalyzer { imageProxy ->
                if (isProcessing) {
                    imageProxy.close()
                    return@onProvideAnalyzer
                }

                val mediaImage = imageProxy.image
                val rec = recognizer
                if (mediaImage != null && rec != null) {
                    isProcessing = true
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    // Đo chất lượng trên kênh Y trước khi ML Kit đọc ảnh.
                    val gray = ImageQuality.sampleLuma(imageProxy)
                    val luminance = ImageQuality.meanLuminance(gray)
                    val sharpness = ImageQuality.laplacianVariance(gray)
                    val shaking = motionMonitor.isShaking()
                    val (uprightW, uprightH) = if (rotation % 180 == 0) {
                        imageProxy.width to imageProxy.height
                    } else {
                        imageProxy.height to imageProxy.width
                    }
                    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                    // QR / mã vạch chạy song song với đọc chữ trên cùng khung hình.
                    val barcodeTask = barcodeScanner.process(inputImage)
                        .addOnSuccessListener { codes -> codes.firstOrNull()?.let { announceBarcode(it) } }
                    val textTask = rec.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            if (text.isNotBlank()) currentText = text

                            if (settingsManager.baselineMode) {
                                // Baseline (VisionAid): không nhắc căn chỉnh, đọc liên tục khi chữ ổn định.
                                if (text.isNotBlank() && ocrManager?.shouldSpeak(text) == true) {
                                    feedback.say(text, Priority.STATE, key = "ocr:auto", isVietnamese = containsVietnamese(text))
                                }
                                return@addOnSuccessListener
                            }

                            val box = ImageQuality.unionBox(
                                visionText.textBlocks.mapNotNull { block ->
                                    block.boundingBox?.let { intArrayOf(it.left, it.top, it.right, it.bottom) }
                                },
                                uprightW,
                                uprightH
                            )
                            val advice = advisor.evaluate(
                                FrameQuality(
                                    luminance = luminance,
                                    flashUnavailable = settingsManager.flashMode == "off",
                                    shaking = shaking,
                                    laplacianVariance = sharpness,
                                    textBox = if (text.isBlank()) null else box
                                ),
                                android.os.SystemClock.elapsedRealtime()
                            )
                            when (advice) {
                                AlignmentAdvice.NONE -> Unit
                                AlignmentAdvice.OK -> {
                                    // Tự chụp: đạt hết điều kiện + chữ ổn định 3/5 frame (OCRManager) → rung ngắn → đọc.
                                    if (ocrManager?.shouldSpeak(text) == true) {
                                        feedback.vibrate(HapticPattern.SHORT, force = true)
                                        // Đọc theo từng câu để dừng / tiếp / đọc lại đúng chỗ.
                                        reader.start(text, containsVietnamese(text))
                                    }
                                }
                                else -> adviceText[advice]?.let {
                                    feedback.say(it, Priority.GUIDANCE, key = "align:$advice")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("OCRScreen", "OCR Failed", e)
                        }
                    com.google.android.gms.tasks.Tasks.whenAllComplete(textTask, barcodeTask)
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
        feedback.vibrate(force = true)
        onBack()
    }

    val ocrGreeting = stringResource(R.string.ocr_greeting)
    val backCommand = stringResource(R.string.back_command)
    val noText = stringResource(R.string.ocr_no_text)

    LaunchedEffect(isActive) {
        if (isActive) {
            ocrManager?.clear()
            feedback.say(
                ocrGreeting,
                Priority.STATE,
                userInitiated = true,
                onDone = {
                    scope.launch {
                        delay(1000)
                        interactionsEnabled = true
                    }
                }
            )
            
            voiceSessionId.value = voiceCommandManager.startListening(isVietnamese = useVietnamese) { result ->
                val lowerResult = result.lowercase()
                if (lowerResult.contains(backCommand.lowercase())) {
                    feedback.say(backCommand, Priority.STATE, userInitiated = true)
                    feedback.vibrate(force = true)
                    onBack()
                } else if (lowerResult.contains("đọc") || lowerResult.contains("read")) {
                    feedback.vibrate(force = true)
                    readCurrent(feedback, reader, currentText, noText)
                }
            }
        } else {
            interactionsEnabled = false
            reader.stop()
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
                        feedback.vibrate(force = true)
                        readCurrent(feedback, reader, currentText, noText)
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

private fun readCurrent(feedback: FeedbackCoordinator, reader: ReadingController, text: String, noText: String) {
    if (text.isBlank()) {
        feedback.say(noText, Priority.STATE, userInitiated = true)
    } else {
        reader.start(text, containsVietnamese(text))
    }
}

private fun containsVietnamese(text: String): Boolean {
    val viChars = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ"
    return text.lowercase().any { it in viChars }
}
