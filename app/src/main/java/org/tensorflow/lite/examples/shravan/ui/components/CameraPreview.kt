package org.tensorflow.lite.examples.shravan.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CameraCharacteristics

object CameraExecutorManager {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    zoomRatio: Float = 1.0f,
    imageAnalyzer: ImageAnalysis.Analyzer? = null,
    onReady: () -> Unit = {},
    torchEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = CameraExecutorManager.executor
    val currentOnReady by rememberUpdatedState(onReady)

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(torchEnabled, cameraControl) {
        val control = cameraControl
        if (control != null) {
            try {
                control.enableTorch(torchEnabled)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to enable torch", e)
            }
        }
    }

    LaunchedEffect(context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to get camera provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }

    LaunchedEffect(cameraProvider, imageAnalyzer, zoomRatio, previewView) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect
        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            previewUseCase = preview

            val backCameraInfos = provider.availableCameraInfos.filter { info ->
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                    .filter(listOf(info))
                    .isNotEmpty()
            }

            val cameraSelector = if (zoomRatio < 1.0f && backCameraInfos.size > 1) {
                // Try to find the ultra-wide camera
                val ultraWide = backCameraInfos.find { info ->
                    try {
                        val characteristics = Camera2CameraInfo.from(info)
                        val focalLengths = characteristics.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        // Heuristic: focal length < 3mm is usually ultra-wide
                        focalLengths?.any { it < 3.0f } == true
                    } catch (e: Exception) {
                        false
                    }
                }
                ultraWide?.let { info ->
                    CameraSelector.Builder().addCameraFilter { it.filter { i -> i == info } }.build()
                } ?: CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val camera = if (imageAnalyzer != null) {
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor, imageAnalyzer)
                analysisUseCase = analysis
                
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analysis
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            }
            cameraControl = camera.cameraControl
            
            try {
                val zoomState = camera.cameraInfo.zoomState.value
                val minZoom = zoomState?.minZoomRatio ?: 1.0f
                val maxZoom = zoomState?.maxZoomRatio ?: 1.0f
                camera.cameraControl.setZoomRatio(zoomRatio.coerceIn(minZoom, maxZoom))
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to set zoom", e)
            }
            
            currentOnReady()
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(cameraProvider) {
        onDispose {
            try {
                analysisUseCase?.clearAnalyzer()
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error unbinding on dispose", e)
            }
        }
    }
}
