package me.zakharenka.sudoku.game

import kotlin.random.Random

/**
 * Генератор классического судоку 9×9.
 *
 * Сначала строится полное решение, затем из него удаляются цифры. Удаление
 * принимается только тогда, когда у поля остаётся ровно одно решение.
 */
class SudokuGenerator(private val random: Random = Random.Default) {

    fun generate(difficulty: Difficulty): GeneratedPuzzle {
        var best: GeneratedPuzzle? = null
        val attempts = if (difficulty == Difficulty.EXPERT) 4 else 2

        repeat(attempts) {
            val candidate = generatePuzzle(difficulty.clues)
            if (best == null || candidate.clues < best.clues) {
                best = candidate
            }
            if (best.clues <= difficulty.clues) {
                return best
            }
        }

        return checkNotNull(best) { "Не удалось сгенерировать поле" }
    }

    fun generateSolved(): IntArray {
        val pattern = { row: Int, col: Int -> (3 * (row % 3) + row / 3 + col) % SIZE }

        val bands = (0 until 3).shuffled(random)
        val rows = bands.flatMap { band ->
            (0 until 3).shuffled(random).map { row -> band * 3 + row }
        }
        val stacks = (0 until 3).shuffled(random)
        val cols = stacks.flatMap { stack ->
            (0 until 3).shuffled(random).map { col -> stack * 3 + col }
        }
        val numbers = (1..SIZE).shuffled(random)

        return IntArray(CELLS) { index ->
            val row = index / SIZE
            val col = index % SIZE
            numbers[pattern(rows[row], cols[col])]
        }
    }

    private fun generatePuzzle(targetClues: Int): GeneratedPuzzle {
        val solved = generateSolved()
        val puzzle = solved.copyOf()
        val order = (0 until CELLS).shuffled(random)
        var clues = CELLS

        for (index in order) {
            if (clues <= targetClues) {
                break
            }

            val backup = puzzle[index]
            puzzle[index] = 0

            if (countSolutions(puzzle, 2) == 1) {
                clues -= 1
            } else {
                puzzle[index] = backup
            }
        }

        return GeneratedPuzzle(puzzle, solved, clues)
    }

    fun countSolutions(source: IntArray, limit: Int = 2): Int {
        require(source.size == CELLS)

        val grid = source.copyOf()
        val rowMask = IntArray(SIZE)
        val colMask = IntArray(SIZE)
        val boxMask = IntArray(SIZE)

        for (index in 0 until CELLS) {
            val value = grid[index]
            if (value == 0) {
                continue
            }

            val bit = 1 shl (value - 1)
            val row = index / SIZE
            val col = index % SIZE
            val box = (row / 3) * 3 + col / 3

            if (rowMask[row] and bit != 0 ||
                colMask[col] and bit != 0 ||
                boxMask[box] and bit != 0
            ) {
                return 0
            }

            rowMask[row] = rowMask[row] or bit
            colMask[col] = colMask[col] or bit
            boxMask[box] = boxMask[box] or bit
        }

        var count = 0

        fun search(): Boolean {
            var bestIndex = -1
            var bestMask = 0
            var bestCount = SIZE + 1

            for (index in 0 until CELLS) {
                if (grid[index] != 0) {
                    continue
                }

                val row = index / SIZE
                val col = index % SIZE
                val box = (row / 3) * 3 + col / 3
                val used = rowMask[row] or colMask[col] or boxMask[box]
                val available = FULL_MASK and used.inv()
                val options = Integer.bitCount(available)

                if (options == 0) {
                    return false
                }

                if (options < bestCount) {
                    bestIndex = index
                    bestMask = available
                    bestCount = options

                    if (options == 1) {
                        break
                    }
                }
            }

            if (bestIndex == -1) {
                count += 1
                return count >= limit
            }

            var mask = bestMask
            val row = bestIndex / SIZE
            val col = bestIndex % SIZE
            val box = (row / 3) * 3 + col / 3

            while (mask != 0) {
                val bit = mask and -mask
                val value = Integer.numberOfTrailingZeros(bit) + 1

                grid[bestIndex] = value
                rowMask[row] = rowMask[row] or bit
                colMask[col] = colMask[col] or bit
                boxMask[box] = boxMask[box] or bit

                if (search()) {
                    return true
                }

                grid[bestIndex] = 0
                rowMask[row] = rowMask[row] xor bit
                colMask[col] = colMask[col] xor bit
                boxMask[box] = boxMask[box] xor bit
                mask = mask xor bit
            }

            return false
        }

        search()
        return count
    }

    fun isValidSolution(grid: IntArray): Boolean {
        if (grid.size != CELLS) {
            return false
        }

        for (index in 0 until CELLS) {
            val value = grid[index]
            if (value !in 1..SIZE) {
                return false
            }

            val row = index / SIZE
            val col = index % SIZE
            val box = (row / 3) * 3 + col / 3

            for (step in 0 until SIZE) {
                if (step != col && grid[row * SIZE + step] == value) {
                    return false
                }
                if (step != row && grid[step * SIZE + col] == value) {
                    return false
                }

                val boxRow = (box / 3) * 3 + step / 3
                val boxCol = (box % 3) * 3 + step % 3
                if ((boxRow != row || boxCol != col) &&
                    grid[boxRow * SIZE + boxCol] == value
                ) {
                    return false
                }
            }
        }

        return true
    }

    private companion object {
        const val SIZE = 9
        const val CELLS = SIZE * SIZE
        const val FULL_MASK = (1 shl SIZE) - 1
    }
}
