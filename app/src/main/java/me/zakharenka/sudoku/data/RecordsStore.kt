package me.zakharenka.sudoku.data

import android.content.Context
import androidx.core.content.edit
import me.zakharenka.sudoku.game.Difficulty

class RecordsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun loadBestTimes(): Map<Difficulty, Long?> = Difficulty.entries.associateWith { difficulty ->
        val value = preferences.getLong(bestKey(difficulty), -1L)
        value.takeIf { it >= 0 }
    }

    /**
     * Сохраняет время, если оно лучше предыдущего. Возвращает true для нового рекорда.
     */
    fun saveBestTime(difficulty: Difficulty, seconds: Long): Boolean {
        val key = bestKey(difficulty)
        val previous = preferences.getLong(key, -1L)

        if (previous >= 0 && seconds >= previous) {
            return false
        }

        preferences.edit(commit = true) { putLong(key, seconds) }
        return true
    }

    fun loadSolvedCounts(): Map<Difficulty, Int> = Difficulty.entries.associateWith { difficulty ->
        preferences.getInt(solvedKey(difficulty), 0)
    }

    fun incrementSolvedCount(difficulty: Difficulty): Map<Difficulty, Int> {
        val counts = loadSolvedCounts().toMutableMap()
        counts[difficulty] = (counts[difficulty] ?: 0) + 1

        preferences.edit(commit = true) {
            putInt(solvedKey(difficulty), counts[difficulty] ?: 0)
        }

        return counts
    }

    fun clearBestTimes() {
        preferences.edit(commit = true) {
            Difficulty.entries.forEach { difficulty ->
                remove(bestKey(difficulty))
                remove(solvedKey(difficulty))
            }
        }
    }

    var notesMode: Boolean
        get() = preferences.getBoolean(NOTES_MODE_KEY, false)
        set(value) {
            preferences.edit { putBoolean(NOTES_MODE_KEY, value) }
        }

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(preferences.getString(THEME_MODE_KEY, null))
        set(value) {
            preferences.edit { putString(THEME_MODE_KEY, value.key) }
        }

    private fun bestKey(difficulty: Difficulty): String = "best:${difficulty.key}"

    private fun solvedKey(difficulty: Difficulty): String = "solved:${difficulty.key}"

    private companion object {
        const val PREFERENCES = "sudoku"
        const val NOTES_MODE_KEY = "notes_mode"
        const val THEME_MODE_KEY = "theme_mode"
    }
}
