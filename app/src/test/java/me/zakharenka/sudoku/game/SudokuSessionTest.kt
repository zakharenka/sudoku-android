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
    fun selectionMovesByOneCellAndStopsAtEdges() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0))

        session.select(0)
        session.moveSelection(0, -1)
        session.moveSelection(-1, 0)
        assertEquals(0, session.selected)

        session.moveSelection(0, 1)
        assertEquals(1, session.selected)

        session.moveSelection(1, 0)
        assertEquals(10, session.selected)

        repeat(20) {
            session.moveSelection(1, 1)
        }
        assertEquals(SudokuSession.CELLS - 1, session.selected)
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
        assertEquals(SudokuSession.MAX_HINTS - 1, session.hintsRemaining)
    }

    @Test
    fun hintIsLimitedPerGame() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0, 1, 2, 3))

        repeat(SudokuSession.MAX_HINTS) { index ->
            session.select(index)
            assertTrue(session.hint().changed)
        }

        assertEquals(0, session.hintsRemaining)
        assertEquals(1, session.cells.count { it.value == 0 })

        val outcome = session.hint()

        assertFalse(outcome.changed)
        assertEquals(1, session.cells.count { it.value == 0 })
    }

    @Test
    fun undoDoesNotRefundAHint() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0, 1))

        session.select(0)
        session.hint()
        assertEquals(SudokuSession.MAX_HINTS - 1, session.hintsRemaining)

        session.undo()

        assertEquals(0, session.cells[0].value)
        assertEquals(SudokuSession.MAX_HINTS - 1, session.hintsRemaining)
    }

    @Test
    fun savedGameRemembersUsedHints() {
        val session = SudokuSession(Difficulty.EASY, solution, puzzleWithBlanks(0, 1))

        session.select(0)
        session.hint()

        val restored = SudokuSession.restore(session.toSavedGame(elapsedSeconds = 10))

        assertEquals(SudokuSession.MAX_HINTS - 1, restored.hintsRemaining)
        assertEquals(solution[0], restored.cells[0].value)
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

    @Test
    fun savedGameRoundTripRestoresBoardAndState() {
        val session = SudokuSession(Difficulty.HARD, solution, puzzleWithBlanks(0, 1, 2))

        session.select(0)
        session.enter(solution[0])

        session.select(1)
        session.toggleNotes()
        session.enter(4)
        session.toggleNotes()

        val wrong = if (solution[2] == 9) 8 else 9
        session.select(2)
        session.enter(wrong)
        session.toggleNotes()

        val saved = session.toSavedGame(elapsedSeconds = 123)
        val restored = SudokuSession.restore(saved)

        assertEquals(123, saved.elapsedSeconds)
        assertEquals(session.selected, restored.selected)
        assertEquals(session.mistakes, restored.mistakes)
        assertEquals(session.notesMode, restored.notesMode)
        assertEquals(session.won, restored.won)
        assertFalse(restored.canUndo)

        for (index in 0 until SudokuSession.CELLS) {
            assertEquals(session.cells[index].value, restored.cells[index].value)
            assertEquals(session.cells[index].given, restored.cells[index].given)
            assertEquals(session.cells[index].error, restored.cells[index].error)
            assertEquals(session.cells[index].notes, restored.cells[index].notes)
        }
    }
}
