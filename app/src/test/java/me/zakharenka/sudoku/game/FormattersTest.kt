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

    @Test
    fun pluralizesMistakes() {
        assertEquals("ошибка", pluralMistakes(1))
        assertEquals("ошибки", pluralMistakes(2))
        assertEquals("ошибок", pluralMistakes(5))
        assertEquals("ошибок", pluralMistakes(11))
        assertEquals("ошибок", pluralMistakes(14))
        assertEquals("ошибка", pluralMistakes(21))
        assertEquals("ошибки", pluralMistakes(22))
    }
}
