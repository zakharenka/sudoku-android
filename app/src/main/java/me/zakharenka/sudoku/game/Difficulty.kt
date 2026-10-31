package me.zakharenka.sudoku.game

enum class Difficulty(
    val key: String,
    val label: String,
    val note: String,
    val clues: Int,
) {
    EASY("easy", "Легко", "разминка", 45),
    MEDIUM("medium", "Средне", "баланс", 38),
    HARD("hard", "Сложно", "вызов", 32),
    EXPERT("expert", "Эксперт", "максимум", 27),
}
