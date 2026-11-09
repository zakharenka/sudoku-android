package me.zakharenka.sudoku.data

import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.GameRecord
import me.zakharenka.sudoku.game.SudokuData
import me.zakharenka.sudoku.game.SudokuSession
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class DataError {
    IO_ERROR,
    INVALID_JSON,
    UNSUPPORTED_VERSION,
    INVALID_FIELDS,
}

sealed interface DataDecodeResult {
    data class Success(val data: SudokuData) : DataDecodeResult
    data class Failure(val error: DataError) : DataDecodeResult
}

sealed interface DataTransferResult {
    data object Success : DataTransferResult
    data class Failure(val error: DataError) : DataTransferResult
}

/**
 * The only JSON parser/validator for both the main storage and import. A file
 * is accepted only when all fields can be decoded safely; otherwise the caller
 * gets a [DataError] and existing data stays untouched.
 */
object SudokuDataCodec {
    const val VERSION = 1

    fun encode(data: SudokuData): String {
        val games = JSONArray()

        data.games.forEach { game ->
            games.put(
                JSONObject().apply {
                    put("id", game.id)
                    put("difficulty", game.difficulty.key)
                    put("showErrors", game.showErrors)
                    put("errors", game.errors)
                    put("puzzle", game.puzzle.joinToString(""))
                    put("solution", game.solution.joinToString(""))
                    put("values", game.values.joinToString(""))
                    put("notes", JSONObject().apply {
                        game.notes.toSortedMap().forEach { (index, notes) ->
                            put(index.toString(), JSONArray(notes.sorted()))
                        }
                    })
                    put("hintsUsed", game.hintsUsed)
                    put("elapsedSeconds", game.elapsedSeconds)
                    put("createdAt", game.createdAt)
                    put("updatedAt", game.updatedAt)
                    put("solvedAt", game.solvedAt ?: JSONObject.NULL)
                },
            )
        }

        return JSONObject().apply {
            put("version", VERSION)
            put("currentGameId", data.currentGameId ?: JSONObject.NULL)
            put("games", games)
        }.toString()
    }

    fun decode(raw: String): DataDecodeResult {
        val json = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            return DataDecodeResult.Failure(DataError.INVALID_JSON)
        }

        val version = json.optInt("version", -1)

        if (version != VERSION) {
            return DataDecodeResult.Failure(DataError.UNSUPPORTED_VERSION)
        }

        return try {
            val currentGameId = if (json.isNull("currentGameId")) {
                null
            } else {
                json.getString("currentGameId").takeIf { it.isNotBlank() }
                    ?: return DataDecodeResult.Failure(DataError.INVALID_FIELDS)
            }

            val games = decodeGames(json.getJSONArray("games"))
                ?: return DataDecodeResult.Failure(DataError.INVALID_FIELDS)
            val ids = mutableSetOf<String>()

            games.forEach { game ->
                if (!ids.add(game.id)) {
                    return DataDecodeResult.Failure(DataError.INVALID_FIELDS)
                }
            }

            DataDecodeResult.Success(
                SudokuData(
                    currentGameId = currentGameId,
                    games = games,
                ).normalized(),
            )
        } catch (_: JSONException) {
            DataDecodeResult.Failure(DataError.INVALID_FIELDS)
        }
    }

    private fun decodeGames(array: JSONArray): List<GameRecord>? {
        val result = mutableListOf<GameRecord>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: return null
            val difficulty = Difficulty.entries.firstOrNull {
                it.key == item.getString("difficulty")
            } ?: return null

            val game = GameRecord(
                id = item.getString("id"),
                difficulty = difficulty,
                showErrors = item.getBoolean("showErrors"),
                errors = item.getInt("errors"),
                puzzle = item.getString("puzzle").toDigits(min = 0) ?: return null,
                solution = item.getString("solution").toDigits(min = 1) ?: return null,
                values = item.getString("values").toDigits(min = 0) ?: return null,
                notes = decodeNotes(item.getJSONObject("notes")) ?: return null,
                hintsUsed = item.getInt("hintsUsed"),
                elapsedSeconds = item.getLong("elapsedSeconds"),
                createdAt = item.getLong("createdAt"),
                updatedAt = item.getLong("updatedAt"),
                solvedAt = if (item.isNull("solvedAt")) {
                    null
                } else {
                    item.getLong("solvedAt")
                },
            )

            if (!game.isValid()) {
                return null
            }

            result += game
        }

        return result
    }

    private fun decodeNotes(json: JSONObject): Map<Int, Set<Int>>? {
        val result = mutableMapOf<Int, Set<Int>>()

        json.keys().forEach { key ->
            val index = key.toIntOrNull()?.takeIf { it in 0 until SudokuSession.CELLS }
                ?: return null
            val values = json.getJSONArray(key)
            val notes = mutableSetOf<Int>()

            for (position in 0 until values.length()) {
                val note = values.getInt(position)

                if (note !in 1..SudokuSession.SIZE || !notes.add(note)) {
                    return null
                }
            }

            if (notes.isEmpty()) {
                return null
            }

            result[index] = notes
        }

        return result
    }

    private fun String.toDigits(min: Int): IntArray? {
        if (length != SudokuSession.CELLS) {
            return null
        }

        val result = IntArray(length)

        for (index in indices) {
            val digit = this[index].digitToIntOrNull()
                ?.takeIf { it in min..SudokuSession.SIZE }
                ?: return null

            result[index] = digit
        }

        return result
    }
}
