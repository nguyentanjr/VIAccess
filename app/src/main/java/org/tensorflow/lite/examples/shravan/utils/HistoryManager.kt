package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import android.content.SharedPreferences

data class HistoryItem(val type: String, val content: String, val timestamp: Long = System.currentTimeMillis())

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shravan_history", Context.MODE_PRIVATE)
    private val MAX_HISTORY = 50

    fun addHistory(type: String, content: String) {
        val history = getHistory().toMutableList()
        history.add(0, HistoryItem(type, content))
        if (history.size > MAX_HISTORY) {
            history.removeAt(history.size - 1)
        }
        saveHistory(history)
    }

    fun getHistory(): List<HistoryItem> {
        val raw = prefs.getString("history_data", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|ITEM|").mapNotNull {
            val parts = it.split("|PART|")
            if (parts.size >= 3) {
                try {
                    HistoryItem(parts[0], parts[1], parts[2].toLong())
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    private fun saveHistory(history: List<HistoryItem>) {
        val raw = history.joinToString("|ITEM|") { "${it.type}|PART|${it.content}|PART|${it.timestamp}" }
        prefs.edit().putString("history_data", raw).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history_data").apply()
    }
}
