package me.zakharenka.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.ui.sketch.HandwrittenDigit
import me.zakharenka.sudoku.ui.sketch.sketchFill
import me.zakharenka.sudoku.ui.sketch.sketchLine
import me.zakharenka.sudoku.ui.sketch.sketchRect
import me.zakharenka.sudoku.ui.theme.SudokuTheme

/** Stroke width of the outer board frame. */
private val BoardFrameStroke = 2.dp

private val EmptyCells = List(81) { Cell() }

@Composable
fun SudokuBoard(
    cells: List<Cell>,
    selectedIndex: Int?,
    straightGrid: Boolean,
    highlightSameDigits: Boolean,
    armedNumber: Int?,
    gameId: Long,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors
    val displayCells = if (cells.size == 81) cells else EmptyCells
    val selectedValue = selectedIndex?.let { displayCells.getOrNull(it)?.value } ?: 0
    // In fast mode the armed digit drives the highlight immediately, so the
    // player sees every matching digit before tapping the board.
    val highlightedDigit = armedNumber ?: selectedValue

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellSize = maxWidth / 9

        Column(Modifier.fillMaxSize()) {
            repeat(9) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(9) { col ->
                        val index = row * 9 + col
                        val cell = displayCells[index]

                        key(gameId, cells.size, index) {
                            SudokuCell(
                                cell = cell,
                                row = row,
                                col = col,
                                cellSize = cellSize,
                                selected = selectedIndex == index,
                                peer = selectedIndex != null &&
                                    selectedIndex != index &&
                                    arePeers(selectedIndex, index),
                                same = highlightSameDigits &&
                                    highlightedDigit != 0 &&
                                    selectedIndex != index &&
                                    cell.value == highlightedDigit,
                                highlightedNote = highlightedDigit.takeIf {
                                    highlightSameDigits &&
                                        it != 0 &&
                                        cell.notes.contains(it)
                                },
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
        }
    }
}

/**
 * Outer square frame of the board. It is drawn on top of the cells so the grid
 * lines always meet it, and it stays visible above the pause and win overlays.
 */
@Composable
internal fun SudokuBoardFrame(
    straightGrid: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors

    Canvas(modifier.fillMaxSize()) {
        val stroke = BoardFrameStroke.toPx()
        // Keep the frame centered on the outer grid line, exactly like the inner
        // lines. Otherwise it eats into the edge cells and hides their insets.
        val topLeft = Offset(0f, 0f)
        val frameSize = Size(size.width, size.height)

        if (straightGrid) {
            drawRect(
                color = colors.gridLineStrong,
                topLeft = topLeft,
                size = frameSize,
                style = Stroke(width = stroke),
            )
        } else {
            sketchRect(
                topLeft = topLeft,
                size = frameSize,
                color = colors.gridLineStrong,
                strokeWidth = stroke,
                seed = 20,
                wobble = 1.1.dp.toPx(),
            )
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
    highlightedNote: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors
    val wash = when {
        selected && cell.error -> colors.dangerWash
        selected -> colors.accentSoft.copy(alpha = 0.32f)
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

                if (selected) {
                    val hatch = if (cell.error) {
                        colors.danger.copy(alpha = 0.18f)
                    } else {
                        colors.accent.copy(alpha = 0.20f)
                    }
                    val side = minOf(size.width, size.height)

                    repeat(3) { index ->
                        val offset = side * (1f + index * 2f) / 3f
                        val start = if (offset <= size.height) {
                            Offset(0f, offset)
                        } else {
                            Offset(offset - size.height, size.height)
                        }
                        val end = if (offset <= size.width) {
                            Offset(offset, 0f)
                        } else {
                            Offset(size.width, offset - size.width)
                        }

                        sketchLine(
                            start = start,
                            end = end,
                            color = hatch,
                            strokeWidth = 1.dp.toPx(),
                            seed = 12_000 + row * 9 + col + index,
                            wobble = 0.5.dp.toPx(),
                            passes = 1,
                        )
                    }
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
                if (selected) {
                    liveRegion = LiveRegionMode.Polite
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HandwrittenDigit(
            value = cell.value,
            color = textColor,
            seed = 7_000 + row * 9 + col,
            modifier = Modifier.fillMaxSize(),
        )

        if (cell.value == 0 && cell.notes.isNotEmpty()) {
            NotesGrid(
                notes = cell.notes,
                color = textColor,
                highlightedNote = highlightedNote,
                seed = 8_000 + row * 9 + col,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

/**
 * Notes are shown as a 3×3 mini-grid inside the cell: each candidate digit
 * keeps its standard position, from 1 in the top-left to 9 in the bottom-right.
 * A highlighted note paints only its own mini-cell, not the whole board cell.
 */
@Composable
private fun NotesGrid(
    notes: Set<Int>,
    color: Color,
    highlightedNote: Int?,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp),
    ) {
        for (row in 0..2) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for (col in 0..2) {
                    val digit = row * 3 + col + 1
                    val highlighted = digit == highlightedNote && digit in notes
                    val digitSeed = seed + digit

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (highlighted) {
                                    colors.highlight
                                } else {
                                    Color.Transparent
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val digitSize = minOf(maxWidth, maxHeight) * 0.88f

                        if (digit in notes) {
                            HandwrittenDigit(
                                value = digit,
                                color = if (highlighted) colors.ink else color,
                                seed = digitSeed,
                                modifier = Modifier.size(digitSize),
                                fitToBounds = true,
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
