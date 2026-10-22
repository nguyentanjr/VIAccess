package org.tensorflow.lite.examples.shravan.utils

/**
 * Mọi ngưỡng và thời gian của app (PLAN.md mục 4).
 * Hiệu chỉnh ở đây, không viết cứng trong code. Giá trị cuối ghi lại vào PLAN.md mục 4.5.
 */
object FeedbackConfig {

    // --- Bộ điều phối phản hồi (PLAN 4.1) ---
    /** Cùng một nội dung (key) không lặp lại trong khoảng này. */
    const val REPEAT_COOLDOWN_MS = 3000L
    /** Số lời nhắc GUIDANCE tối đa trong 1 phút. */
    const val MAX_GUIDANCE_PER_MIN = 12
    const val GUIDANCE_WINDOW_MS = 60_000L
    /** Sau khi TTS dừng, chờ thêm khoảng này trước khi coi là rảnh (tránh mic nghe lại TTS). */
    const val TTS_TAIL_BUFFER_MS = 500L

    // --- Camera / độ sáng (PLAN 4.2, ưu tiên 1) ---
    const val DARK_LUMINANCE_ON = 40.0
    const val DARK_LUMINANCE_OFF = 120.0

    // --- Căn camera cho OCR (PLAN 4.2) ---
    const val GYRO_SHAKE_RAD_S = 0.5f
    const val GYRO_SHAKE_WINDOW_MS = 500L
    const val BLUR_LAPLACIAN_MIN = 100.0
    /** Khung chữ cách mép ảnh nhỏ hơn tỷ lệ này → "Đưa máy ra xa". */
    const val TEXT_EDGE_MARGIN = 0.05f
    const val TEXT_CENTER_MIN_X = 0.3f
    const val TEXT_CENTER_MAX_X = 0.7f
    /** Khung chữ chiếm ít hơn tỷ lệ diện tích ảnh này → "Lại gần". */
    const val TEXT_MIN_AREA = 0.10f
    const val NO_TEXT_FIRST_PROMPT_MS = 5000L
    const val NO_TEXT_REPEAT_MS = 10_000L

    // --- Ổn định OCR (kế thừa OCRManager) ---
    const val OCR_MIN_TEXT_LENGTH = 3
    const val OCR_STABLE_BUFFER = 5
    const val OCR_STABLE_MATCHES = 3
    /** Hai lần đọc giống nhau ≥ (1 - tỷ lệ này) thì coi là cùng một văn bản. */
    const val OCR_STABLE_MAX_DIFF = 0.2
    const val OCR_REPEAT_MAX_DIFF = 0.4
    const val OCR_REPEAT_COOLDOWN_MS = 10_000L

    // --- Nhận diện vật thể (kế thừa YoloAnalyzer) ---
    const val YOLO_FRAME_SKIP = 5
    const val YOLO_MIN_CONFIDENCE = 0.25f
    const val YOLO_PERSISTENCE_FRAMES = 5
    const val YOLO_DEBOUNCE_TOTALLY_BLIND_MS = 1000L
    const val YOLO_DEBOUNCE_PARTIAL_MS = 2000L
    const val YOLO_SEEN_CACHE_MS = 5000L

    // --- Tìm vật thể (PLAN 4.3) ---
    const val FIND_CONFIRM_FRAMES = 3
    const val FIND_MIN_CONFIDENCE = 0.5f
    const val FIND_LOST_MS = 1500L
    const val FIND_CENTERED_MS = 1000L
    const val FIND_DIRECTION_COOLDOWN_MS = 2000L
    const val FIND_LEFT_MAX_X = 0.35f
    const val FIND_RIGHT_MIN_X = 0.65f
    const val FIND_SEARCH_PULSE_MS = 2000L
    const val HAPTIC_INTERVAL_FAR_MS = 1000L
    const val HAPTIC_INTERVAL_NEAR_MS = 250L

    // --- Rung ---
    const val HAPTIC_SHORT_MS = 50L
    const val HAPTIC_DEFAULT_MS = 100L
    const val HAPTIC_LONG_MS = 300L
}
