package me.zakharenka.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.StatusKind
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.game.pluralMistakes
import me.zakharenka.sudoku.ui.sketch.SketchButton
import me.zakharenka.sudoku.ui.sketch.SketchIcon
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOCK
import me.zakharenka.sudoku.ui.sketch.SketchIcon.ERASE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.HINT
import me.zakharenka.sudoku.ui.sketch.SketchIcon.NOTES
import me.zakharenka.sudoku.ui.sketch.SketchIcon.PAUSE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.PLAY
import me.zakharenka.sudoku.ui.sketch.SketchIcon.REFRESH
import me.zakharenka.sudoku.ui.sketch.SketchIcon.TROPHY
import me.zakharenka.sudoku.ui.sketch.SketchIcon.UNDO
import me.zakharenka.sudoku.ui.sketch.SketchSpinner
import me.zakharenka.sudoku.ui.sketch.SketchSurface
import me.zakharenka.sudoku.ui.theme.HandFont
import me.zakharenka.sudoku.ui.theme.SudokuTheme

@Composable
fun DifficultySelector(
    selected: Difficulty,
    enabled: Boolean,
    onSelect: (Difficulty) -> Unit,
) {
    val colors = SudokuTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Difficulty.entries.forEachIndexed { index, difficulty ->
            val active = difficulty == selected

            SketchButton(
                onClick = { onSelect(difficulty) },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                enabled = enabled,
                fill = colors.paperSunken,
                border = colors.gridLine,
                active = active,
                seed = 100 + index,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = difficulty.label,
                        color = if (active) colors.accent else colors.ink,
                        fontSize = 15.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = difficulty.note,
                        color = if (active) colors.accentSoft else colors.inkSoft,
                        fontSize = 10.sp,
                        fontFamily = HandFont,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun NewGameButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        enabled = enabled,
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = 200,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SketchIcon(
                icon = REFRESH,
                tint = colors.inkSoft,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Новая игра",
                color = colors.inkSoft,
                fontSize = 16.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun Hud(
    mistakes: Int,
    elapsedSeconds: Long,
    paused: Boolean,
    enabled: Boolean,
    onTogglePause: () -> Unit,
) {
    val colors = SudokuTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            HudStat(
                icon = {
                    Text(
                        text = "×",
                        color = colors.danger,
                        fontSize = 26.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    )
                },
                value = mistakes.toString(),
                description = "$mistakes ${pluralMistakes(mistakes)}",
                seed = 300,
            )
        }

        SketchButton(
            onClick = onTogglePause,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = if (paused) "Продолжить" else "Пауза"
                },
            enabled = enabled,
            fill = colors.paperSunken,
            border = colors.accent,
            seed = 301,
            contentPadding = PaddingValues(0.dp),
        ) {
            SketchIcon(
                icon = if (paused) PLAY else PAUSE,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            HudStat(
                icon = {
                    SketchIcon(
                        icon = CLOCK,
                        tint = colors.inkSoft,
                        modifier = Modifier.size(18.dp),
                    )
                },
                value = formatTime(elapsedSeconds),
                description = "Время: ${formatTime(elapsedSeconds)}",
                seed = 302,
            )
        }
    }
}

@Composable
private fun HudStat(
    icon: @Composable () -> Unit,
    value: String,
    description: String,
    seed: Int,
) {
    val colors = SudokuTheme.colors

    SketchSurface(
        modifier = Modifier
            .height(46.dp)
            .widthIn(min = 104.dp)
            .semantics { contentDescription = description },
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = seed,
        shadow = colors.shadow,
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon()
            Text(
                text = value,
                color = colors.accent,
                fontSize = 20.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
fun NumberPad(
    cells: List<Cell>,
    enabled: Boolean,
    onNumber: (Int) -> Unit,
) {
    val colors = SudokuTheme.colors
    val counts = remember(cells) {
        val result = IntArray(10)
        cells.forEach { cell ->
            if (cell.value in 1..9) {
                result[cell.value] += 1
            }
        }
        result
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (number in 1..9) {
            val remaining = (9 - counts[number]).coerceAtLeast(0)
            val complete = remaining == 0

            SketchButton(
                onClick = { onNumber(number) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .semantics {
                        contentDescription = if (complete) {
                            "Цифра $number, все расставлены"
                        } else {
                            "Цифра $number, осталось $remaining"
                        }
                    },
                enabled = enabled && !complete,
                fill = colors.paperSunken,
                border = colors.gridLine,
                seed = 400 + number,
                contentPadding = PaddingValues(2.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = number.toString(),
                        color = colors.accent,
                        fontSize = 23.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Text(
                        text = remaining.toString(),
                        color = colors.inkSoft,
                        fontSize = 10.sp,
                        fontFamily = HandFont,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ActionRow(
    state: GameUiState,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onHint: () -> Unit,
) {
    val locked = state.loading || state.paused || state.won

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ActionButton(
            icon = UNDO,
            label = "Отменить",
            enabled = !locked && state.canUndo,
            modifier = Modifier.weight(1f),
            seed = 500,
            onClick = onUndo,
        )
        ActionButton(
            icon = ERASE,
            label = "Стереть",
            enabled = !locked && state.canErase,
            modifier = Modifier.weight(1f),
            seed = 501,
            onClick = onErase,
        )
        ActionButton(
            icon = NOTES,
            label = "Заметки",
            enabled = !locked,
            active = state.notesMode,
            modifier = Modifier.weight(1f),
            seed = 502,
            onClick = onNotes,
        )
        ActionButton(
            icon = HINT,
            label = "Подсказка",
            enabled = !locked,
            modifier = Modifier.weight(1f),
            seed = 503,
            onClick = onHint,
        )
    }
}

@Composable
private fun ActionButton(
    icon: SketchIcon,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    seed: Int,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val contentColor = if (active) colors.accent else colors.inkSoft

    SketchButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        enabled = enabled,
        fill = colors.paperSunken,
        border = colors.gridLine,
        active = active,
        seed = seed,
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SketchIcon(
                icon = icon,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun StatusText(
    status: String,
    kind: StatusKind,
) {
    val colors = SudokuTheme.colors

    Text(
        text = status,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = when (kind) {
            StatusKind.ERROR -> colors.danger
            StatusKind.SUCCESS -> colors.success
            StatusKind.NEUTRAL -> colors.inkSoft
        },
        fontSize = 15.sp,
        fontFamily = HandFont,
        fontWeight = if (kind == StatusKind.SUCCESS) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        minLines = 1,
        maxLines = 2,
    )
}

@Composable
fun LoadingOverlay() {
    val colors = SudokuTheme.colors

    BoardOverlay {
        SketchSpinner(
            color = colors.accent,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Генерация поля…",
            color = colors.inkSoft,
            fontSize = 17.sp,
            fontFamily = HandFont,
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit) {
    val colors = SudokuTheme.colors

    BoardOverlay {
        Text(
            text = "Пауза",
            color = colors.ink,
            fontSize = 32.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Таймер остановлен",
            color = colors.inkSoft,
            fontSize = 14.sp,
            fontFamily = HandFont,
        )
        Spacer(Modifier.height(18.dp))
        OverlayButton(
            text = "Продолжить",
            primary = true,
            seed = 600,
            onClick = onResume,
        )
    }
}

@Composable
fun WinOverlay(
    elapsedSeconds: Long,
    isNewRecord: Boolean,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors

    BoardOverlay {
        SketchIcon(
            icon = TROPHY,
            tint = colors.accent,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Решено!",
            color = colors.success,
            fontSize = 34.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatTime(elapsedSeconds),
            color = colors.ink,
            fontSize = 24.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        if (isNewRecord) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Новый рекорд!",
                color = colors.accent,
                fontSize = 15.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayButton(
                text = "Новая игра",
                primary = true,
                seed = 601,
                onClick = onNewGame,
            )
            OverlayButton(
                text = "Закрыть",
                primary = false,
                seed = 602,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun BoardOverlay(content: @Composable ColumnScope.() -> Unit) {
    val colors = SudokuTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
fun OverlayButton(
    text: String,
    primary: Boolean,
    seed: Int,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 120.dp),
        fill = if (primary) colors.accentWash else colors.paperSunken,
        border = if (primary) colors.accent else colors.gridLine,
        active = primary,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            color = if (primary) colors.accent else colors.inkSoft,
            fontSize = 16.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
    }
}
