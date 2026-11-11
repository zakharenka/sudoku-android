package me.zakharenka.sudoku.data

import android.content.Context
import androidx.core.content.edit

/**
 * Only global settings live in SharedPreferences. Records, the current game and
 * the history are stored in sudoku_data.json by [SudokuDataStore].
 */
class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

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

    var straightGrid: Boolean
        get() = preferences.getBoolean(STRAIGHT_GRID_KEY, true)
        set(value) {
            preferences.edit { putBoolean(STRAIGHT_GRID_KEY, value) }
        }

    var sameHighlight: Boolean
        get() = preferences.getBoolean(SAME_HIGHLIGHT_KEY, true)
        set(value) {
            preferences.edit { putBoolean(SAME_HIGHLIGHT_KEY, value) }
        }

    var showErrors: Boolean
        get() = preferences.getBoolean(SHOW_ERRORS_KEY, true)
        set(value) {
            preferences.edit { putBoolean(SHOW_ERRORS_KEY, value) }
        }

    private companion object {
        const val PREFERENCES = "sudoku"
        const val NOTES_MODE_KEY = "notes_mode"
        const val THEME_MODE_KEY = "theme_mode"
        const val STRAIGHT_GRID_KEY = "straight_grid"
        const val SAME_HIGHLIGHT_KEY = "same_highlight"
        const val SHOW_ERRORS_KEY = "show_errors"
    }
}
