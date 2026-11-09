package me.zakharenka.sudoku.game

/**
 * One saved game. It deliberately does not store the current selection, the
 * notes mode or per-cell error flags: they are either global settings or can be
 * derived from [puzzle] and [solution].
 */
data class GameRecord(
    val id: String,
    val difficulty: Difficulty,
    val showErrors: Boolean,
    val errors: Int,
    val puzzle: IntArray,
    val solution: IntArray,
    val values: IntArray,
    val notes: Map<Int, Set<Int>>,
    val hintsUsed: Int,
    val elapsedSeconds: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val solvedAt: Long?,
) {
    val solved: Boolean
        get() = solvedAt != null

    fun isValid(): Boolean {
        if (id.isBlank()) {
            return false
        }

        if (errors < 0 || hintsUsed !in 0..SudokuSession.MAX_HINTS || elapsedSeconds < 0) {
            return false
        }

        if (puzzle.size != SudokuSession.CELLS ||
            solution.size != SudokuSession.CELLS ||
            values.size != SudokuSession.CELLS
        ) {
            return false
        }

        for (index in 0 until SudokuSession.CELLS) {
            val puzzleValue = puzzle[index]
            val solutionValue = solution[index]
            val value = values[index]

            if (puzzleValue !in 0..SudokuSession.SIZE || value !in 0..SudokuSession.SIZE) {
                return false
            }

            if (solutionValue !in 1..SudokuSession.SIZE) {
                return false
            }

            if (puzzleValue != 0 && (puzzleValue != solutionValue || value != puzzleValue)) {
                return false
            }
        }

        if (notes.keys.any { it !in 0 until SudokuSession.CELLS }) {
            return false
        }

        for ((index, cellNotes) in notes) {
            if (cellNotes.isEmpty() || cellNotes.any { it !in 1..SudokuSession.SIZE }) {
                return false
            }

            if (values[index] != 0) {
                return false
            }
        }

        if (createdAt < 0 || updatedAt < 0 || (solvedAt != null && solvedAt < 0)) {
            return false
        }

        if (solvedAt != null && !values.contentEquals(solution)) {
            return false
        }

        if (!showErrors && errors != 0) {
            return false
        }

        return true
    }
}
