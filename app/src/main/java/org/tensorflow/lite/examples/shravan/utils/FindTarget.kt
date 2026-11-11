package org.tensorflow.lite.examples.shravan.utils

import java.text.Normalizer
import java.util.Locale

/**
 * Vật có thể tìm (đều là lớp COCO mà YOLOv5s đã nhận diện được, không cần huấn luyện lại).
 * - 6 vật gốc theo PLAN 1.2 (dùng cho bộ đánh giá 150 ảnh).
 * - 11 vật mở rộng (PLAN mục 11, 2026-11-11): đồ dùng hằng ngày người khiếm thị hay cần tìm.
 *
 * [vietnameseLabel] phải trùng đúng dòng tương ứng trong assets/coco_vi.txt (dùng để lọc kết quả YOLO).
 * Thứ tự khai báo là thứ tự ưu tiên khi so khớp lệnh và thứ tự nút trên màn hình.
 */
enum class FindTarget(
    val classId: Int,
    val englishLabel: String,
    val vietnameseLabel: String,
    val displayName: String,
    val aliases: List<String>,
    val isCore: Boolean = false,
    /** Ngưỡng tin cậy để coi là thấy vật; vật khó dùng ngưỡng thấp hơn. */
    val minConfidence: Float = FeedbackConfig.FIND_MIN_CONFIDENCE
) {
    CUP(41, "cup", "cốc", "Cốc", listOf("cup", "cốc", "ly", "cái ly"), isCore = true),
    BOTTLE(39, "bottle", "chai", "Chai", listOf("bottle", "chai", "chai nước"), isCore = true, minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD),
    PHONE(67, "cell phone", "điện thoại di động", "Điện thoại", listOf("cell phone", "phone", "điện thoại"), isCore = true),
    REMOTE(65, "remote", "điều khiển từ xa", "Điều khiển", listOf("remote", "điều khiển"), isCore = true, minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD),
    BOOK(73, "book", "sách", "Sách", listOf("book", "sách", "quyển sách", "cuốn sách"), isCore = true),
    CHAIR(56, "chair", "ghế", "Ghế", listOf("chair", "ghế", "cái ghế"), isCore = true),

    BACKPACK(24, "backpack", "ba lô", "Ba lô", listOf("backpack", "ba lô", "balo", "cặp")),
    HANDBAG(26, "handbag", "túi xách", "Túi xách", listOf("handbag", "túi xách", "túi")),
    UMBRELLA(25, "umbrella", "ô", "Ô (dù)", listOf("umbrella", "cái ô", "chiếc ô", "dù", "cái dù")),
    LAPTOP(63, "laptop", "máy tính xách tay", "Laptop", listOf("laptop", "máy tính", "máy tính xách tay")),
    MOUSE(64, "mouse", "chuột", "Chuột", listOf("mouse", "chuột", "chuột máy tính"), minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD),
    KEYBOARD(66, "keyboard", "bàn phím", "Bàn phím", listOf("keyboard", "bàn phím")),
    CLOCK(74, "clock", "đồng hồ", "Đồng hồ", listOf("clock", "đồng hồ")),
    SCISSORS(76, "scissors", "kéo", "Kéo", listOf("scissors", "kéo", "cái kéo"), minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD),
    SPOON(44, "spoon", "thìa", "Thìa", listOf("spoon", "thìa", "muỗng"), minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD),
    BOWL(45, "bowl", "bát", "Bát", listOf("bowl", "bát", "chén", "cái bát")),
    TOOTHBRUSH(79, "toothbrush", "bàn chải đánh răng", "Bàn chải", listOf("toothbrush", "bàn chải", "bàn chải đánh răng"), minConfidence = FeedbackConfig.FIND_MIN_CONFIDENCE_HARD);

    companion object {
        fun fromCommand(command: String): FindTarget? {
            // So theo từ nguyên vẹn: "nghe" không khớp "ghe", "time" không khớp "tim".
            val words = " ${normalize(command)} "
            if (!words.contains(" tim ") && !words.contains(" find ")) return null

            // Ưu tiên cụm dài nhất ("bàn chải" trước "bàn"...; "máy tính xách tay" trước "máy tính").
            return entries
                .flatMap { target -> target.aliases.map { target to normalize(it) } }
                .filter { (_, alias) -> words.contains(" $alias ") }
                .maxByOrNull { (_, alias) -> alias.length }
                ?.first
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
