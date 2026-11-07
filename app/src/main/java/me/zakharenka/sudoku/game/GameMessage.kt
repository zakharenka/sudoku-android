package me.zakharenka.sudoku.game

sealed interface GameMessage {
    data object SelectCellFirst : GameMessage
    data object CellAlreadyFilled : GameMessage
    data object EraseNumberFirst : GameMessage
    data class NoteAdded(val number: Int) : GameMessage
    data class NoteRemoved(val number: Int) : GameMessage
    data object Correct : GameMessage
    data class WrongNumber(val mistakes: Int) : GameMessage
    data object CellCleared : GameMessage
    data object MoveUndone : GameMessage
    data object BoardAlreadyCorrect : GameMessage
    data object NoHintsLeft : GameMessage
    data class Hint(
        val number: Int,
        val row: Int,
        val col: Int,
        val hintsLeft: Int,
    ) : GameMessage
    data object CannotEraseGiven : GameMessage
    data object NotesEnabled : GameMessage
    data object NotesDisabled : GameMessage
    data object Solved : GameMessage
}
