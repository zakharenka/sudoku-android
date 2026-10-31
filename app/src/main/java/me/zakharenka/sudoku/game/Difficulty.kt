package me.zakharenka.sudoku.game

enum class Difficulty(
    val key: String,
    val clues: Int,
) {
    EASY("easy", 45),
    MEDIUM("medium", 38),
    HARD("hard", 32),
    EXPERT("expert", 27),
}
