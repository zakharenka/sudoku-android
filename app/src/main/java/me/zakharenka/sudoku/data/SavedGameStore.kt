package me.zakharenka.sudoku.data

import android.content.Context
import androidx.core.content.edit
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.SavedGame
import me.zakharenka.sudoku.game.SudokuSession
import org.json.JSONArray
import org.json.JSONObject

class SavedGameStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(game: SavedGame) {
        val json = JSONObject().apply {
            put("version", VERSION)
            put("difficulty", game.difficulty.key)
            put("solution", game.solution.joinToString(""))
            put("puzzle", game.puzzle.joinToString(""))
            put("values", game.values.joinToString(""))
            put("givens", game.givens.joinToString("") { if (it) "1" else "0" })
            put("errors", game.errors.joinToString("") { if (it) "1" else "0" })
            put("notes", JSONArray().apply {
                game.notes.forEach { notes ->
                    put(notes.sorted().joinToString(""))
                }
            })
            put("selected", game.selected ?: -1)
            put("notesMode", game.notesMode)
            put("mistakes", game.mistakes)
            put("hintsUsed", game.hintsUsed)
            put("elapsedSeconds", game.elapsedSeconds)
            put("won", game.won)
        }

        preferences.edit(commit = true) { putString(KEY, json.toString()) }
    }

    fun load(): SavedGame? {
        val raw = preferences.getString(KEY, null) ?: return null

        return runCatching {
            val json = JSONObject(raw)
            val version = json.optInt("version", 0)
            if (version !in 1..VERSION) {
                return null
            }

            val difficulty = Difficulty.entries.firstOrNull {
                it.key == json.getString("difficulty")
            } ?: return null

            val solution = json.getString("solution").toDigits()
            val puzzle = json.getString("puzzle").toDigits()
            val values = json.getString("values").toDigits()
            val givens = json.getString("givens").toFlags()
            val errors = json.getString("errors").toFlags()
            val notesArray = json.getJSONArray("notes")

            if (solution.size != SudokuSession.CELLS ||
                puzzle.size != SudokuSession.CELLS ||
                values.size != SudokuSession.CELLS ||
                givens.size != SudokuSession.CELLS ||
                errors.size != SudokuSession.CELLS ||
                notesArray.length() != SudokuSession.CELLS
            ) {
                return null
            }

            val notes = Array(SudokuSession.CELLS) { index ->
                notesArray.getString(index)
                    .mapNotNull { it.digitToIntOrNull() }
                    .filter { it in 1..SudokuSession.SIZE }
                    .toSet()
            }

            SavedGame(
                difficulty = difficulty,
                solution = solution,
                puzzle = puzzle,
                values = values,
                givens = givens,
                errors = errors,
                notes = notes,
                selected = json.optInt("selected", -1).takeIf { it in 0 until SudokuSession.CELLS },
                notesMode = json.optBoolean("notesMode", false),
                mistakes = json.optInt("mistakes", 0).coerceAtLeast(0),
                hintsUsed = json.optInt("hintsUsed", 0)
                    .coerceIn(0, SudokuSession.MAX_HINTS),
                elapsedSeconds = json.optLong("elapsedSeconds", 0L).coerceAtLeast(0),
                won = json.optBoolean("won", false),
            )
        }.getOrNull()
    }

    fun clear() {
        preferences.edit(commit = true) { remove(KEY) }
    }

    private fun String.toDigits(): IntArray {
        if (length != SudokuSession.CELLS) {
            return IntArray(0)
        }

        val result = IntArray(length)

        for (index in indices) {
            val digit = this[index].digitToIntOrNull() ?: return IntArray(0)
            if (digit !in 0..SudokuSession.SIZE) {
                return IntArray(0)
            }
            result[index] = digit
        }

        return result
    }

    private fun String.toFlags(): BooleanArray {
        if (length != SudokuSession.CELLS) {
            return BooleanArray(0)
        }

        val result = BooleanArray(length)

        for (index in indices) {
            result[index] = when (this[index]) {
                '1' -> true
                '0' -> false
                else -> return BooleanArray(0)
            }
        }

        return result
    }

    private companion object {
        const val PREFERENCES = "sudoku_saved_game"
        const val KEY = "saved_game"
        const val VERSION = 2
    }
}
