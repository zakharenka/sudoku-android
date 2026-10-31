package me.zakharenka.sudoku.game

import java.util.Locale

fun formatTime(seconds: Long): String {
    val total = maxOf(0L, seconds)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val rest = total % 60

    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, rest)
    }
}
