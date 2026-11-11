package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {

    private fun cmd(text: String) = VoiceCommandParser.parse(text)

    @Test
    fun fiveCommandsInVietnamese() {
        assertEquals(VoiceCommand.READ_TEXT, cmd("đọc chữ"))
        assertEquals(VoiceCommand.READ_TEXT, cmd("Văn bản"))
        assertEquals(VoiceCommand.FIND, cmd("tìm cốc"))
        assertEquals(VoiceCommand.FIND, cmd("Tìm điện thoại giúp tôi"))
        assertEquals(VoiceCommand.PAUSE, cmd("dừng"))
        assertEquals(VoiceCommand.PAUSE, cmd("tạm dừng lại"))
        assertEquals(VoiceCommand.RESUME, cmd("tiếp"))
        assertEquals(VoiceCommand.RESUME, cmd("đọc tiếp tục"))
        assertEquals(VoiceCommand.REPEAT, cmd("đọc lại"))
        assertEquals(VoiceCommand.REPEAT, cmd("lặp lại đi"))
        assertEquals(VoiceCommand.BACK, cmd("quay lại"))
    }

    @Test
    fun sentenceNavigationAndExplore() {
        assertEquals(VoiceCommand.NEXT_SENTENCE, cmd("câu tiếp"))
        assertEquals(VoiceCommand.NEXT_SENTENCE, cmd("đọc câu tiếp theo"))
        assertEquals(VoiceCommand.PREVIOUS_SENTENCE, cmd("câu trước"))
        assertEquals(VoiceCommand.RESUME, cmd("tiếp"))
        assertEquals(VoiceCommand.EXPLORE, cmd("xung quanh có gì"))
        assertEquals(VoiceCommand.EXPLORE, cmd("khám phá"))
        assertEquals(VoiceCommand.EXPLORE, cmd("trước mặt có gì"))
    }

    @Test
    fun specificPhrasesWinOverShorterOnes() {
        // "đọc lại" chứa "đọc" nhưng phải là đọc lại, không phải vào màn đọc chữ.
        assertEquals(VoiceCommand.REPEAT, cmd("đọc lại"))
        // "quay lại" không bị hiểu thành "đọc lại".
        assertEquals(VoiceCommand.BACK, cmd("quay lại màn chính"))
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertEquals(VoiceCommand.UNKNOWN, cmd("cách sử dụng"))
        assertEquals(VoiceCommand.UNKNOWN, cmd("nghe nhạc"))
        assertEquals(VoiceCommand.UNKNOWN, cmd("timeout"))
        assertEquals(VoiceCommand.UNKNOWN, cmd(""))
        assertEquals(VoiceCommand.UNKNOWN, cmd("xin chào"))
    }

    @Test
    fun englishFallback() {
        assertEquals(VoiceCommand.FIND, cmd("find the cup"))
        assertEquals(VoiceCommand.HELP, cmd("help"))
        assertEquals(VoiceCommand.PAUSE, cmd("stop"))
    }

    @Test
    fun failureCounterSuggestsTouchOnThirdMiss() {
        val counter = VoiceFailureCounter(limit = 3)
        assertEquals(VoiceFailureCounter.Reaction.ASK_AGAIN, counter.onUnrecognized())
        assertEquals(VoiceFailureCounter.Reaction.ASK_AGAIN, counter.onUnrecognized())
        assertEquals(VoiceFailureCounter.Reaction.SUGGEST_TOUCH, counter.onUnrecognized())
        assertEquals(VoiceFailureCounter.Reaction.ASK_AGAIN, counter.onUnrecognized())
        counter.onRecognized()
        assertEquals(0, counter.failures)
    }
}
