package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ImpairmentLevel {
    PartiallyImpaired,
    TotallyImpaired
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shravan_prefs", Context.MODE_PRIVATE)

    var impairmentLevel by mutableStateOf(
        prefs.getString("impairment_level", null)?.let {
            try {
                ImpairmentLevel.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    )

    fun updateImpairmentLevel(level: ImpairmentLevel?) {
        impairmentLevel = level
        prefs.edit().putString("impairment_level", level?.name).apply()
    }

    var activeThemeIndex by mutableStateOf(prefs.getInt("active_theme_index", 3))

    fun updateActiveThemeIndex(index: Int) {
        activeThemeIndex = index
        prefs.edit().putInt("active_theme_index", index).apply()
    }

    private var _speechRate = mutableStateOf(prefs.getFloat("speech_rate", 1.0f))
    var speechRate: Float
        get() = _speechRate.value
        set(value) {
            _speechRate.value = value
            prefs.edit().putFloat("speech_rate", value).apply()
        }

    private var _hapticsEnabled = mutableStateOf(prefs.getBoolean("haptics_enabled", true))
    var hapticsEnabled: Boolean
        get() = _hapticsEnabled.value
        set(value) {
            _hapticsEnabled.value = value
            prefs.edit().putBoolean("haptics_enabled", value).apply()
        }

    private var _baselineMode = mutableStateOf(prefs.getBoolean("baseline_mode", false))
    var baselineMode: Boolean
        get() = _baselineMode.value
        set(value) {
            _baselineMode.value = value
            prefs.edit().putBoolean("baseline_mode", value).apply()
        }

    /** App chỉ hỗ trợ tiếng Việt (PLAN 1.3). Giữ thuộc tính để code cũ đọc được, luôn là true. */
    val useVietnamese: Boolean get() = true

    private var _flashMode = mutableStateOf(prefs.getString("flash_mode", "auto") ?: "auto")
    var flashMode: String
        get() = _flashMode.value
        set(value) {
            _flashMode.value = value
            prefs.edit().putString("flash_mode", value).apply()
        }

    fun clearAll() {
        prefs.edit().clear().apply()
        impairmentLevel = null
        activeThemeIndex = 3
        speechRate = 1.0f
        hapticsEnabled = true
        baselineMode = false
        flashMode = "auto"
    }
}
