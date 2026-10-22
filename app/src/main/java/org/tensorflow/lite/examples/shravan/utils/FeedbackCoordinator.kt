package org.tensorflow.lite.examples.shravan.utils

/** Mức ưu tiên của lời nói (PLAN 4.1). Cao hơn thì ngắt lời thấp hơn. */
enum class Priority { INFO, GUIDANCE, STATE, ALERT }

enum class HapticPattern { SHORT, DEFAULT, LONG, DOUBLE }

interface SpeechOutput {
    fun speak(text: String, isVietnamese: Boolean, onDone: (() -> Unit)?)
    fun isSpeaking(): Boolean
    fun stop()
}

interface HapticOutput {
    fun vibrate(pattern: HapticPattern)
}

/**
 * Điểm duy nhất để phát lời nói và rung, chống quá tải theo PLAN.md mục 4.1:
 * - Lời mới ưu tiên cao hơn lời đang đọc thì ngắt; thấp hơn hoặc bằng thì bỏ.
 * - Cùng một key không lặp lại trong [FeedbackConfig.REPEAT_COOLDOWN_MS].
 * - GUIDANCE tối đa [FeedbackConfig.MAX_GUIDANCE_PER_MIN] lần mỗi phút.
 * - Không rung khi đang nói, trừ khi [vibrate] được gọi với force = true.
 */
class FeedbackCoordinator(
    private val speech: SpeechOutput,
    private val haptics: HapticOutput,
    private val defaultVietnamese: () -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var currentPriority: Priority? = null
    private val lastSaidAt = mutableMapOf<String, Long>()
    private val guidanceTimes = ArrayDeque<Long>()

    /**
     * @param key dùng để chống lặp; mặc định là chính nội dung.
     * @param queue đọc nối tiếp sau lời hiện tại thay vì bị bỏ (vd lời chào + hướng dẫn).
     * @param userInitiated người dùng vừa yêu cầu (chạm, ra lệnh): luôn đọc ngay, bỏ qua cooldown.
     * @return true nếu lời được phát (hoặc xếp hàng), false nếu bị bỏ.
     */
    fun say(
        text: String,
        priority: Priority,
        key: String = text,
        isVietnamese: Boolean? = null,
        queue: Boolean = false,
        userInitiated: Boolean = false,
        onDone: (() -> Unit)? = null
    ): Boolean {
        if (text.isBlank()) {
            onDone?.invoke()
            return false
        }
        val now = clock()
        if (!userInitiated && !isAllowed(key, priority, now, queue)) {
            onDone?.invoke()
            return false
        }

        lastSaidAt[key] = now
        if (priority == Priority.GUIDANCE) guidanceTimes.addLast(now)
        val vi = isVietnamese ?: defaultVietnamese()

        if (queue) {
            pending.addLast(Utterance(text, vi, priority, onDone))
            if (!draining && currentPriority == null) drainNext()
        } else {
            // Ngắt lời đang đọc: bỏ luôn các lời đang xếp hàng.
            pending.clear()
            draining = false
            currentPriority = priority
            speech.speak(text, vi) {
                currentPriority = null
                onDone?.invoke()
                if (pending.isNotEmpty() && !draining) drainNext()
            }
        }
        return true
    }

    private fun isAllowed(key: String, priority: Priority, now: Long, queue: Boolean): Boolean {
        lastSaidAt[key]?.let { if (now - it < FeedbackConfig.REPEAT_COOLDOWN_MS) return false }

        if (priority == Priority.GUIDANCE) {
            while (guidanceTimes.isNotEmpty() && now - guidanceTimes.first() >= FeedbackConfig.GUIDANCE_WINDOW_MS) {
                guidanceTimes.removeFirst()
            }
            if (guidanceTimes.size >= FeedbackConfig.MAX_GUIDANCE_PER_MIN) return false
        }

        if (queue) return true
        val playing = currentPriority
        return !speech.isSpeaking() || playing == null || priority > playing
    }

    private data class Utterance(val text: String, val vi: Boolean, val priority: Priority, val onDone: (() -> Unit)?)

    /** Hàng đợi riêng cho queue = true, vì SpeechOutput chỉ có một callback hoàn thành. */
    private val pending = ArrayDeque<Utterance>()
    private var draining = false

    private fun drainNext() {
        val next = pending.removeFirstOrNull()
        if (next == null) {
            draining = false
            currentPriority = null
            return
        }
        draining = true
        currentPriority = next.priority
        speech.speak(next.text, next.vi) {
            next.onDone?.invoke()
            drainNext()
        }
    }

    /** Dừng mọi lời nói (vd khi rời màn hình hoặc app chạy nền). */
    fun stopSpeech() {
        pending.clear()
        draining = false
        currentPriority = null
        speech.stop()
    }

    /** @param force rung kể cả khi đang nói (dùng cho xác nhận hành động). */
    fun vibrate(pattern: HapticPattern = HapticPattern.DEFAULT, force: Boolean = false): Boolean {
        if (!force && speech.isSpeaking()) return false
        haptics.vibrate(pattern)
        return true
    }

    /** true khi đang nói (kể cả khoảng đệm sau TTS) — nhận lệnh giọng nói nên chờ. */
    fun isBusy(): Boolean = speech.isSpeaking()
}
