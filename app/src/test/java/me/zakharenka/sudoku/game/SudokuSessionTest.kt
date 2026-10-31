package me.zakharenka.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuSessionTest {

    private val solution = intArrayOf(
        5, 3, 4, 6, 7, 8, 9, 1, 2,
        6, 7, 2, 1, 9, 5, 3, 4, 8,
        1, 9, 8, 3, 4, 2, 5, 6, 7,
        8, 5, 9, 7, 6, 1, 4, 2, 3,
        4, 2, 6, 8, 5, 3, 7, 9, 1,
        7, 1, 3, 9, 2, 4, 8, 5, 6,
        9, 6, 1, 5, 3, 7, 2, 8, 4,
        2, 8, 7, 4, 1, 9, 6, 3, 5,
        3, 4, 5, 2, 8, 6, 1, 7, 9,
    )

    private fun puzzleWithBlanks(vararg blanks: Int): IntArray {
        val puzzle = solution.copyOf()
        blanks.forEach { puzzle[it] = 0 }
        return puzzle
    }

    @Test
    fun wrongNumberCountsMistakeAndUndoRestoresCell() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))
        val index = session.selected

        assertEquals(0, index)
        session.select(0)

        val wrong = if (solution[0] == 1) 2 else 1
        val outcome = session.enter(wrong)

        assertTrue(outcome.changed)
        assertEquals(1, session.mistakes)
        assertTrue(session.cells[0].error)
        assertEquals(wrong, session.cells[0].value)
        assertTrue(session.canUndo)

        val undo = session.undo()

        assertTrue(undo.changed)
        assertEquals(0, session.mistakes)
        assertEquals(0, session.cells[0].value)
        assertFalse(session.cells[0].error)
        assertFalse(session.canUndo)
    }

    @Test
    fun notesCanBeAddedAndRemoved() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        session.toggleNotes()
        session.enter(5)

        assertTrue(session.notesMode)
        assertEquals(setOf(5), session.cells[0].notes)
        assertEquals(0, session.cells[0].value)

        session.enter(5)

        assertTrue(session.cells[0].notes.isEmpty())

        session.toggleNotes()
        assertFalse(session.notesMode)
    }

    @Test
    fun eraseClearsValueAndNotes() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        session.toggleNotes()
        session.enter(3)
        session.toggleNotes()
        session.erase()

        assertTrue(session.cells[0].notes.isEmpty())
        assertEquals(0, session.cells[0].value)
    }

    @Test
    fun hintFillsCorrectValueAndMarksCellAsGiven() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        val outcome = session.hint()

        assertTrue(outcome.changed)
        assertEquals(solution[0], session.cells[0].value)
        assertTrue(session.cells[0].given)
        assertFalse(session.cells[0].error)
        assertEquals(0, session.mistakes)
    }

    @Test
    fun fillingLastCellWinsTheGame() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        val outcome = session.enter(solution[0])

        assertTrue(outcome.won)
        assertTrue(session.won)
    }

    @Test
    fun remainingCountsPlacedNumbers() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        assertEquals(1, session.remaining(5))
    }
}
