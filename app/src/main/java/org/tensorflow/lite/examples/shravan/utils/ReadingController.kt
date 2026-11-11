package org.tensorflow.lite.examples.shravan.utils

/**
 * Đọc văn bản dài theo từng câu (PLAN 4.4): dừng / tiếp / đọc lại / câu trước / câu sau.
 * Mỗi câu đi qua [FeedbackCoordinator] với Priority.STATE; đọc xong câu này mới sang câu kế.
 *
 * Khi đang đọc, Coordinator chuyển lệnh "dừng" / "tiếp" / "đọc lại" về đây (xem [FeedbackCoordinator.reader]),
 * nên "dừng" giữ đúng câu đang đọc thay vì cả đoạn.
 */
class ReadingController(private val feedback: FeedbackCoordinator) : ReaderControl {

    var sentences: List<String> = emptyList()
        private set
    var index = 0
        private set
    /** Đang có văn bản và chưa đọc hết (kể cả khi đang dừng). */
    override val isReading: Boolean get() = sentences.isNotEmpty() && index < sentences.size
    override var isPaused = false
        private set
    /** Tăng mỗi lần bắt đầu / dừng, để callback của câu cũ không đẩy sang câu kế. */
    private var generation = 0

    fun start(text: String, isVietnamese: Boolean? = null) {
        sentences = splitSentences(text)
        index = 0
        isPaused = false
        vi = isVietnamese
        feedback.reader = this
        speakCurrent()
    }

    private var vi: Boolean? = null

    private fun speakCurrent() {
        if (!isReading) {
            finish()
            return
        }
        val gen = ++generation
        val sentence = sentences[index]
        feedback.say(sentence, Priority.STATE, key = "read:$index:$sentence", isVietnamese = vi, userInitiated = true) {
            if (gen != generation || isPaused) return@say
            index++
            if (isReading) speakCurrent() else finish()
        }
    }

    override fun pause(): Boolean {
        if (!isReading || isPaused) return false
        isPaused = true
        generation++
        feedback.stopSpeech()
        return true
    }

    override fun resume(): Boolean {
        if (!isReading || !isPaused) return false
        isPaused = false
        speakCurrent()
        return true
    }

    /** Đọc lại câu hiện tại (hoặc câu cuối nếu đã đọc xong). */
    override fun repeat(): Boolean {
        if (sentences.isEmpty()) return false
        if (index >= sentences.size) index = sentences.size - 1
        isPaused = false
        feedback.reader = this
        generation++
        speakCurrent()
        return true
    }

    override fun next(): Boolean {
        if (index + 1 >= sentences.size) return false
        index++
        isPaused = false
        generation++
        speakCurrent()
        return true
    }

    override fun previous(): Boolean {
        if (sentences.isEmpty()) return false
        index = (index - 1).coerceAtLeast(0)
        isPaused = false
        generation++
        speakCurrent()
        return true
    }

    fun stop() {
        generation++
        isPaused = false
        index = sentences.size
        finish()
        feedback.stopSpeech()
    }

    private fun finish() {
        // Giữ lại sentences để "đọc lại" câu cuối; chỉ nhả quyền điều khiển khi không còn gì đang dừng.
        if (!isPaused && feedback.reader === this && index >= sentences.size) {
            feedback.readerFinished(this)
        }
    }

    companion object {
        /** Tách theo dấu kết câu và xuống dòng; gộp mảnh quá ngắn vào câu trước (vd "1." trong danh sách). */
        fun splitSentences(text: String): List<String> {
            val raw = text
                .replace("\r", "")
                .split(Regex("(?<=[.!?…])\\s+|\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val out = mutableListOf<String>()
            for (part in raw) {
                if (out.isNotEmpty() && part.length < FeedbackConfig.READING_MIN_SENTENCE_CHARS) {
                    out[out.lastIndex] = out.last() + " " + part
                } else if (out.isNotEmpty() && out.last().length < FeedbackConfig.READING_MIN_SENTENCE_CHARS) {
                    out[out.lastIndex] = out.last() + " " + part
                } else {
                    out += part
                }
            }
            return out
        }
    }
}

/** Phần điều khiển mà Coordinator chuyển lệnh dừng / tiếp / đọc lại sang. */
interface ReaderControl {
    val isReading: Boolean
    val isPaused: Boolean
    fun pause(): Boolean
    fun resume(): Boolean
    fun repeat(): Boolean
    fun next(): Boolean
    fun previous(): Boolean
}
