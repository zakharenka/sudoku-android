package me.zakharenka.sudoku.game

import java.util.ArrayDeque
import java.util.UUID

data class MoveOutcome(
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
    val showErrors: Boolean = true,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val puzzleCells: IntArray = puzzle.copyOf()

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

    var hintsUsed: Int = 0
        private set

    var won: Boolean = false
        private set

    val hintsRemaining: Int
        get() = MAX_HINTS - hintsUsed

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

    fun moveSelection(rowDelta: Int, colDelta: Int) {
        val current = selected
            ?: cells.indexOfFirst { it.value == 0 }.takeIf { it >= 0 }
            ?: 0
        val row = (current / SIZE + rowDelta).coerceIn(0, SIZE - 1)
        val col = (current % SIZE + colDelta).coerceIn(0, SIZE - 1)
        selected = row * SIZE + col
    }

    fun remaining(number: Int): Int = SIZE - cells.count { it.value == number }

    fun toggleNotes(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        notesMode = !notesMode
        return MoveOutcome(changed = true)
    }

    fun enter(number: Int): MoveOutcome {
        if (won || number !in 1..SIZE) {
            return MoveOutcome()
        }

        val index = selected ?: return MoveOutcome()
        val cell = cells[index]

        if (cell.given) {
            return MoveOutcome()
        }

        if (notesMode) {
            if (cell.value != 0) {
                return MoveOutcome()
            }

            val entry = snapshot(listOf(index))
            val notes = if (number in cell.notes) {
                cell.notes - number
            } else {
                cell.notes + number
            }

            cells[index] = cell.copy(notes = notes)
            pushHistory(entry)

            return MoveOutcome(changed = true)
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
        cells[index] = cell.copy(
            value = number,
            notes = emptySet(),
            error = showErrors && !correct,
        )

        if (correct) {
            for (peer in affected) {
                cells[peer] = cells[peer].copy(notes = cells[peer].notes - number)
            }
        } else if (showErrors) {
            mistakes += 1
        }

        pushHistory(entry)
        val finished = checkWin()

        return MoveOutcome(changed = true, won = finished)
    }

    fun erase(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        val index = selected ?: return MoveOutcome()
        val cell = cells[index]

        if (cell.given) {
            return MoveOutcome()
        }

        if (cell.value == 0 && cell.notes.isEmpty()) {
            return MoveOutcome()
        }

        val entry = snapshot(listOf(index))
        cells[index] = cell.copy(value = 0, error = false, notes = emptySet())
        pushHistory(entry)

        return MoveOutcome(changed = true)
    }

    fun undo(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        val entry = history.pollLast() ?: return MoveOutcome()

        for (change in entry.changes) {
            cells[change.index] = change.cell
        }

        selected = entry.changes.firstOrNull()?.index ?: selected

        return MoveOutcome(changed = true)
    }

    fun hint(): MoveOutcome {
        if (won) {
            return MoveOutcome()
        }

        if (hintsRemaining <= 0) {
            return MoveOutcome()
        }

        val candidates = cells.indices.filter { index ->
            !cells[index].given && cells[index].value != solution[index]
        }

        if (candidates.isEmpty()) {
            return MoveOutcome()
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
        )

        for (peer in affected) {
            cells[peer] = cells[peer].copy(notes = cells[peer].notes - number)
        }

        selected = index
        pushHistory(entry)
        hintsUsed += 1
        val finished = checkWin()

        return MoveOutcome(changed = true, won = finished)
    }

    fun toGameRecord(
        elapsedSeconds: Long,
        updatedAt: Long,
        solvedAt: Long? = if (won) updatedAt else null,
    ): GameRecord = GameRecord(
        id = id,
        difficulty = difficulty,
        showErrors = showErrors,
        errors = mistakes,
        puzzle = puzzleCells.copyOf(),
        solution = solution.copyOf(),
        values = IntArray(CELLS) { cells[it].value },
        notes = buildMap {
            cells.forEachIndexed { index, cell ->
                if (cell.notes.isNotEmpty()) {
                    put(index, cell.notes.toSet())
                }
            }
        },
        hintsUsed = hintsUsed,
        elapsedSeconds = elapsedSeconds.coerceAtLeast(0),
        createdAt = createdAt,
        updatedAt = updatedAt,
        solvedAt = solvedAt,
    )

    private fun checkWin(): Boolean {
        if (cells.indices.all { cells[it].value == solution[it] }) {
            won = true
            return true
        }

        return false
    }

    private fun snapshot(indices: List<Int>): HistoryEntry = HistoryEntry(
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
        const val MAX_HINTS = 3
        private const val MAX_HISTORY = 200

        fun restore(game: GameRecord, notesMode: Boolean = false): SudokuSession {
            require(game.isValid())

            val session = SudokuSession(
                difficulty = game.difficulty,
                solution = game.solution.copyOf(),
                puzzle = game.puzzle.copyOf(),
                notesMode = notesMode,
                showErrors = game.showErrors,
                id = game.id,
                createdAt = game.createdAt,
            )

            for (index in 0 until CELLS) {
                val value = game.values[index]

                session.cells[index] = Cell(
                    value = value,
                    given = game.puzzle[index] != 0,
                    error = game.showErrors && value != 0 && value != game.solution[index],
                    notes = game.notes[index]?.filter { it in 1..SIZE }?.toSet() ?: emptySet(),
                )
            }

            session.selected = session.cells.indexOfFirst { it.value == 0 }.takeIf { it >= 0 }
            session.mistakes = if (game.showErrors) game.errors else 0
            session.hintsUsed = game.hintsUsed
            session.won = game.solved

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
