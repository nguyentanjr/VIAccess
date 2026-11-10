package org.tensorflow.lite.examples.shravan.utils

/** Kết quả xét một frame theo bảng PLAN 4.2. Mỗi lần chỉ một lời nhắc. */
enum class AlignmentAdvice {
    /** Không cần nói gì ở frame này (vd chưa đủ 5 s không thấy chữ). */
    NONE,
    DARK, SHAKY, BLURRY, TOO_CLOSE, OFF_LEFT, OFF_RIGHT, TOO_SMALL, NO_TEXT,
    /** Đạt hết các điều kiện; độ ổn định 3/5 frame do OCRManager kiểm tra. */
    OK
}

data class FrameQuality(
    val luminance: Double,
    /** true khi không thể bật flash (người dùng đặt flash tắt) → phải nhắc "Ánh sáng yếu". */
    val flashUnavailable: Boolean,
    val shaking: Boolean,
    val laplacianVariance: Double,
    val textBox: TextBox?
)

/**
 * Luật căn camera cho OCR, xét lần lượt từ trên xuống (PLAN 4.2):
 * thiếu sáng → rung tay → ảnh mờ → chạm mép → lệch tâm → quá nhỏ → đạt.
 * Không có chữ: nhắc lần đầu sau [FeedbackConfig.NO_TEXT_FIRST_PROMPT_MS], sau đó mỗi
 * [FeedbackConfig.NO_TEXT_REPEAT_MS].
 */
class AlignmentAdvisor {
    private var noTextSinceMs: Long? = null
    private var lastNoTextPromptMs: Long? = null

    fun reset() {
        noTextSinceMs = null
        lastNoTextPromptMs = null
    }

    fun evaluate(q: FrameQuality, nowMs: Long): AlignmentAdvice {
        if (q.flashUnavailable && q.luminance < FeedbackConfig.DARK_LUMINANCE_ON) return AlignmentAdvice.DARK
        if (q.shaking) return AlignmentAdvice.SHAKY
        if (q.laplacianVariance < FeedbackConfig.BLUR_LAPLACIAN_MIN) return AlignmentAdvice.BLURRY

        val box = q.textBox
        if (box == null) return noText(nowMs)
        noTextSinceMs = null
        lastNoTextPromptMs = null

        return when {
            box.edgeMargin < FeedbackConfig.TEXT_EDGE_MARGIN -> AlignmentAdvice.TOO_CLOSE
            box.centerX < FeedbackConfig.TEXT_CENTER_MIN_X -> AlignmentAdvice.OFF_LEFT
            box.centerX > FeedbackConfig.TEXT_CENTER_MAX_X -> AlignmentAdvice.OFF_RIGHT
            box.area < FeedbackConfig.TEXT_MIN_AREA -> AlignmentAdvice.TOO_SMALL
            else -> AlignmentAdvice.OK
        }
    }

    private fun noText(nowMs: Long): AlignmentAdvice {
        val since = noTextSinceMs ?: nowMs.also { noTextSinceMs = it }
        val lastPrompt = lastNoTextPromptMs
        val due = if (lastPrompt == null) {
            nowMs - since >= FeedbackConfig.NO_TEXT_FIRST_PROMPT_MS
        } else {
            nowMs - lastPrompt >= FeedbackConfig.NO_TEXT_REPEAT_MS
        }
        if (!due) return AlignmentAdvice.NONE
        lastNoTextPromptMs = nowMs
        return AlignmentAdvice.NO_TEXT
    }
}
