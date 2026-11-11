package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExploreSummarizerTest {

    @Test
    fun groupsTopThreeByDirectionInFixedOrder() {
        val text = ExploreSummarizer.summarize(
            listOf(
                SeenObject("chai", 0.8f, 0.1f),
                SeenObject("ghế", 0.9f, 0.5f),
                SeenObject("cốc", 0.7f, 0.2f),
                SeenObject("sách", 0.6f, 0.9f) // thứ 4 → bỏ
            )
        )
        assertEquals("Phía trước có ghế. Bên trái có chai và cốc.", text)
    }

    @Test
    fun deduplicatesSameLabelAndDropsLowConfidence() {
        val text = ExploreSummarizer.summarize(
            listOf(
                SeenObject("người", 0.9f, 0.8f),
                SeenObject("người", 0.85f, 0.2f),
                SeenObject("chai", 0.3f, 0.5f)
            )
        )
        assertEquals("Bên phải có người.", text)
    }

    @Test
    fun returnsNullWhenNothingConfident() {
        assertNull(ExploreSummarizer.summarize(listOf(SeenObject("cốc", 0.2f, 0.5f))))
        assertNull(ExploreSummarizer.summarize(emptyList()))
    }
}
