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
            val normalized = normalize(command)
            if (!normalized.contains("tim") && !normalized.contains("find")) return null

            return entries.firstOrNull { target ->
                target.aliases.any { alias -> normalize(alias).let(normalized::contains) }
            }
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}
