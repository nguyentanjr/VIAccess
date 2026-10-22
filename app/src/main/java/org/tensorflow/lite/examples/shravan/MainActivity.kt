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

    override fun onPause() {
        super.onPause()
        ttsManagerInstance?.stopAll()
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
                val historyManager = remember { 
                    try { HistoryManager(context) } catch (e: Throwable) { 
                        android.util.Log.e("MainActivity", "HistoryManager init failed", e)
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
                    try { VoiceCommandManager(context) } catch (e: Throwable) { 
                        android.util.Log.e("MainActivity", "VoiceCommandManager init failed", e)
                        null 
                    }
                }

                if (settingsManager == null || historyManager == null || ttsManager == null || 
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
val locale = remember(useVietnamese) { if (useVietnamese) java.util.Locale("vi") else java.util.Locale.ENGLISH }

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
                                        voiceCommandManager = voiceCommandManager,
                                        historyManager = historyManager,
                                        onReset = {
                                            settingsManager.clearAll()
                                            historyManager.clearHistory()
                                            (context as? android.app.Activity)?.finishAffinity()
                                        }
                                    )
                                } else {
                                    // Show loading or permission request state if still missing
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
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
