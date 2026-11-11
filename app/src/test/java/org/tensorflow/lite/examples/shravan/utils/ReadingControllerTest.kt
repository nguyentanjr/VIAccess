package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadingControllerTest {

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
        // TTS thật không gọi callback của lời bị ngắt khi đã có lời mới; ở đây bỏ callback khi stop.
        override fun stop() { speaking = false; onDone = null }
        fun finish() {
            speaking = false
            val cb = onDone
            onDone = null
            cb?.invoke()
        }
    }

    private lateinit var speech: FakeSpeech
    private lateinit var fc: FeedbackCoordinator
    private lateinit var reader: ReadingController

    @Before
    fun setUp() {
        speech = FakeSpeech()
        fc = FeedbackCoordinator(speech, object : HapticOutput {
            override fun vibrate(pattern: HapticPattern) {}
        }, defaultVietnamese = { true }, clock = { 0L })
        reader = ReadingController(fc)
    }

    @Test
    fun splitsIntoSentencesAndLines() {
        assertEquals(
            listOf("Uống 2 viên mỗi ngày.", "Sau bữa ăn!", "Tổng tiền: 120.000đ"),
            ReadingController.splitSentences("Uống 2 viên mỗi ngày. Sau bữa ăn!\nTổng tiền: 120.000đ")
        )
        // Mảnh quá ngắn như "1." được gộp vào câu sau.
        assertEquals(listOf("1. Lắc đều trước khi dùng."), ReadingController.splitSentences("1.\nLắc đều trước khi dùng."))
    }

    @Test
    fun readsSentenceBySentence() {
        reader.start("Câu một. Câu hai. Câu ba.")
        assertEquals(listOf("Câu một."), speech.spoken)
        speech.finish()
        speech.finish()
        assertEquals(listOf("Câu một.", "Câu hai.", "Câu ba."), speech.spoken)
        speech.finish()
        assertFalse(reader.isReading)
    }

    @Test
    fun pauseKeepsCurrentSentenceAndResumeContinuesFromIt() {
        reader.start("Câu một. Câu hai. Câu ba.")
        speech.finish() // xong câu một, đang đọc câu hai
        assertTrue(fc.pauseSpeech())
        assertTrue(fc.hasPausedSpeech())
        assertTrue(fc.resumeSpeech())
        assertEquals(listOf("Câu một.", "Câu hai.", "Câu hai."), speech.spoken)
        speech.finish()
        assertEquals("Câu ba.", speech.spoken.last())
    }

    @Test
    fun repeatReadsCurrentSentenceAgainAndAfterEndReadsLast() {
        reader.start("Câu một. Câu hai.")
        assertTrue(fc.repeatLast())
        assertEquals(listOf("Câu một.", "Câu một."), speech.spoken)
        speech.finish()
        speech.finish()
        assertFalse(reader.isReading)
        assertTrue(reader.repeat())
        assertEquals("Câu hai.", speech.spoken.last())
    }

    @Test
    fun nextAndPreviousMoveBetweenSentences() {
        reader.start("Một. Hai. Ba.")
        assertTrue(reader.next())
        assertEquals("Hai.", speech.spoken.last())
        assertTrue(reader.previous())
        assertEquals("Một.", speech.spoken.last())
    }
}
