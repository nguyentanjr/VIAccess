package org.tensorflow.lite.examples.shravan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import org.tensorflow.lite.examples.shravan.ui.screens.*
import org.tensorflow.lite.examples.shravan.ui.theme.ShravanTheme
import org.tensorflow.lite.examples.shravan.utils.*

class MainActivity : ComponentActivity() {
    private var ttsManagerInstance: TTSManager? = null
    private var feedbackInstance: FeedbackCoordinator? = null

    private var voiceInstance: VoiceCommandManager? = null

    override fun onPause() {
        super.onPause()
        // App chạy nền: dừng lời nói + hàng đợi, chặn rung mới, ngừng nghe mic.
        // Camera tự dừng vì CameraX gắn với lifecycle của Activity.
        feedbackInstance?.setSuspended(true)
        ttsManagerInstance?.stopAll()
        voiceInstance?.suspend()
    }

    override fun onResume() {
        super.onResume()
        feedbackInstance?.setSuspended(false)
        voiceInstance?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManagerInstance?.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("SHRAVAN_DEBUG", "MainActivity onCreate started")
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            android.util.Log.d("SHRAVAN_DEBUG", "Window configured")
            setContent {
                val context = LocalContext.current
                android.util.Log.d("SHRAVAN_DEBUG", "setContent started")
                
                val settingsManager = remember { 
                    try { SettingsManager(context) } catch (e: Throwable) { 
                        android.util.Log.e("MainActivity", "SettingsManager init failed", e)
                        null 
                    }
                }
                val ttsManager = remember { 
                    try { 
                        val manager = TTSManager(context)
                        ttsManagerInstance = manager
                        manager 
                    } catch (e: Throwable) { 
                        android.util.Log.e("MainActivity", "TTSManager init failed", e)
                        null 
                    }
                }
                val hapticManager = remember(settingsManager) { 
                    try { 
                        if (settingsManager != null) HapticManager(context, settingsManager) else null
                    } catch (e: Throwable) { 
                        android.util.Log.e("MainActivity", "HapticManager init failed", e)
                        null 
                    }
                }
                val voiceCommandManager = remember { 
                    try { VoiceCommandManager(context).also { voiceInstance = it } } catch (e: Throwable) {
                        android.util.Log.e("MainActivity", "VoiceCommandManager init failed", e)
                        null 
                    }
                }

                val feedback = remember(ttsManager, hapticManager, settingsManager) {
                    if (ttsManager != null && hapticManager != null && settingsManager != null)
                        createFeedbackCoordinator(ttsManager, hapticManager, settingsManager).also { feedbackInstance = it }
                    else null
                }

                if (feedback == null || settingsManager == null || ttsManager == null || 
                    hapticManager == null || voiceCommandManager == null) {
                    // Critical failure, show simple error
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            androidx.compose.material3.Text(
                                "Critical system failure. Please restart the app.",
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    return@setContent
                }
val useVietnamese = settingsManager.useVietnamese
val locale = remember { java.util.Locale("vi") }

LaunchedEffect(locale) {
    java.util.Locale.setDefault(locale)
    val resources = this@MainActivity.resources
    val configuration = android.content.res.Configuration(resources.configuration)
    configuration.setLocale(locale)
    resources.updateConfiguration(configuration, resources.displayMetrics)
}

val configuration = LocalConfiguration.current
val overriddenConfiguration = remember(locale, configuration) {
    android.content.res.Configuration(configuration).apply {
        setLocale(locale)
    }
}

CompositionLocalProvider(LocalConfiguration provides overriddenConfiguration) {
    ShravanTheme(themeIndex = settingsManager.activeThemeIndex) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF222222)
    ) {

                        val navController = rememberNavController()

                        var hasCameraPermission by remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            )
                        }
                        
                        var hasRecordAudioPermission by remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            )
                        }

                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestMultiplePermissions(),
                            onResult = { permissions ->
                                hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
                                hasRecordAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasRecordAudioPermission
                            }
                        )

                        var permissionAsked by remember { mutableStateOf(false) }

                        // Quay lại từ màn cài đặt hệ thống: đọc lại quyền.
                        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                    hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    hasRecordAudioPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        // Lỗi TTS: không khởi động được, hoặc thiếu giọng tiếng Việt.
                        var showMissingVoice by remember { mutableStateOf(false) }
                        DisposableEffect(ttsManager) {
                            ttsManager.onVietnameseVoiceMissing = {
                                showMissingVoice = true
                                feedback.vibrate(HapticPattern.DOUBLE, force = true)
                            }
                            ttsManager.onInitFailed = {
                                feedback.vibrate(HapticPattern.DOUBLE, force = true)
                                android.widget.Toast.makeText(
                                    context, context.getString(R.string.error_tts_init),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            onDispose {
                                ttsManager.onVietnameseVoiceMissing = null
                                ttsManager.onInitFailed = null
                            }
                        }
                        if (showMissingVoice) {
                            MissingVoiceDialog(
                                onInstall = {
                                    showMissingVoice = false
                                    try {
                                        context.startActivity(
                                            android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (e: Exception) {
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent("com.android.settings.TTS_SETTINGS")
                                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }
                                },
                                onDismiss = { showMissingVoice = false }
                            )
                        }

                        LaunchedEffect(settingsManager.speechRate) {
                            ttsManager.setSpeechRate(settingsManager.speechRate)
                        }

                        LaunchedEffect(settingsManager.useVietnamese) {
                            ttsManager.setLanguage(settingsManager.useVietnamese)
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                voiceCommandManager.destroy()
                            }
                        }

                        val startDestination = remember { if (settingsManager.impairmentLevel == null) "setup" else "main" }

                        NavHost(
                            navController = navController, 
                            startDestination = startDestination,
                            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) },
                            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) }
                        ) {
                            composable("setup") {
                                SetupHomeScreen(
                                    navController = navController,
                                    settingsManager = settingsManager,
                                    ttsManager = ttsManager,
                                    hapticManager = hapticManager,
                                    voiceCommandManager = voiceCommandManager
                                )
                            }
                            composable("tutorial") {
                                TutorialScreen(
                                    navController = navController,
                                    settingsManager = settingsManager,
                                    ttsManager = ttsManager,
                                    hapticManager = hapticManager,
                                    onRequestPermissions = {
                                        val permissionsNeeded = mutableListOf<String>()
                                        if (!hasCameraPermission) permissionsNeeded.add(Manifest.permission.CAMERA)
                                        if (!hasRecordAudioPermission) permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
                                        
                                        if (permissionsNeeded.isNotEmpty()) {
                                            launcher.launch(permissionsNeeded.toTypedArray())
                                        }
                                    },
                                    hasPermissions = hasCameraPermission && hasRecordAudioPermission
                                )
                            }
                            composable("main") {
                                if (hasCameraPermission && hasRecordAudioPermission) {
                                    MainScreen(
                                        settingsManager = settingsManager,
                                        ttsManager = ttsManager,
                                        hapticManager = hapticManager,
                                        feedback = feedback,
                                        voiceCommandManager = voiceCommandManager,
                                        onReset = {
                                            settingsManager.clearAll()
                                            (context as? android.app.Activity)?.finishAffinity()
                                        }
                                    )
                                } else {
                                    // Thiếu quyền: nói ra lý do + một nút lớn để cấp quyền (hoặc mở cài đặt nếu đã bị chặn).
                                    PermissionRequiredScreen(
                                        feedback = feedback,
                                        askedBefore = permissionAsked,
                                        onRequest = {
                                            permissionAsked = true
                                            launcher.launch(
                                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "CRITICAL ERROR DURING ONCREATE", e)
            // Show a very basic view if setContent failed
            android.widget.Toast.makeText(this, "Critical error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
