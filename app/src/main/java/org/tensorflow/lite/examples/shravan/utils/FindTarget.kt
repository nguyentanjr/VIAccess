package org.tensorflow.lite.examples.shravan.utils

import java.text.Normalizer
import java.util.Locale

enum class FindTarget(
    val classId: Int,
    val englishLabel: String,
    val vietnameseLabel: String,
    val aliases: List<String>
) {
    CUP(41, "cup", "cốc", listOf("cup", "cốc", "ly")),
    BOTTLE(39, "bottle", "chai", listOf("bottle", "chai")),
    PHONE(67, "cell phone", "điện thoại di động", listOf("cell phone", "phone", "điện thoại")),
    REMOTE(65, "remote", "điều khiển từ xa", listOf("remote", "điều khiển")),
    BOOK(73, "book", "sách", listOf("book", "sách")),
    CHAIR(56, "chair", "ghế", listOf("chair", "ghế"));

    companion object {
        fun fromCommand(command: String): FindTarget? {
            // So theo từ nguyên vẹn: "nghe" không khớp "ghe", "lý do" không khớp "ly"... vẫn khớp, chấp nhận.
            val words = " ${normalize(command)} "
            if (!words.contains(" tim ") && !words.contains(" find ")) return null

            return entries.firstOrNull { target ->
                target.aliases.any { alias -> words.contains(" ${normalize(alias)} ") }
            }
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replace("[^a-z0-9 ]".toRegex(), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}
