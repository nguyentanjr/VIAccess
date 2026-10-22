package org.tensorflow.lite.examples.shravan.utils

import android.content.Context
import java.util.LinkedList

class OCRManager(context: Context) {
    private val spokenTextTimes = mutableMapOf<String, Long>()
    private val DEBOUNCE_MS = 10000L // 10 seconds
    
    private val historyBuffer = LinkedList<String>()
    private val BUFFER_SIZE = 5

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0)
        var newcost = IntArray(len0)
        for (i in 0 until len0) cost[i] = i
        for (j in 1 until len1) {
            newcost[0] = j
            for (i in 1 until len0) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newcost[i - 1] + 1
                newcost[i] = Math.min(Math.min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newcost
            newcost = swap
        }
        return cost[len0 - 1]
    }

    fun shouldSpeak(text: String): Boolean {
        val normalized = text.lowercase().trim()
        if (normalized.length <= 3) return false
        
        val currentTime = System.currentTimeMillis()
        
        // Clear out expired cache to prevent memory bloat
        spokenTextTimes.entries.removeIf { currentTime - it.value > DEBOUNCE_MS }
        
        // 1. Check if it's too similar to a recently spoken text
        for ((spokenText, time) in spokenTextTimes) {
            val dist = levenshtein(normalized, spokenText)
            val maxLength = Math.max(normalized.length, spokenText.length)
            // If similarity is > 60%, suppress it (dist <= 40% of length)
            if (dist <= maxLength * 0.4) {
                // Update the timer to keep debouncing while they hold it there
                spokenTextTimes[spokenText] = currentTime
                return false
            }
        }
        
        // 2. Add to history buffer and wait for stability
        historyBuffer.add(normalized)
        if (historyBuffer.size > BUFFER_SIZE) {
            historyBuffer.removeFirst()
        }
        
        var matches = 0
        for (buffered in historyBuffer) {
            val dist = levenshtein(normalized, buffered)
            val maxLength = Math.max(normalized.length, buffered.length)
            // 80% similar to count as the "same" word for stability check
            if (dist <= maxLength * 0.2) { 
                matches++
            }
        }
        
        if (matches >= 3) {
            spokenTextTimes[normalized] = currentTime
            historyBuffer.clear()
            return true
        }
        
        return false
    }

    fun clear() {
        spokenTextTimes.clear()
        historyBuffer.clear()
    }
}
