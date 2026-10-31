package me.zakharenka.sudoku.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import me.zakharenka.sudoku.data.ThemeMode

data class SketchColors(
    val isDark: Boolean,
    val paper: Color,
    val paperAlt: Color,
    val paperSunken: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentWash: Color,
    val highlight: Color,
    val danger: Color,
    val dangerWash: Color,
    val success: Color,
    val shadow: Color,
    val gridLine: Color,
    val gridLineStrong: Color,
    val paperSpeckle: Color,
)

val LightSketchColors = SketchColors(
    isDark = false,
    paper = Color(0xFFF4EFE4),
    paperAlt = Color(0xFFFBF7EE),
    paperSunken = Color(0xFFEDE5D5),
    ink = Color(0xFF243447),
    inkSoft = Color(0xFF5E6B7A),
    accent = Color(0xFF1F6F9C),
    accentSoft = Color(0xFF4C9AC4),
    accentWash = Color(0xFFCFE7F2),
    highlight = Color(0xFFF2DFA8),
    danger = Color(0xFFB83A3A),
    dangerWash = Color(0xFFF4D2CE),
    success = Color(0xFF2F7D4E),
    shadow = Color(0x2E243447),
    gridLine = Color(0x59243447),
    gridLineStrong = Color(0xBF243447),
    paperSpeckle = Color(0x0D243447),
)

val DarkSketchColors = SketchColors(
    isDark = true,
    paper = Color(0xFF14161A),
    paperAlt = Color(0xFF1B1E23),
    paperSunken = Color(0xFF0F1115),
    ink = Color(0xFFE9E4D8),
    inkSoft = Color(0xFFB0AA9E),
    accent = Color(0xFF7DD3FC),
    accentSoft = Color(0xFFA5E3FF),
    accentWash = Color(0xFF1E3A4A),
    highlight = Color(0xFF4A422A),
    danger = Color(0xFFF87171),
    dangerWash = Color(0xFF4A2528),
    success = Color(0xFF6EE7B7),
    shadow = Color(0x73000000),
    gridLine = Color(0x4DE9E4D8),
    gridLineStrong = Color(0xCCE9E4D8),
    paperSpeckle = Color(0x0DE9E4D8),
)

private val LocalSketchColors = staticCompositionLocalOf { LightSketchColors }

object SudokuTheme {
    val colors: SketchColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSketchColors.current
}

@Composable
fun SudokuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkSketchColors else LightSketchColors

    CompositionLocalProvider(LocalSketchColors provides colors) {
        ApplySystemBars()
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            content = content,
        )
    }
}

private fun SketchColors.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = paper,
        secondary = accentSoft,
        background = paper,
        onBackground = ink,
        surface = paperAlt,
        onSurface = ink,
        surfaceVariant = paperSunken,
        onSurfaceVariant = inkSoft,
        error = danger,
        onError = paper,
        outline = gridLineStrong,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = paper,
        secondary = accentSoft,
        background = paper,
        onBackground = ink,
        surface = paperAlt,
        onSurface = ink,
        surfaceVariant = paperSunken,
        onSurfaceVariant = inkSoft,
        error = danger,
        onError = paper,
        outline = gridLineStrong,
    )
}

@Composable
private fun ApplySystemBars() {
    val colors = LocalSketchColors.current
    val view = LocalView.current
    val context = LocalContext.current

    if (view.isInEditMode) {
        return
    }

    SideEffect {
        val window = context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !colors.isDark
            isAppearanceLightNavigationBars = !colors.isDark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
