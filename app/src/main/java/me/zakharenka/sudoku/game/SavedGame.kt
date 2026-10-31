package me.zakharenka.sudoku.game

data class SavedGame(
    val difficulty: Difficulty,
    val solution: IntArray,
    val puzzle: IntArray,
    val values: IntArray,
    val givens: BooleanArray,
    val errors: BooleanArray,
    val notes: Array<Set<Int>>,
    val selected: Int?,
    val notesMode: Boolean,
    val mistakes: Int,
    val elapsedSeconds: Long,
    val won: Boolean,
)
