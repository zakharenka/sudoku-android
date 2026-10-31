package me.zakharenka.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsTimeWithAndWithoutHours() {
        assertEquals("00:00", formatTime(0))
        assertEquals("01:05", formatTime(65))
        assertEquals("1:00:00", formatTime(3600))
        assertEquals("2:03:04", formatTime(7384))
    }

}
