package me.zakharenka.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.ui.theme.Accent
import me.zakharenka.sudoku.ui.theme.AccentSoft
import me.zakharenka.sudoku.ui.theme.Danger
import me.zakharenka.sudoku.ui.theme.Line
import me.zakharenka.sudoku.ui.theme.LineStrong
import me.zakharenka.sudoku.ui.theme.TextMuted
import me.zakharenka.sudoku.ui.theme.TextPrimary
import me.zakharenka.sudoku.ui.theme.Violet

private val EmptyCells = List(81) { Cell() }

@Composable
fun SudokuBoard(
    cells: List<Cell>,
    selectedIndex: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayCells = if (cells.size == 81) cells else EmptyCells
    val selectedValue = selectedIndex?.let { displayCells.getOrNull(it)?.value } ?: 0

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellSize = maxWidth / 9

        Column(Modifier.fillMaxSize()) {
            repeat(9) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(9) { col ->
                        val index = row * 9 + col
                        val cell = displayCells[index]
                        val isSelected = selectedIndex == index

                        SudokuCell(
                            cell = cell,
                            row = row,
                            col = col,
                            cellSize = cellSize,
                            selected = isSelected,
                            peer = selectedIndex != null &&
                                selectedIndex != index &&
                                arePeers(selectedIndex, index),
                            same = selectedValue != 0 &&
                                cell.value == selectedValue &&
                                selectedIndex != index,
                            enabled = enabled,
                            onClick = { onSelect(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCell(
    cell: Cell,
    row: Int,
    col: Int,
    cellSize: Dp,
    selected: Boolean,
    peer: Boolean,
    same: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        selected && cell.error -> Danger.copy(alpha = 0.18f)
        selected -> Accent.copy(alpha = 0.20f)
        cell.error -> Danger.copy(alpha = 0.14f)
        same -> Violet.copy(alpha = 0.15f)
        peer -> Accent.copy(alpha = 0.07f)
        (row / 3 + col / 3) % 2 == 1 -> Color(0xFF0D1626)
        else -> Color(0xFF0B1220)
    }

    val textColor = when {
        cell.error -> Danger
        selected -> Color(0xFFF0FDFF)
        same -> Color(0xFFDDD6FE)
        cell.given -> TextPrimary
        else -> AccentSoft
    }

    val fontSize = with(LocalDensity.current) { (cellSize * 0.46f).toSp() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(background)
            .drawBehind {
                val thin = 1.dp.toPx()
                val thick = 2.dp.toPx()

                if (col < 8) {
                    val thickLine = col % 3 == 2
                    val width = if (thickLine) thick else thin
                    drawRect(
                        color = if (thickLine) LineStrong else Line,
                        topLeft = Offset(size.width - width, 0f),
                        size = Size(width, size.height),
                    )
                }

                if (row < 8) {
                    val thickLine = row % 3 == 2
                    val height = if (thickLine) thick else thin
                    drawRect(
                        color = if (thickLine) LineStrong else Line,
                        topLeft = Offset(0f, size.height - height),
                        size = Size(size.width, height),
                    )
                }

                if (selected) {
                    val stroke = 2.dp.toPx()
                    drawRect(
                        color = if (cell.error) Danger else Accent,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = cellDescription(row, col, cell)
                role = Role.Button
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.value != 0 -> {
                Text(
                    text = cell.value.toString(),
                    color = textColor,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    lineHeight = fontSize,
                )
            }

            cell.notes.isNotEmpty() -> {
                NotesGrid(
                    notes = cell.notes,
                    cellSize = cellSize,
                )
            }
        }
    }
}

@Composable
private fun NotesGrid(
    notes: Set<Int>,
    cellSize: Dp,
) {
    val fontSize = with(LocalDensity.current) { (cellSize * 0.18f).toSp() }
    val padding = cellSize * 0.04f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        repeat(3) { row ->
            Row(Modifier.weight(1f)) {
                repeat(3) { col ->
                    val number = row * 3 + col + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (number in notes) {
                            Text(
                                text = number.toString(),
                                color = TextMuted.copy(alpha = 0.92f),
                                fontSize = fontSize,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = fontSize,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun arePeers(first: Int, second: Int): Boolean {
    if (first == second) {
        return false
    }

    val firstRow = first / 9
    val firstCol = first % 9
    val secondRow = second / 9
    val secondCol = second % 9

    return firstRow == secondRow ||
        firstCol == secondCol ||
        (firstRow / 3 == secondRow / 3 && firstCol / 3 == secondCol / 3)
}

private fun cellDescription(row: Int, col: Int, cell: Cell): String {
    val builder = StringBuilder("Строка ${row + 1}, столбец ${col + 1}")

    if (cell.given) {
        builder.append(", подсказка")
    }

    when {
        cell.error && cell.value != 0 -> builder.append(", ошибка, цифра ${cell.value}")
        cell.value != 0 -> builder.append(", цифра ${cell.value}")
        cell.notes.isNotEmpty() -> builder.append(", заметки ${cell.notes.sorted().joinToString(" ")}")
        else -> builder.append(", пусто")
    }

    return builder.toString()
}
