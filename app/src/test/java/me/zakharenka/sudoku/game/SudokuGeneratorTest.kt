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
                "Решение для ${difficulty.name} должно быть корректным",
                generator.isValidSolution(generated.solution),
            )
            assertEquals(
                "У поля ${difficulty.name} должно быть одно решение",
                1,
                generator.countSolutions(generated.puzzle, 2),
            )
            assertTrue(
                "Подсказок должно быть не больше целевого значения",
                generated.clues <= difficulty.clues,
            )
            assertTrue("Подсказок должно быть не меньше 17", generated.clues >= 17)
            assertEquals(
                "Счётчик подсказок должен совпадать с полем",
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
