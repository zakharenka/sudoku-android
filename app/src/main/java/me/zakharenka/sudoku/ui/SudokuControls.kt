package me.zakharenka.sudoku.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
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
import me.zakharenka.sudoku.game.pluralMistakes
import me.zakharenka.sudoku.ui.theme.Accent
import me.zakharenka.sudoku.ui.theme.AccentSoft
import me.zakharenka.sudoku.ui.theme.Amber
import me.zakharenka.sudoku.ui.theme.Danger
import me.zakharenka.sudoku.ui.theme.Line
import me.zakharenka.sudoku.ui.theme.LineStrong
import me.zakharenka.sudoku.ui.theme.PanelDeep
import me.zakharenka.sudoku.ui.theme.Success
import me.zakharenka.sudoku.ui.theme.TextMuted
import me.zakharenka.sudoku.ui.theme.TextPrimary

@Composable
fun DifficultySelector(
    selected: Difficulty,
    enabled: Boolean,
    onSelect: (Difficulty) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelDeep.copy(alpha = 0.55f))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        Difficulty.entries.forEach { difficulty ->
            val active = difficulty == selected
            val shape = RoundedCornerShape(13.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(
                        if (active) {
                            Modifier
                                .background(Accent.copy(alpha = 0.10f))
                                .border(1.dp, Accent.copy(alpha = 0.35f), shape)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(
                        selected = active,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = { onSelect(difficulty) },
                    )
                    .padding(vertical = 7.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = difficulty.label,
                    color = if (active) AccentSoft else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = difficulty.note,
                    color = if (active) {
                        Accent.copy(alpha = 0.8f)
                    } else {
                        TextMuted.copy(alpha = 0.55f)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun NewGameButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(PanelDeep.copy(alpha = 0.5f))
            .border(1.dp, LineStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.55f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Новая игра",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
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
                        color = Danger,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                value = mistakes.toString(),
                description = "$mistakes ${pluralMistakes(mistakes)}",
            )
        }

        PauseButton(
            paused = paused,
            enabled = enabled,
            onClick = onTogglePause,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            HudStat(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_clock),
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                },
                value = formatTime(elapsedSeconds),
                description = "Время: ${formatTime(elapsedSeconds)}",
            )
        }
    }
}

@Composable
private fun HudStat(
    icon: @Composable () -> Unit,
    value: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDeep)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = value,
            color = AccentSoft,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun PauseButton(
    paused: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Accent.copy(alpha = 0.16f),
                        Accent.copy(alpha = 0.05f),
                    ),
                ),
            )
            .border(1.dp, Accent.copy(alpha = 0.38f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause),
            contentDescription = if (paused) "Продолжить" else "Пауза",
            tint = AccentSoft,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun NumberPad(
    cells: List<Cell>,
    enabled: Boolean,
    onNumber: (Int) -> Unit,
) {
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

            PadButton(
                number = number,
                remaining = remaining,
                enabled = enabled && remaining > 0,
                modifier = Modifier.weight(1f),
                onClick = { onNumber(number) },
            )
        }
    }
}

@Composable
private fun PadButton(
    number: Int,
    remaining: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val complete = remaining == 0

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E2939),
                        Color(0xFF0F172A),
                    ),
                ),
            )
            .border(1.dp, Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (complete) 0.42f else 1f)
            .semantics {
                contentDescription = if (complete) {
                    "Цифра $number, все расставлены"
                } else {
                    "Цифра $number, осталось $remaining"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = AccentSoft,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = remaining.toString(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 5.dp),
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
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
            icon = R.drawable.ic_undo,
            label = "Отменить",
            enabled = !locked && state.canUndo,
            modifier = Modifier.weight(1f),
            onClick = onUndo,
        )
        ActionButton(
            icon = R.drawable.ic_erase,
            label = "Стереть",
            enabled = !locked && state.canErase,
            modifier = Modifier.weight(1f),
            onClick = onErase,
        )
        ActionButton(
            icon = R.drawable.ic_notes,
            label = "Заметки",
            enabled = !locked,
            active = state.notesMode,
            modifier = Modifier.weight(1f),
            onClick = onNotes,
        )
        ActionButton(
            icon = R.drawable.ic_hint,
            label = "Подсказка",
            enabled = !locked,
            modifier = Modifier.weight(1f),
            onClick = onHint,
        )
    }
}

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val contentColor = if (active) Color(0xFFCFFAFE) else TextMuted

    Column(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(if (active) Accent.copy(alpha = 0.13f) else PanelDeep.copy(alpha = 0.40f))
            .border(1.dp, if (active) Accent.copy(alpha = 0.42f) else Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (active) AccentSoft else contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun StatusText(
    status: String,
    kind: StatusKind,
) {
    Text(
        text = status,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = when (kind) {
            StatusKind.ERROR -> Danger
            StatusKind.SUCCESS -> Success
            StatusKind.NEUTRAL -> TextMuted
        },
        fontSize = 13.sp,
        fontWeight = if (kind == StatusKind.SUCCESS) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        minLines = 1,
        maxLines = 2,
    )
}

@Composable
fun LoadingOverlay() {
    BoardOverlay {
        CircularProgressIndicator(
            color = Accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Генерация поля…",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit) {
    BoardOverlay {
        Text(
            text = "Пауза",
            color = AccentSoft,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(14.dp))
        OverlayButton(
            text = "Продолжить",
            primary = true,
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
    BoardOverlay {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Решено!",
            color = Success,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatTime(elapsedSeconds),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
        if (isNewRecord) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Новый рекорд!",
                color = Amber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverlayButton(
                text = "Новая игра",
                primary = true,
                onClick = onNewGame,
            )
            OverlayButton(
                text = "Закрыть",
                primary = false,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun BoardOverlay(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14)),
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
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .background(if (primary) Accent.copy(alpha = 0.18f) else PanelDeep.copy(alpha = 0.5f))
            .border(1.dp, if (primary) Accent.copy(alpha = 0.5f) else Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color(0xFFCFFAFE) else TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
