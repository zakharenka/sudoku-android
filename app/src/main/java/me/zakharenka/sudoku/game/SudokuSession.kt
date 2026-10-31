package me.zakharenka.sudoku.game

import java.util.ArrayDeque

enum class StatusKind {
    NEUTRAL,
    ERROR,
    SUCCESS,
}

data class MoveOutcome(
    val message: String = "",
    val kind: StatusKind = StatusKind.NEUTRAL,
    val changed: Boolean = false,
    val won: Boolean = false,
)

data class Cell(
    val value: Int = 0,
    val given: Boolean = false,
    val error: Boolean = false,
    val notes: Set<Int> = emptySet(),
)

private data class CellChange(
    val index: Int,
    val cell: Cell,
)

private data class HistoryEntry(
    val mistakes: Int,
    val changes: List<CellChange>,
)

/**
 * Состояние одной партии. Не зависит от Android и UI, поэтому легко тестируется.
 */
class SudokuSession(
    val difficulty: Difficulty,
    val solution: IntArray,
    puzzle: IntArray,
    notesMode: Boolean = false,
) {
    val cells: Array<Cell> = Array(CELLS) { index ->
        val value = puzzle[index]
        Cell(value = value, given = value != 0)
    }

    var selected: Int? = cells.indexOfFirst { it.value == 0 }.takeIf { it >= 0 }
        private set

    var notesMode: Boolean = notesMode
        private set

    var mistakes: Int = 0
        private set

    var won: Boolean = false
        private set

    private val history = ArrayDeque<HistoryEntry>()

    val canUndo: Boolean
        get() = history.isNotEmpty()

    val canErase: Boolean
        get() {
            val index = selected ?: return false
            val cell = cells[index]
            return !cell.given && (cell.value != 0 || cell.notes.isNotEmpty())
        }

    fun select(index: Int) {
        if (index in 0 until CELLS) {
            selected = index
        }
    }

    fun remaining(number: Int): Int = SIZE - cells.count { it.value == number }

    fun toggleNotes(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        notesMode = !notesMode
        return MoveOutcome(
            message = if (notesMode) "Заметки включены" else "Заметки выключены",
            changed = true,
        )
    }

    fun enter(number: Int): MoveOutcome {
        if (won || number !in 1..SIZE) {
            return MoveOutcome()
        }

        val index = selected
            ?: return MoveOutcome("Сначала выберите клетку", StatusKind.ERROR)

        val cell = cells[index]

        if (cell.given) {
            return MoveOutcome("Эта клетка уже заполнена")
        }

        if (notesMode) {
            if (cell.value != 0) {
                return MoveOutcome(
                    "Сначала сотрите цифру, чтобы добавить заметку",
                    StatusKind.ERROR,
                )
            }

            val entry = snapshot(listOf(index))
            val notes = if (number in cell.notes) {
                cell.notes - number
            } else {
                cell.notes + number
            }

            cells[index] = cell.copy(notes = notes)
            pushHistory(entry)

            return MoveOutcome(
                message = if (number in notes) {
                    "Заметка $number добавлена"
                } else {
                    "Заметка $number убрана"
                },
                changed = true,
            )
        }

        if (cell.value == number) {
            return MoveOutcome()
        }

        val correct = number == solution[index]
        val affected = if (correct) {
            PEERS[index].filter { number in cells[it].notes }
        } else {
            emptyList()
        }

        val entry = snapshot(listOf(index) + affected)
        cells[index] = cell.copy(value = number, notes = emptySet(), error = !correct)

        if (correct) {
            for (peer in affected) {
                cells[peer] = cells[peer].copy(notes = cells[peer].notes - number)
            }
        } else {
            mistakes += 1
        }

        pushHistory(entry)
        val finished = checkWin()

        return when {
            finished -> MoveOutcome("Решено!", StatusKind.SUCCESS, changed = true, won = true)
            correct -> MoveOutcome("Верно", changed = true)
            else -> MoveOutcome(
                "Неверная цифра. Ошибок: $mistakes.",
                StatusKind.ERROR,
                changed = true,
            )
        }
    }

    fun erase(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        val index = selected ?: return MoveOutcome()
        val cell = cells[index]

        if (cell.given) {
            return MoveOutcome("Эту клетку нельзя стереть", StatusKind.ERROR)
        }

        if (cell.value == 0 && cell.notes.isEmpty()) {
            return MoveOutcome()
        }

        val entry = snapshot(listOf(index))
        cells[index] = cell.copy(value = 0, error = false, notes = emptySet())
        pushHistory(entry)

        return MoveOutcome("Клетка очищена", changed = true)
    }

    fun undo(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        val entry = history.pollLast() ?: return MoveOutcome()

        for (change in entry.changes) {
            cells[change.index] = change.cell
        }

        mistakes = entry.mistakes
        selected = entry.changes.firstOrNull()?.index ?: selected

        return MoveOutcome("Ход отменён", changed = true)
    }

    fun hint(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        val candidates = cells.indices.filter { index ->
            !cells[index].given && cells[index].value != solution[index]
        }

        if (candidates.isEmpty()) {
            return MoveOutcome("Поле уже заполнено верно")
        }

        val index = selected?.takeIf { it in candidates }
            ?: candidates.random()
        val number = solution[index]
        val affected = PEERS[index].filter { number in cells[it].notes }
        val entry = snapshot(listOf(index) + affected)

        cells[index] = cells[index].copy(
            value = number,
            notes = emptySet(),
            error = false,
            given = true,
        )

        for (peer in affected) {
            cells[peer] = cells[peer].copy(notes = cells[peer].notes - number)
        }

        selected = index
        pushHistory(entry)
        val finished = checkWin()

        val row = index / SIZE + 1
        val col = index % SIZE + 1

        return MoveOutcome(
            message = "Подсказка: $number — строка $row, столбец $col",
            kind = StatusKind.SUCCESS,
            changed = true,
            won = finished,
        )
    }

    private fun checkWin(): Boolean {
        if (cells.indices.all { cells[it].value == solution[it] }) {
            won = true
            return true
        }

        return false
    }

    fun toSavedGame(elapsedSeconds: Long): SavedGame = SavedGame(
        difficulty = difficulty,
        solution = solution.copyOf(),
        puzzle = IntArray(CELLS) { index ->
            if (cells[index].given) cells[index].value else 0
        },
        values = IntArray(CELLS) { cells[it].value },
        givens = BooleanArray(CELLS) { cells[it].given },
        errors = BooleanArray(CELLS) { cells[it].error },
        notes = Array(CELLS) { cells[it].notes },
        selected = selected,
        notesMode = notesMode,
        mistakes = mistakes,
        elapsedSeconds = elapsedSeconds.coerceAtLeast(0),
        won = won,
    )

    private fun snapshot(indices: List<Int>): HistoryEntry = HistoryEntry(
        mistakes = mistakes,
        changes = indices.map { index -> CellChange(index, cells[index]) },
    )

    private fun pushHistory(entry: HistoryEntry) {
        if (entry.changes.isEmpty()) {
            return
        }

        history.addLast(entry)
        if (history.size > MAX_HISTORY) {
            history.removeFirst()
        }
    }

    companion object {
        const val SIZE = 9
        const val CELLS = SIZE * SIZE
        private const val MAX_HISTORY = 200

        fun restore(saved: SavedGame): SudokuSession {
            require(saved.solution.size == CELLS)
            require(saved.puzzle.size == CELLS)
            require(saved.values.size == CELLS)
            require(saved.givens.size == CELLS)
            require(saved.errors.size == CELLS)
            require(saved.notes.size == CELLS)

            val session = SudokuSession(
                difficulty = saved.difficulty,
                solution = saved.solution.copyOf(),
                puzzle = saved.puzzle.copyOf(),
                notesMode = saved.notesMode,
            )

            for (index in 0 until CELLS) {
                val value = saved.values[index]
                require(value in 0..SIZE)

                session.cells[index] = Cell(
                    value = value,
                    given = saved.givens[index],
                    error = saved.errors[index],
                    notes = saved.notes[index].filter { it in 1..SIZE }.toSet(),
                )
            }

            session.selected = saved.selected?.takeIf { it in 0 until CELLS }
                ?: session.cells.indexOfFirst { it.value == 0 }.takeIf { it >= 0 }
            session.mistakes = saved.mistakes.coerceAtLeast(0)
            session.won = saved.won

            return session
        }

        private val PEERS: Array<IntArray> = Array(CELLS) { index ->
            val row = index / SIZE
            val col = index % SIZE
            val peers = linkedSetOf<Int>()

            for (step in 0 until SIZE) {
                peers.add(row * SIZE + step)
                peers.add(step * SIZE + col)
            }

            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3

            for (rowOffset in 0 until 3) {
                for (colOffset in 0 until 3) {
                    peers.add((boxRow + rowOffset) * SIZE + boxCol + colOffset)
                }
            }

            peers.remove(index)
            peers.toIntArray()
        }
    }
}
