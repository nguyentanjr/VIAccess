package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlignmentAdvisorTest {

    private val goodBox = TextBox(0.2f, 0.3f, 0.8f, 0.6f) // giữa, 18% diện tích, cách mép 20%
    private fun q(
        lum: Double = 100.0,
        flashUnavailable: Boolean = false,
        shaking: Boolean = false,
        lap: Double = 500.0,
        box: TextBox? = goodBox
    ) = FrameQuality(lum, flashUnavailable, shaking, lap, box)

    private val advisor = AlignmentAdvisor()

    @Test
    fun goodFrameIsOk() = assertEquals(AlignmentAdvice.OK, advisor.evaluate(q(), 0))

    @Test
    fun rulesAreCheckedInPriorityOrder() {
        // Mọi điều kiện đều sai → chỉ nhắc điều quan trọng nhất.
        val bad = q(lum = 10.0, flashUnavailable = true, shaking = true, lap = 1.0, box = TextBox(0f, 0f, 0.1f, 0.1f))
        assertEquals(AlignmentAdvice.DARK, advisor.evaluate(bad, 0))
        assertEquals(AlignmentAdvice.SHAKY, advisor.evaluate(bad.copy(luminance = 100.0), 0))
        assertEquals(AlignmentAdvice.BLURRY, advisor.evaluate(bad.copy(luminance = 100.0, shaking = false), 0))
        assertEquals(AlignmentAdvice.TOO_CLOSE, advisor.evaluate(bad.copy(luminance = 100.0, shaking = false, laplacianVariance = 500.0), 0))
    }

    @Test
    fun darkIsIgnoredWhenFlashCanTurnOn() {
        assertEquals(AlignmentAdvice.OK, advisor.evaluate(q(lum = 10.0, flashUnavailable = false), 0))
    }

    @Test
    fun boxPositionRules() {
        assertEquals(AlignmentAdvice.OFF_LEFT, advisor.evaluate(q(box = TextBox(0.06f, 0.3f, 0.4f, 0.7f)), 0))
        assertEquals(AlignmentAdvice.OFF_RIGHT, advisor.evaluate(q(box = TextBox(0.6f, 0.3f, 0.94f, 0.7f)), 0))
        assertEquals(AlignmentAdvice.TOO_SMALL, advisor.evaluate(q(box = TextBox(0.4f, 0.45f, 0.6f, 0.55f)), 0))
        assertEquals(AlignmentAdvice.TOO_CLOSE, advisor.evaluate(q(box = TextBox(0.02f, 0.3f, 0.98f, 0.7f)), 0))
    }

    @Test
    fun noTextPromptsAfterFiveSecondsThenEveryTen() {
        val none = q(box = null)
        assertEquals(AlignmentAdvice.NONE, advisor.evaluate(none, 0))
        assertEquals(AlignmentAdvice.NONE, advisor.evaluate(none, FeedbackConfig.NO_TEXT_FIRST_PROMPT_MS - 1))
        assertEquals(AlignmentAdvice.NO_TEXT, advisor.evaluate(none, FeedbackConfig.NO_TEXT_FIRST_PROMPT_MS))
        val t = FeedbackConfig.NO_TEXT_FIRST_PROMPT_MS
        assertEquals(AlignmentAdvice.NONE, advisor.evaluate(none, t + 1000))
        assertEquals(AlignmentAdvice.NO_TEXT, advisor.evaluate(none, t + FeedbackConfig.NO_TEXT_REPEAT_MS))
        // Thấy chữ thì đếm lại từ đầu.
        assertEquals(AlignmentAdvice.OK, advisor.evaluate(q(), t + 20_000))
        assertEquals(AlignmentAdvice.NONE, advisor.evaluate(none, t + 21_000))
    }

    @Test
    fun shakeDetectorWindow() {
        val d = ShakeDetector(threshold = 0.5f, windowMs = 500)
        d.onSample(0.1f, 0.1f, 0.1f, 0)
        assertFalse(d.isShaking(0))
        d.onSample(0.6f, 0f, 0f, 100)
        assertTrue(d.isShaking(600))
        assertFalse(d.isShaking(601))
    }

    @Test
    fun laplacianSeparatesSharpFromFlat() {
        val flat = GrayImage(8, 8, IntArray(64) { 128 })
        val checker = GrayImage(8, 8, IntArray(64) { i -> if ((i % 8 + i / 8) % 2 == 0) 0 else 255 })
        assertEquals(0.0, ImageQuality.laplacianVariance(flat), 1e-9)
        assertTrue(ImageQuality.laplacianVariance(checker) > FeedbackConfig.BLUR_LAPLACIAN_MIN)
    }

    @Test
    fun unionBoxNormalises() {
        val box = ImageQuality.unionBox(listOf(intArrayOf(10, 20, 30, 40), intArrayOf(50, 10, 90, 30)), 100, 100)!!
        assertEquals(TextBox(0.1f, 0.1f, 0.9f, 0.4f), box)
    }
}
