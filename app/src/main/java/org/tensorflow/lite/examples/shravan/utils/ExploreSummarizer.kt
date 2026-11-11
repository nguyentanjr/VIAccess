package org.tensorflow.lite.examples.shravan.utils

/** Một vật YOLO thấy trong lần chụp "Khám phá". [centerX] chuẩn hoá 0..1. */
data class SeenObject(val label: String, val confidence: Float, val centerX: Float)

/**
 * Khám phá xung quanh theo yêu cầu (đề xuất đề tài: "nhận diện các vật thể phổ biến trong khung hình").
 * Khác Explore của VisionAid (đọc liên tục): chỉ đọc MỘT lần, tối đa [FeedbackConfig.EXPLORE_MAX_OBJECTS]
 * loại vật rõ nhất, gom theo hướng.
 */
object ExploreSummarizer {

    /** @return câu để đọc, hoặc null nếu không thấy gì đủ chắc chắn. */
    fun summarize(objects: List<SeenObject>): String? {
        val picked = objects
            .filter { it.confidence >= FeedbackConfig.EXPLORE_MIN_CONFIDENCE }
            .groupBy { it.label }
            .map { (_, same) -> same.maxBy { it.confidence } }
            .sortedByDescending { it.confidence }
            .take(FeedbackConfig.EXPLORE_MAX_OBJECTS)
        if (picked.isEmpty()) return null

        val byDir = linkedMapOf<String, MutableList<String>>()
        // Thứ tự đọc cố định: phía trước, bên trái, bên phải.
        listOf("Phía trước", "Bên trái", "Bên phải").forEach { byDir[it] = mutableListOf() }
        picked.forEach { o ->
            val dir = when {
                o.centerX < FeedbackConfig.FIND_LEFT_MAX_X -> "Bên trái"
                o.centerX > FeedbackConfig.FIND_RIGHT_MIN_X -> "Bên phải"
                else -> "Phía trước"
            }
            byDir.getValue(dir) += o.label
        }
        return byDir.filterValues { it.isNotEmpty() }
            .map { (dir, labels) -> "$dir có ${joinVi(labels)}." }
            .joinToString(" ")
    }

    private fun joinVi(items: List<String>): String = when (items.size) {
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " và " + items.last()
    }
}
