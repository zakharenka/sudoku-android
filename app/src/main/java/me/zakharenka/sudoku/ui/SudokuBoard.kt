package me.zakharenka.sudoku.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.ui.sketch.sketchFill
import me.zakharenka.sudoku.ui.sketch.sketchLine
import me.zakharenka.sudoku.ui.sketch.sketchRect
import me.zakharenka.sudoku.ui.theme.HandFont
import me.zakharenka.sudoku.ui.theme.SudokuTheme

private val EmptyCells = List(81) { Cell() }

@Composable
fun SudokuBoard(
    cells: List<Cell>,
    selectedIndex: Int?,
    straightGrid: Boolean,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors
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

                        SudokuCell(
                            cell = cell,
                            row = row,
                            col = col,
                            cellSize = cellSize,
                            selected = selectedIndex == index,
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

        Canvas(Modifier.fillMaxSize()) {
            val cellPx = size.width / 9f

            for (index in 1..8) {
                val thick = index % 3 == 0
                val color = if (thick) colors.gridLineStrong else colors.gridLine
                val width = if (thick) 2.dp.toPx() else 1.dp.toPx()
                val offset = index * cellPx

                if (straightGrid) {
                    drawLine(
                        color = color,
                        start = Offset(offset, 0f),
                        end = Offset(offset, size.height),
                        strokeWidth = width,
                    )
                    drawLine(
                        color = color,
                        start = Offset(0f, offset),
                        end = Offset(size.width, offset),
                        strokeWidth = width,
                    )
                } else {
                    sketchLine(
                        start = Offset(offset, 0f),
                        end = Offset(offset, size.height),
                        color = color,
                        strokeWidth = width,
                        seed = 1_000 + index,
                        wobble = 1.dp.toPx(),
                    )
                    sketchLine(
                        start = Offset(0f, offset),
                        end = Offset(size.width, offset),
                        color = color,
                        strokeWidth = width,
                        seed = 2_000 + index,
                        wobble = 1.dp.toPx(),
                    )
                }
            }

            if (selectedIndex != null && selectedIndex in 0..80) {
                val row = selectedIndex / 9
                val col = selectedIndex % 9
                val cell = displayCells[selectedIndex]
                val inset = 1.5.dp.toPx()
                val topLeft = Offset(col * cellPx + inset, row * cellPx + inset)
                val rectSize = Size(cellPx - inset * 2f, cellPx - inset * 2f)
                val color = if (cell.error) colors.danger else colors.accent

                if (straightGrid) {
                    drawRect(
                        color = color,
                        topLeft = topLeft,
                        size = rectSize,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                } else {
                    sketchRect(
                        topLeft = topLeft,
                        size = rectSize,
                        color = color,
                        strokeWidth = 2.dp.toPx(),
                        seed = 3_000 + selectedIndex,
                        wobble = 1.dp.toPx(),
                    )
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
    val colors = SudokuTheme.colors
    val wash = when {
        selected && cell.error -> colors.dangerWash
        selected -> colors.accentWash
        cell.error -> colors.dangerWash
        same -> colors.highlight
        peer -> colors.accentWash.copy(alpha = 0.45f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    val textColor = when {
        cell.error -> colors.danger
        selected -> colors.ink
        same -> colors.ink
        cell.given -> colors.ink
        else -> colors.accent
    }

    val fontSize = with(LocalDensity.current) { (cellSize * 0.5f).toSp() }
    val interactionSource = remember { MutableInteractionSource() }
    val description = when {
        cell.error && cell.value != 0 -> stringResource(
            R.string.cell_error,
            row + 1,
            col + 1,
            cell.value,
        )
        cell.value != 0 && cell.given -> stringResource(
            R.string.cell_given,
            row + 1,
            col + 1,
            cell.value,
        )
        cell.value != 0 -> stringResource(
            R.string.cell_value,
            row + 1,
            col + 1,
            cell.value,
        )
        cell.notes.isNotEmpty() -> stringResource(
            R.string.cell_notes,
            row + 1,
            col + 1,
            cell.notes.sorted().joinToString(" "),
        )
        else -> stringResource(R.string.cell_empty, row + 1, col + 1)
    }

    Box(
        modifier = modifier
            .drawBehind {
                if (wash.alpha > 0f) {
                    sketchFill(
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height),
                        color = wash,
                        seed = 5_000 + row * 9 + col,
                        wobble = 1.dp.toPx(),
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
                contentDescription = description
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
                    fontFamily = HandFont,
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
    val colors = SudokuTheme.colors
    val fontSize = with(LocalDensity.current) { (cellSize * 0.2f).toSp() }
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
                                color = colors.inkSoft,
                                fontSize = fontSize,
                                fontFamily = HandFont,
                                fontWeight = FontWeight.Normal,
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
