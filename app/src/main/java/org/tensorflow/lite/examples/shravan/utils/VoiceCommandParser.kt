package org.tensorflow.lite.examples.shravan.utils

import java.text.Normalizer
import java.util.Locale

/** 5 lệnh giọng nói theo PLAN.md mục 4.4 (+ "giúp đỡ" và "cài đặt" giữ lại từ VisionAid). */
enum class VoiceCommand { READ_TEXT, FIND, PAUSE, RESUME, REPEAT, BACK, HELP, SETTINGS, UNKNOWN }

/**
 * Nhận diện lệnh từ câu nói. So khớp theo từ nguyên vẹn sau khi bỏ dấu,
 * để "dừng" không khớp nhầm "sử dụng" hay "ghế" không khớp "nghe".
 * Bỏ dấu vì SpeechRecognizer đôi khi trả về sai dấu; đổi lại "dừng"/"dùng" và
 * "tìm"/"tím" bị coi là một — chấp nhận được với bộ lệnh ngắn này.
 */
object VoiceCommandParser {

    // Thứ tự quan trọng: cụm dài/cụ thể trước ("đọc lại" trước "đọc chữ" / "đọc").
    private val rules: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.REPEAT to listOf("doc lai", "lap lai", "noi lai", "repeat", "read again"),
        VoiceCommand.BACK to listOf("quay lai", "tro lai", "go back", "back"),
        VoiceCommand.PAUSE to listOf("dung", "tam dung", "im", "stop", "pause"),
        VoiceCommand.RESUME to listOf("tiep", "tiep tuc", "continue", "resume"),
        VoiceCommand.READ_TEXT to listOf("doc chu", "van ban", "doc", "read", "text", "ocr"),
        VoiceCommand.FIND to listOf("tim", "find"),
        VoiceCommand.HELP to listOf("giup", "giup do", "toi dang o dau", "help", "where am i"),
        VoiceCommand.SETTINGS to listOf("cai dat", "settings")
    )

    /** Cụm từ thường gặp sau khi bỏ dấu trùng với từ khóa ("sử dụng" → "dung"). */
    private val ignoredPhrases = listOf("su dung")

    fun parse(text: String): VoiceCommand {
        var words = " ${normalize(text)} "
        ignoredPhrases.forEach { words = words.replace(" $it ", " ") }
        if (words.isBlank()) return VoiceCommand.UNKNOWN
        for ((command, phrases) in rules) {
            if (phrases.any { words.contains(" $it ") }) return command
        }
        return VoiceCommand.UNKNOWN
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
}

/**
 * Đếm lần không nhận ra lệnh liên tiếp (PLAN 4.4):
 * lần 1, 2 → "Không hiểu, hãy nói lại"; lần thứ [limit] → nhắc dùng nút chạm rồi đếm lại.
 */
class VoiceFailureCounter(private val limit: Int = FeedbackConfig.VOICE_MAX_FAILURES) {
    enum class Reaction { ASK_AGAIN, SUGGEST_TOUCH }

    var failures = 0
        private set

    fun onUnrecognized(): Reaction {
        failures++
        return if (failures >= limit) {
            failures = 0
            Reaction.SUGGEST_TOUCH
        } else {
            Reaction.ASK_AGAIN
        }
    }

    fun onRecognized() {
        failures = 0
    }
}
