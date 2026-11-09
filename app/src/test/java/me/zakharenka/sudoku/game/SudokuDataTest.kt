package me.zakharenka.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuDataTest {

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

    private fun game(
        id: String = "game",
        showErrors: Boolean = true,
        errors: Int = 0,
        elapsedSeconds: Long = 100,
        solvedAt: Long? = 100,
        values: IntArray = solution.copyOf(),
    ): GameRecord = GameRecord(
        id = id,
        difficulty = Difficulty.MEDIUM,
        showErrors = showErrors,
        errors = errors,
        puzzle = IntArray(SudokuSession.CELLS),
        solution = solution.copyOf(),
        values = values,
        notes = emptyMap(),
        hintsUsed = 0,
        elapsedSeconds = elapsedSeconds,
        createdAt = 1,
        updatedAt = 100,
        solvedAt = solvedAt,
    )

    private fun stats(data: SudokuData): RecordStats =
        data.recordsByDifficulty().getValue(Difficulty.MEDIUM)

    @Test
    fun recordsAreDerivedFromSolvedGames() {
        val data = SudokuData(
            games = listOf(
                game(id = "clean", errors = 0, elapsedSeconds = 60),
                game(id = "dirty", errors = 2, elapsedSeconds = 90),
                game(
                    id = "open",
                    solvedAt = null,
                    values = IntArray(SudokuSession.CELLS),
                    elapsedSeconds = 10,
                ),
            ),
        )
        val records = stats(data)

        assertEquals(1, records.withoutMistakes.solved)
        assertEquals(60L, records.withoutMistakes.bestSeconds)
        assertEquals(1, records.withMistakes.solved)
        assertEquals(90L, records.withMistakes.bestSeconds)
    }

    @Test
    fun cleanWinUpdatesCleanCategoryOnly() {
        val data = SudokuData(
            games = listOf(
                game(id = "clean", errors = 0, elapsedSeconds = 60),
                game(id = "dirty", errors = 2, elapsedSeconds = 90),
            ),
        )

        val result = data.withWin(game(id = "new", errors = 0, elapsedSeconds = 40))
        val records = stats(result.data)

        assertTrue(result.isNewRecord)
        assertEquals("new", result.data.currentGameId)
        assertEquals(2, records.withoutMistakes.solved)
        assertEquals(40L, records.withoutMistakes.bestSeconds)
        assertEquals(1, records.withMistakes.solved)
        assertEquals(90L, records.withMistakes.bestSeconds)
    }

    @Test
    fun winWithMistakesUpdatesMistakesCategoryOnly() {
        val data = SudokuData(
            games = listOf(
                game(id = "clean", errors = 0, elapsedSeconds = 40),
                game(id = "dirty", errors = 1, elapsedSeconds = 100),
            ),
        )

        val result = data.withWin(game(id = "new", errors = 3, elapsedSeconds = 80))
        val records = stats(result.data)

        assertTrue(result.isNewRecord)
        assertEquals(2, records.withMistakes.solved)
        assertEquals(80L, records.withMistakes.bestSeconds)
        assertEquals(1, records.withoutMistakes.solved)
        assertEquals(40L, records.withoutMistakes.bestSeconds)
    }

    @Test
    fun slowerWinWithMistakesDoesNotSetRecord() {
        val data = SudokuData(
            games = listOf(game(id = "dirty", errors = 1, elapsedSeconds = 70)),
        )

        val result = data.withWin(game(id = "new", errors = 2, elapsedSeconds = 120))

        assertFalse(result.isNewRecord)
        assertEquals(70L, stats(result.data).withMistakes.bestSeconds)
    }

    @Test
    fun hiddenErrorsWinCountsAsClean() {
        val result = SudokuData().withWin(
            game(showErrors = false, errors = 0, elapsedSeconds = 80),
        )
        val records = stats(result.data)

        assertTrue(result.isNewRecord)
        assertEquals(1, records.withoutMistakes.solved)
        assertEquals(80L, records.withoutMistakes.bestSeconds)
        assertEquals(0, records.withMistakes.solved)
    }

    @Test
    fun normalizedClearsOnlyMissingCurrentGame() {
        val unsolved = game(id = "unsolved", solvedAt = null, values = IntArray(SudokuSession.CELLS))
        val solved = game(id = "solved")

        assertEquals(
            "unsolved",
            SudokuData(
                currentGameId = "unsolved",
                games = listOf(unsolved),
            ).normalized().currentGameId,
        )

        assertNull(
            SudokuData(
                currentGameId = "missing",
                games = listOf(unsolved),
            ).normalized().currentGameId,
        )

        assertEquals(
            "solved",
            SudokuData(
                currentGameId = "solved",
                games = listOf(solved),
            ).normalized().currentGameId,
        )
    }

    @Test
    fun gameRecordValidationRejectsBrokenValues() {
        assertTrue(game().isValid())
        assertFalse(game(values = IntArray(SudokuSession.CELLS)).isValid())
        assertFalse(game(showErrors = false, errors = 1).isValid())
        assertFalse(game(errors = -1).isValid())
    }
}
