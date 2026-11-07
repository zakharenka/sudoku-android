package me.zakharenka.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.StatusKind
import me.zakharenka.sudoku.game.formatTime
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
fun TopToolbar(
    difficulty: Difficulty,
    mistakes: Int,
    elapsedSeconds: Long,
    paused: Boolean,
    enabled: Boolean,
    onTogglePause: () -> Unit,
    onNewGame: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val pauseDescription = stringResource(
        if (paused) R.string.action_resume else R.string.action_pause,
    )
    val newGameDescription = stringResource(R.string.new_game)
    val time = formatTime(elapsedSeconds)
    val statsDescription = stringResource(difficulty.labelRes()) + ", " +
        pluralStringResource(R.plurals.mistakes, mistakes, mistakes) +
        ", " + stringResource(R.string.timer_content_description, time)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SketchSurface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .semantics { contentDescription = statsDescription },
            fill = colors.paperSunken,
            border = colors.gridLine,
            seed = 300,
            shadow = colors.shadow,
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(difficulty.labelRes()),
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable(onClick = onNewGame),
                )

                ToolbarDivider()

                Row(
                    modifier = Modifier.weight(0.8f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "×",
                        color = colors.danger,
                        fontSize = 22.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = mistakes.toString(),
                        color = colors.accent,
                        fontSize = 19.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ToolbarDivider()

                Row(
                    modifier = Modifier.weight(1.4f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SketchIcon(
                        icon = CLOCK,
                        tint = colors.inkSoft,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = time,
                        color = colors.accent,
                        fontSize = 19.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        SketchButton(
            onClick = onTogglePause,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = pauseDescription },
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

        SketchButton(
            onClick = onNewGame,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = newGameDescription },
            fill = colors.paperSunken,
            border = colors.gridLine,
            seed = 302,
            contentPadding = PaddingValues(0.dp),
        ) {
            SketchIcon(
                icon = REFRESH,
                tint = colors.inkSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    val colors = SudokuTheme.colors
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(colors.gridLine),
    )
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
            val description = if (complete) {
                stringResource(R.string.number_complete, number)
            } else {
                stringResource(R.string.number_remaining, number, remaining)
            }

            SketchButton(
                onClick = { onNumber(number) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .semantics { contentDescription = description },
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
            label = stringResource(R.string.action_undo),
            enabled = !locked && state.canUndo,
            modifier = Modifier.weight(1f),
            seed = 500,
            onClick = onUndo,
        )
        ActionButton(
            icon = ERASE,
            label = stringResource(R.string.action_erase),
            enabled = !locked && state.canErase,
            modifier = Modifier.weight(1f),
            seed = 501,
            onClick = onErase,
        )
        ActionButton(
            icon = NOTES,
            label = stringResource(R.string.action_notes),
            enabled = !locked,
            active = state.notesMode,
            modifier = Modifier.weight(1f),
            seed = 502,
            onClick = onNotes,
        )
        ActionButton(
            icon = HINT,
            label = stringResource(R.string.action_hint),
            enabled = !locked && state.hintsRemaining > 0,
            badge = state.hintsRemaining.toString(),
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
    badge: String? = null,
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
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center),
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
            if (badge != null) {
                Text(
                    text = badge,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp),
                )
            }
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
            text = stringResource(R.string.loading_generating),
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
            text = stringResource(R.string.pause_title),
            color = colors.ink,
            fontSize = 32.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.pause_subtitle),
            color = colors.inkSoft,
            fontSize = 14.sp,
            fontFamily = HandFont,
        )
        Spacer(Modifier.height(18.dp))
        OverlayButton(
            text = stringResource(R.string.action_resume),
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
            text = stringResource(R.string.win_title),
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
                text = stringResource(R.string.win_new_record),
                color = colors.accent,
                fontSize = 15.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayButton(
                text = stringResource(R.string.new_game),
                primary = true,
                seed = 601,
                onClick = onNewGame,
            )
            OverlayButton(
                text = stringResource(R.string.win_close),
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

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}
