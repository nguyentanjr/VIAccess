package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {
    private lateinit var settingsManager: SettingsManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testDefaultValues() {
        assertNull(settingsManager.impairmentLevel)
        assertEquals(3, settingsManager.activeThemeIndex)
        assertEquals(1.0f, settingsManager.speechRate)
        assertTrue(settingsManager.hapticsEnabled)
        assertFalse(settingsManager.useVietnamese)
    }

    @Test
    fun testUpdateImpairmentLevel() {
        settingsManager.updateImpairmentLevel(ImpairmentLevel.PartiallyImpaired)
        assertEquals(ImpairmentLevel.PartiallyImpaired, settingsManager.impairmentLevel)
        
        settingsManager.updateImpairmentLevel(ImpairmentLevel.TotallyImpaired)
        assertEquals(ImpairmentLevel.TotallyImpaired, settingsManager.impairmentLevel)
    }

    @Test
    fun testUpdateThemeIndex() {
        settingsManager.updateActiveThemeIndex(5)
        assertEquals(5, settingsManager.activeThemeIndex)
    }

    @Test
    fun testUpdateVibration() {
        settingsManager.hapticsEnabled = false
        assertFalse(settingsManager.hapticsEnabled)
    }
}
