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

        preferences.edit { putLong(key, seconds) }
        return true
    }

    var notesMode: Boolean
        get() = preferences.getBoolean(NOTES_MODE_KEY, false)
        set(value) {
            preferences.edit { putBoolean(NOTES_MODE_KEY, value) }
        }

    private fun bestKey(difficulty: Difficulty): String = "best:${difficulty.key}"

    private companion object {
        const val PREFERENCES = "sudoku"
        const val NOTES_MODE_KEY = "notes_mode"
    }
}
