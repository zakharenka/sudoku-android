package me.zakharenka.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF070B14)
val PanelTop = Color(0xFF172031)
val PanelBottom = Color(0xFF0B101A)
val PanelDeep = Color(0xFF050912)
val TextPrimary = Color(0xFFE7EDF8)
val TextMuted = Color(0xFF94A3B8)
val Accent = Color(0xFF22D3EE)
val AccentSoft = Color(0xFF7DD3FC)
val Danger = Color(0xFFFB7185)
val Success = Color(0xFF34D399)
val Violet = Color(0xFFA78BFA)
val Amber = Color(0xFFFBBF24)
val Line = Color(0x2694A3B8)
val LineStrong = Color(0x4D94A3B8)

private val SudokuColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF041016),
    secondary = AccentSoft,
    background = Background,
    onBackground = TextPrimary,
    surface = PanelTop,
    onSurface = TextPrimary,
    surfaceVariant = PanelBottom,
    onSurfaceVariant = TextMuted,
    error = Danger,
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SudokuColors,
        content = content,
    )
}
