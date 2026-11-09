package me.zakharenka.sudoku.ui.sketch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwrittenDigitsTest {

    @Test
    fun everyDigitHasValidStrokes() {
        assertEquals((1..9).toSet(), HandwrittenDigitStrokes.keys)

        HandwrittenDigitStrokes.forEach { (digit, strokes) ->
            assertTrue("Digit $digit has no strokes", strokes.isNotEmpty())

            strokes.forEachIndexed { index, stroke ->
                assertTrue(
                    "Digit $digit stroke $index has fewer than two points",
                    stroke.size >= 2,
                )
                stroke.forEach { point ->
                    assertTrue(
                        "Digit $digit stroke $index has an out-of-range point",
                        point.x in 0f..1f && point.y in 0f..1f,
                    )
                }
            }
        }
    }
}
