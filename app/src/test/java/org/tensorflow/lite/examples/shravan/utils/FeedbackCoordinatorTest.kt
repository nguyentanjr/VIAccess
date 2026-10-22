package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedbackCoordinatorTest {

    private class FakeSpeech : SpeechOutput {
        val spoken = mutableListOf<String>()
        var speaking = false
        private var onDone: (() -> Unit)? = null

        override fun speak(text: String, isVietnamese: Boolean, onDone: (() -> Unit)?) {
            spoken += text
            speaking = true
            this.onDone = onDone
        }
        override fun isSpeaking() = speaking
        override fun stop() { finish() }

        fun finish() {
            speaking = false
            val cb = onDone
            onDone = null
            cb?.invoke()
        }
    }

    private class FakeHaptics : HapticOutput {
        val patterns = mutableListOf<HapticPattern>()
        override fun vibrate(pattern: HapticPattern) { patterns += pattern }
    }

    private lateinit var speech: FakeSpeech
    private lateinit var haptics: FakeHaptics
    private var now = 0L
    private lateinit var fc: FeedbackCoordinator

    @Before
    fun setUp() {
        speech = FakeSpeech()
        haptics = FakeHaptics()
        now = 0L
        fc = FeedbackCoordinator(speech, haptics, defaultVietnamese = { true }, clock = { now })
    }

    @Test
    fun sameKeyIsNotRepeatedWithinCooldown() {
        assertTrue(fc.say("Lệch trái", Priority.GUIDANCE))
        speech.finish()
        now += FeedbackConfig.REPEAT_COOLDOWN_MS - 1
        assertFalse(fc.say("Lệch trái", Priority.GUIDANCE))
        now += 1
        assertTrue(fc.say("Lệch trái", Priority.GUIDANCE))
        assertEquals(2, speech.spoken.size)
    }

    @Test
    fun higherPriorityInterruptsLowerOrEqualIsDropped() {
        fc.say("Lại gần", Priority.GUIDANCE)
        assertFalse(fc.say("Lệch phải", Priority.GUIDANCE))
        assertFalse(fc.say("thông tin", Priority.INFO))
        assertTrue(fc.say("Mất dấu", Priority.STATE))
        assertEquals(listOf("Lại gần", "Mất dấu"), speech.spoken)
    }

    @Test
    fun guidanceIsLimitedPerMinute() {
        repeat(FeedbackConfig.MAX_GUIDANCE_PER_MIN) { i ->
            assertTrue(fc.say("nhắc $i", Priority.GUIDANCE))
            speech.finish()
            now += 1000
        }
        assertFalse(fc.say("nhắc thêm", Priority.GUIDANCE))
        // STATE không bị giới hạn
        assertTrue(fc.say("Đã thấy cốc", Priority.STATE))
        speech.finish()
        now += FeedbackConfig.GUIDANCE_WINDOW_MS
        assertTrue(fc.say("nhắc thêm", Priority.GUIDANCE))
    }

    @Test
    fun userInitiatedBypassesCooldownAndPriority() {
        fc.say("Đang đọc văn bản dài", Priority.STATE)
        assertTrue(fc.say("Đang đọc văn bản dài", Priority.STATE, userInitiated = true))
    }

    @Test
    fun queuedUtterancesPlayInOrder() {
        fc.say("Chào", Priority.STATE, queue = true)
        fc.say("Hãy nói tên vật", Priority.STATE, queue = true)
        assertEquals(listOf("Chào"), speech.spoken)
        speech.finish()
        assertEquals(listOf("Chào", "Hãy nói tên vật"), speech.spoken)
    }

    @Test
    fun queuedAfterNonQueuedPlaysWhenItFinishes() {
        fc.say("Đã thấy cốc", Priority.STATE)
        fc.say("bên trái", Priority.STATE, queue = true)
        assertEquals(1, speech.spoken.size)
        speech.finish()
        assertEquals(listOf("Đã thấy cốc", "bên trái"), speech.spoken)
    }

    @Test
    fun droppedUtteranceStillCallsOnDone() {
        fc.say("Lại gần", Priority.GUIDANCE)
        var done = false
        fc.say("Lệch phải", Priority.GUIDANCE) { done = true }
        assertTrue(done)
    }

    @Test
    fun vibrationIsBlockedWhileSpeakingUnlessForced() {
        fc.say("Đang nói", Priority.STATE)
        assertFalse(fc.vibrate(HapticPattern.DEFAULT))
        assertTrue(fc.vibrate(HapticPattern.LONG, force = true))
        speech.finish()
        assertTrue(fc.vibrate(HapticPattern.SHORT))
        assertEquals(listOf(HapticPattern.LONG, HapticPattern.SHORT), haptics.patterns)
    }
}
