package org.tensorflow.lite.examples.shravan.tflite

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tensorflow.lite.examples.shravan.utils.FeedbackConfig
import org.tensorflow.lite.examples.shravan.utils.FindObservation
import org.tensorflow.lite.examples.shravan.utils.FindEvent
import org.tensorflow.lite.examples.shravan.utils.FindStateMachine
import org.tensorflow.lite.examples.shravan.utils.FindTarget

class LetterboxTest {

    @Test
    fun portraitImageIsPaddedLeftAndRightKeepingAspect() {
        // Ảnh dọc 480×640 vào 416: cao 416, rộng 312, viền 52 mỗi bên.
        val box = Letterbox.of(480, 640, 416)
        assertEquals(312f, box.contentW, 0.01f)
        assertEquals(416f, box.contentH, 0.01f)
        assertEquals(52f, box.padX, 0.01f)
        assertEquals(0f, box.padY, 0.01f)
    }

    @Test
    fun mapsLetterboxCoordinatesBackToFullFrame() {
        val box = Letterbox.of(480, 640, 416)
        // Mép trái nội dung (x = 52) → 0; mép phải (x = 364) → 416; tâm giữ nguyên.
        assertEquals(0f, box.toFullFrame(52f, 0f).first, 0.01f)
        assertEquals(416f, box.toFullFrame(364f, 416f).first, 0.01f)
        assertEquals(208f, box.toFullFrame(208f, 208f).first, 0.01f)
        // Điểm nằm trong viền bị kẹp vào khung.
        assertEquals(0f, box.toFullFrame(10f, 10f).first, 0.01f)
    }

    @Test
    fun hardTargetsUseLowerConfidenceThreshold() {
        val machine = FindStateMachine()
        val obs = listOf(FindObservation(FindTarget.MOUSE.classId, 0.42f, 0.5f, 0.1f))
        var last = machine.update(obs, FindTarget.MOUSE.classId, 0, FindTarget.MOUSE.minConfidence)
        last = machine.update(obs, FindTarget.MOUSE.classId, 100, FindTarget.MOUSE.minConfidence)
        last = machine.update(obs, FindTarget.MOUSE.classId, 200, FindTarget.MOUSE.minConfidence)
        assertEquals(FindEvent.FOUND, last.event)

        // Cùng độ tin cậy 0.42 nhưng với cốc (ngưỡng 0.5) thì không được coi là thấy.
        val cup = FindStateMachine()
        val cupObs = listOf(FindObservation(FindTarget.CUP.classId, 0.42f, 0.5f, 0.1f))
        repeat(3) { cup.update(cupObs, FindTarget.CUP.classId, it * 100L, FindTarget.CUP.minConfidence) }
        assertEquals(FeedbackConfig.FIND_MIN_CONFIDENCE, FindTarget.CUP.minConfidence, 0f)
        assertEquals(FindEvent.NONE, cup.update(cupObs, FindTarget.CUP.classId, 300, FindTarget.CUP.minConfidence).event)
    }
}
