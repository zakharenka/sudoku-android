package me.zakharenka.sudoku.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGeneratorTest {

    @Test
    fun solvedGridIsValid() {
        val generator = SudokuGenerator(Random(7))
        val solution = generator.generateSolved()

        assertEquals(81, solution.size)
        assertTrue(generator.isValidSolution(solution))
    }

    @Test
    fun generatedPuzzlesHaveUniqueSolution() {
        val generator = SudokuGenerator(Random(42))

        Difficulty.entries.forEach { difficulty ->
            val generated = generator.generate(difficulty)

            assertTrue(
                "Solution for ${difficulty.name} must be valid",
                generator.isValidSolution(generated.solution),
            )
            assertEquals(
                "Puzzle for ${difficulty.name} must have one solution",
                1,
                generator.countSolutions(generated.puzzle, 2),
            )
            assertTrue(
                "Clue count must not exceed the target",
                generated.clues <= difficulty.clues,
            )
            assertTrue("Clue count must be at least 17", generated.clues >= 17)
            assertEquals(
                "Clue count must match the puzzle",
                generated.clues,
                generated.puzzle.count { it != 0 },
            )
        }
    }

    @Test
    fun countSolutionsStopsAtLimit() {
        val generator = SudokuGenerator(Random(1))
        val empty = IntArray(81)

        assertEquals(2, generator.countSolutions(empty, 2))
    }
}
