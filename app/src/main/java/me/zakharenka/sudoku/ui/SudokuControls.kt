package me.zakharenka.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.ui.sketch.HandwrittenDigit
import me.zakharenka.sudoku.ui.sketch.SketchButton
import me.zakharenka.sudoku.ui.sketch.SketchIcon
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOCK
import me.zakharenka.sudoku.ui.sketch.SketchIcon.ERASE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.FAST_INPUT
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
    showErrors: Boolean,
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
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val timerMinWidth = remember(density, textMeasurer) {
        with(density) {
            textMeasurer.measure(
                text = "00:00",
                style = TextStyle(
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
            ).size.width.toDp() + 4.dp
        }
    }
    val statsDescription = if (showErrors) {
        stringResource(difficulty.labelRes()) + ", " +
            pluralStringResource(R.plurals.mistakes, mistakes, mistakes) +
            ", " + stringResource(R.string.timer_content_description, time)
    } else {
        stringResource(difficulty.labelRes()) + ", " +
            stringResource(R.string.timer_content_description, time)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SketchSurface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .semantics { contentDescription = statsDescription },
            fill = colors.paperSunken,
            border = colors.gridLine,
            seed = 300,
            shadow = colors.shadow,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DifficultyTitle(
                    difficulty = difficulty,
                    onNewGame = onNewGame,
                    modifier = Modifier.weight(1f),
                )

                if (showErrors) {
                    MistakesCounter(mistakes = mistakes)
                    Spacer(Modifier.width(6.dp))
                }

                TimerValue(time = time, minWidth = timerMinWidth)
            }
        }

        SketchButton(
            onClick = onTogglePause,
            modifier = Modifier
                .size(52.dp)
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
                modifier = Modifier.size(28.dp),
            )
        }

        SketchButton(
            onClick = onNewGame,
            modifier = Modifier
                .size(52.dp)
                .semantics { contentDescription = newGameDescription },
            fill = colors.paperSunken,
            border = colors.gridLine,
            seed = 302,
            contentPadding = PaddingValues(0.dp),
        ) {
            SketchIcon(
                icon = REFRESH,
                tint = colors.inkSoft,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private val ToolbarTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
private fun DifficultyTitle(
    difficulty: Difficulty,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors

    BasicText(
        text = stringResource(difficulty.labelRes()),
        style = TextStyle(
            color = colors.ink,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 6.sp,
            maxFontSize = 18.sp,
            stepSize = 1.sp,
        ),
        modifier = modifier.clickable(onClick = onNewGame),
    )
}

@Composable
private fun MistakesCounter(mistakes: Int) {
    val colors = SudokuTheme.colors

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "×",
            color = colors.danger,
            fontSize = 20.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            style = ToolbarTextStyle,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = mistakes.toString(),
            color = colors.accent,
            fontSize = 18.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            style = ToolbarTextStyle,
        )
    }
}

@Composable
private fun TimerValue(time: String, minWidth: Dp) {
    val colors = SudokuTheme.colors

    Row(verticalAlignment = Alignment.CenterVertically) {
        SketchIcon(
            icon = CLOCK,
            tint = colors.inkSoft,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = time,
            color = colors.accent,
            fontSize = 18.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            style = ToolbarTextStyle,
            maxLines = 1,
            modifier = Modifier.widthIn(min = minWidth),
        )
    }
}

@Composable
fun NumberPad(
    cells: List<Cell>,
    enabled: Boolean,
    onNumber: (Int) -> Unit,
    boardCellSize: Dp? = null,
    armedNumber: Int? = null,
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
    val spacing = 4.dp
    val countSize = with(LocalDensity.current) { 14.dp.toSp() }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val buttonWidth = (maxWidth - spacing * 8) / 9
        val cellSize = minOf(boardCellSize ?: (maxWidth / 9), buttonWidth / 0.66f)
        val digitHeight = cellSize * 0.66f
        val buttonHeight = maxOf(52.dp, digitHeight + 20.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            for (number in 1..9) {
                val remaining = (9 - counts[number]).coerceAtLeast(0)
                val complete = remaining == 0
                val armed = armedNumber == number
                val description = when {
                    armed -> stringResource(R.string.number_armed, number)
                    complete -> stringResource(R.string.number_complete, number)
                    else -> stringResource(R.string.number_remaining, number, remaining)
                }

                SketchButton(
                    onClick = { onNumber(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    enabled = enabled && !complete,
                    fill = colors.paperSunken,
                    border = colors.gridLine,
                    active = armed,
                    seed = 400 + number,
                    contentPadding = PaddingValues(2.dp),
                    description = description,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(digitHeight)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center,
                        ) {
                            HandwrittenDigit(
                                value = number,
                                color = colors.accent,
                                seed = 9_000 + number,
                                modifier = Modifier.requiredSize(cellSize),
                                boxFraction = 0.66f,
                            )
                        }
                        Text(
                            text = remaining.toString(),
                            color = colors.ink,
                            fontSize = countSize,
                            fontFamily = HandFont,
                            fontWeight = FontWeight.Bold,
                            lineHeight = countSize * 1.15f,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
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
    onFastInput: () -> Unit,
) {
    val locked = state.loading || state.paused || state.won
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val gap = 6.dp
    val buttonHeight = (54f + (fontScale - 1f) * 18f).dp
    val actions = listOf(
        ActionSpec(
            icon = UNDO,
            label = stringResource(R.string.action_undo),
            enabled = !locked && state.canUndo,
            seed = 500,
            onClick = onUndo,
        ),
        ActionSpec(
            icon = ERASE,
            label = stringResource(R.string.action_erase),
            enabled = !locked && state.canErase,
            seed = 501,
            onClick = onErase,
        ),
        ActionSpec(
            icon = NOTES,
            label = stringResource(R.string.action_notes),
            enabled = !locked,
            active = state.notesMode,
            seed = 502,
            onClick = onNotes,
        ),
        ActionSpec(
            icon = FAST_INPUT,
            label = stringResource(R.string.action_fast_input),
            enabled = !locked,
            active = state.fastInput,
            seed = 504,
            onClick = onFastInput,
        ),
        ActionSpec(
            icon = HINT,
            label = stringResource(R.string.action_hint),
            enabled = !locked && state.hintsRemaining > 0,
            badge = state.hintsRemaining.toString(),
            seed = 503,
            onClick = onHint,
        ),
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 5 else 3
        val actionWidth = (maxWidth - gap * (columns - 1)) / columns

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        gap,
                        Alignment.CenterHorizontally,
                    ),
                ) {
                    rowActions.forEach { action ->
                        ActionButton(
                            icon = action.icon,
                            label = action.label,
                            enabled = action.enabled,
                            height = buttonHeight,
                            modifier = Modifier.width(actionWidth),
                            active = action.active,
                            badge = action.badge,
                            seed = action.seed,
                            onClick = action.onClick,
                        )
                    }
                }
            }
        }
    }
}

private data class ActionSpec(
    val icon: SketchIcon,
    val label: String,
    val enabled: Boolean,
    val active: Boolean = false,
    val badge: String? = null,
    val seed: Int,
    val onClick: () -> Unit,
)

@Composable
private fun ActionButton(
    icon: SketchIcon,
    label: String,
    enabled: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    badge: String? = null,
    seed: Int,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val contentColor = if (active) colors.accent else colors.inkSoft
    val badgeFontSize = with(LocalDensity.current) { 14.dp.toSp() }

    SketchButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        fill = colors.paperSunken,
        border = colors.gridLine,
        active = active,
        seed = seed,
        contentPadding = PaddingValues(2.dp),
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
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = TextStyle(
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 14.sp,
                        stepSize = 0.5.sp,
                    ),
                )
            }
            if (badge != null) {
                Text(
                    text = badge,
                    color = colors.ink,
                    fontSize = badgeFontSize,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 1.dp, end = 3.dp),
                )
            }
        }
    }
}

@Composable
fun LoadingOverlay() {
    val colors = SudokuTheme.colors

    BoardOverlay {
        SketchSpinner(
            color = colors.accent,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.loading_generating),
            color = colors.inkSoft,
            fontSize = 19.sp,
            fontFamily = HandFont,
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit) {
    val colors = SudokuTheme.colors

    BoardOverlay {
        BasicText(
            text = stringResource(R.string.pause_title),
            style = TextStyle(
                color = colors.ink,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 36.sp,
                stepSize = 1.sp,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.pause_subtitle),
            color = colors.inkSoft,
            fontSize = 16.sp,
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
            modifier = Modifier.size(50.dp),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = stringResource(R.string.win_title),
            style = TextStyle(
                color = colors.success,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 38.sp,
                stepSize = 1.sp,
            ),
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = formatTime(elapsedSeconds),
            style = TextStyle(
                color = colors.ink,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 20.sp,
                maxFontSize = 28.sp,
                stepSize = 1.sp,
            ),
        )
        if (isNewRecord) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.win_new_record),
                color = colors.accent,
                fontSize = 17.sp,
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
            .heightIn(min = 48.dp)
            .widthIn(min = 120.dp),
        fill = if (primary) colors.accentWash else colors.paperSunken,
        border = if (primary) colors.accent else colors.gridLine,
        active = primary,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (primary) colors.accent else colors.inkSoft,
            fontSize = 18.sp,
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
