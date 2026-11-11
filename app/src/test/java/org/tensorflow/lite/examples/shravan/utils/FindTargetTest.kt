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
    fun matchesWholeWordsOnly() {
        // "nghe" chứa "ghe" nhưng không phải "ghế"; "time" chứa "tim" nhưng không phải lệnh tìm.
        assertNull(FindTarget.fromCommand("tìm nhạc để nghe"))
        assertNull(FindTarget.fromCommand("what time is it phone"))
        assertEquals(FindTarget.CUP, FindTarget.fromCommand("tìm cái ly"))
    }
}
