package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindTargetTest {
    @Test
    fun parsesVietnameseAndEnglishTargetCommands() {
        assertEquals(FindTarget.CUP, FindTarget.fromCommand("Tìm cốc"))
        assertEquals(FindTarget.PHONE, FindTarget.fromCommand("tim dien thoai"))
        assertEquals(FindTarget.REMOTE, FindTarget.fromCommand("find remote"))
        assertEquals(FindTarget.CHAIR, FindTarget.fromCommand("tìm cái ghế"))
    }

    @Test
    fun rejectsCommandsWithoutFindIntentOrKnownTarget() {
        assertNull(FindTarget.fromCommand("cốc"))
        assertNull(FindTarget.fromCommand("tìm ví"))
    }

    @Test
    fun parsesExtendedTargetsAndPrefersLongestAlias() {
        assertEquals(FindTarget.BACKPACK, FindTarget.fromCommand("tìm ba lô"))
        assertEquals(FindTarget.KEYBOARD, FindTarget.fromCommand("tìm bàn phím"))
        assertEquals(FindTarget.TOOTHBRUSH, FindTarget.fromCommand("tìm bàn chải đánh răng"))
        assertEquals(FindTarget.HANDBAG, FindTarget.fromCommand("tìm túi xách"))
        assertEquals(FindTarget.LAPTOP, FindTarget.fromCommand("tìm máy tính"))
        assertEquals(FindTarget.UMBRELLA, FindTarget.fromCommand("tìm cái ô"))
        assertEquals(FindTarget.BOWL, FindTarget.fromCommand("tìm cái chén"))
        // "ở" không được hiểu nhầm thành "ô".
        assertEquals(FindTarget.CUP, FindTarget.fromCommand("tìm cốc ở đâu"))
    }

    @Test
    fun everyTargetMatchesItsVietnameseCocoLabel() {
        val labels = java.io.File("src/main/assets/coco_vi.txt").readLines(Charsets.UTF_8)
        FindTarget.entries.forEach { t ->
            assertEquals("${t.name} classId", t.vietnameseLabel, labels[t.classId].trim())
        }
    }

    @Test
    fun matchesWholeWordsOnly() {
        // "nghe" chứa "ghe" nhưng không phải "ghế"; "time" chứa "tim" nhưng không phải lệnh tìm.
        assertNull(FindTarget.fromCommand("tìm nhạc để nghe"))
        assertNull(FindTarget.fromCommand("what time is it phone"))
        assertEquals(FindTarget.CUP, FindTarget.fromCommand("tìm cái ly"))
    }
}
