package me.zakharenka.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.SudokuViewModel
import me.zakharenka.sudoku.data.ThemeMode
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.StatusKind
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.ui.sketch.SketchButton
import me.zakharenka.sudoku.ui.sketch.SketchIcon
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOSE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.INFO
import me.zakharenka.sudoku.ui.sketch.SketchIcon.SETTINGS
import me.zakharenka.sudoku.ui.sketch.SketchIcon.TROPHY
import me.zakharenka.sudoku.ui.sketch.SketchSurface
import me.zakharenka.sudoku.ui.sketch.paperTexture
import me.zakharenka.sudoku.ui.sketch.sketchFill
import me.zakharenka.sudoku.ui.sketch.sketchLine
import me.zakharenka.sudoku.ui.sketch.sketchOval
import me.zakharenka.sudoku.ui.sketch.sketchRect
import me.zakharenka.sudoku.ui.theme.HandFont
import me.zakharenka.sudoku.ui.theme.SudokuTheme

@Composable
fun SudokuScreen(viewModel: SudokuViewModel = viewModel()) {
    val colors = SudokuTheme.colors
    val state = viewModel.uiState

    var showRecords by rememberSaveable { mutableStateOf(false) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var dismissedGameId by rememberSaveable { mutableStateOf<Long?>(null) }
    val winDismissed = dismissedGameId == state.gameId

    BackHandler(enabled = state.paused) {
        viewModel.resume()
    }

    BackHandler(enabled = state.won && !winDismissed) {
        dismissedGameId = state.gameId
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            paperTexture(
                base = colors.paper,
                speckle = colors.paperSpeckle,
                seed = 7,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                onRules = { showRules = true },
                onRecords = { showRecords = true },
                onSettings = { showSettings = true },
            )

            Spacer(Modifier.height(10.dp))

            GamePanel(
                state = state,
                winDismissed = winDismissed,
                onDismissWin = { dismissedGameId = state.gameId },
                viewModel = viewModel,
            )
        }
    }

    if (showRecords) {
        RecordsDialog(
            bestTimes = state.bestTimes,
            onDismiss = { showRecords = false },
        )
    }

    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }

    if (showSettings) {
        SettingsDialog(
            themeMode = viewModel.themeMode,
            onThemeModeChange = viewModel::updateThemeMode,
            onRules = {
                showSettings = false
                showRules = true
            },
            onRecords = {
                showSettings = false
                showRecords = true
            },
            onResetRecords = viewModel::clearBestTimes,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun GamePanel(
    state: GameUiState,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    viewModel: SudokuViewModel,
) {
    val colors = SudokuTheme.colors

    SketchSurface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        fill = colors.paperAlt,
        border = colors.inkSoft,
        strokeWidth = 1.6.dp,
        seed = 10,
        shadow = colors.shadow,
        contentPadding = PaddingValues(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.fillMaxWidth()) {
            DifficultySelector(
                selected = state.difficulty,
                enabled = !state.loading,
                onSelect = viewModel::selectDifficulty,
            )

            Spacer(Modifier.height(8.dp))

            NewGameButton(
                enabled = !state.loading,
                onClick = { viewModel.newGame() },
            )

            Spacer(Modifier.height(10.dp))

            Hud(
                mistakes = state.mistakes,
                elapsedSeconds = state.elapsedSeconds,
                paused = state.paused,
                enabled = !state.loading && !state.won,
                onTogglePause = viewModel::togglePause,
            )

            Spacer(Modifier.height(10.dp))

            BoardBox(
                state = state,
                winDismissed = winDismissed,
                onDismissWin = onDismissWin,
                onSelect = viewModel::select,
                onResume = viewModel::resume,
                onNewGame = { viewModel.newGame() },
            )

            Spacer(Modifier.height(8.dp))

            NumberPad(
                cells = state.cells,
                enabled = !state.loading && !state.paused && !state.won,
                onNumber = viewModel::enter,
            )

            Spacer(Modifier.height(8.dp))

            ActionRow(
                state = state,
                onUndo = viewModel::undo,
                onErase = viewModel::erase,
                onNotes = viewModel::toggleNotes,
                onHint = viewModel::hint,
            )

            StatusText(
                status = state.status,
                kind = state.statusKind,
            )
        }
    }
}

@Composable
private fun BoardBox(
    state: GameUiState,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    onSelect: (Int) -> Unit,
    onResume: () -> Unit,
    onNewGame: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchSurface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        fill = colors.paperAlt,
        border = colors.gridLineStrong,
        strokeWidth = 2.dp,
        seed = 20,
        shadow = colors.shadow,
        contentPadding = PaddingValues(4.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(Modifier.fillMaxSize()) {
            SudokuBoard(
                cells = state.cells,
                selectedIndex = state.selectedIndex,
                enabled = !state.loading && !state.paused && !state.won,
                onSelect = onSelect,
            )

            when {
                state.loading -> LoadingOverlay()

                state.paused -> PauseOverlay(onResume = onResume)

                state.won && !winDismissed -> WinOverlay(
                    elapsedSeconds = state.elapsedSeconds,
                    isNewRecord = state.isNewRecord,
                    onNewGame = onNewGame,
                    onDismiss = onDismissWin,
                )
            }
        }
    }
}

@Composable
private fun Header(
    onRules: () -> Unit,
    onRecords: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = SudokuTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SketchLogo(52.dp)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Судоку",
                color = colors.ink,
                fontSize = 34.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
            )
            Text(
                text = "Логика без спешки",
                color = colors.inkSoft,
                fontSize = 14.sp,
                fontFamily = HandFont,
            )
        }

        HeaderIconButton(
            icon = INFO,
            description = "Правила",
            seed = 700,
            onClick = onRules,
        )

        Spacer(Modifier.width(4.dp))

        HeaderIconButton(
            icon = TROPHY,
            description = "Рекорды",
            seed = 701,
            onClick = onRecords,
        )

        Spacer(Modifier.width(4.dp))

        HeaderIconButton(
            icon = SETTINGS,
            description = "Настройки",
            seed = 702,
            onClick = onSettings,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: SketchIcon,
    description: String,
    seed: Int,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description },
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = seed,
        contentPadding = PaddingValues(0.dp),
    ) {
        SketchIcon(
            icon = icon,
            tint = colors.inkSoft,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SketchLogo(size: Dp) {
    val colors = SudokuTheme.colors

    SketchSurface(
        modifier = Modifier.size(size),
        fill = colors.accentWash,
        border = colors.ink,
        strokeWidth = 1.6.dp,
        seed = 710,
        shadow = colors.shadow,
        contentPadding = PaddingValues(0.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            val cell = this.size.width / 3f

            for (index in 1..2) {
                val offset = index * cell
                sketchLine(
                    start = Offset(offset, 0f),
                    end = Offset(offset, this.size.height),
                    color = colors.ink,
                    strokeWidth = 1.dp.toPx(),
                    seed = 720 + index,
                    wobble = 0.7.dp.toPx(),
                )
                sketchLine(
                    start = Offset(0f, offset),
                    end = Offset(this.size.width, offset),
                    color = colors.ink,
                    strokeWidth = 1.dp.toPx(),
                    seed = 730 + index,
                    wobble = 0.7.dp.toPx(),
                )
            }

            sketchFill(
                topLeft = Offset(cell, cell),
                size = Size(cell, cell),
                color = colors.paperAlt,
                seed = 740,
                wobble = 0.8.dp.toPx(),
            )
            sketchRect(
                topLeft = Offset(cell, cell),
                size = Size(cell, cell),
                color = colors.ink,
                strokeWidth = 1.2.dp.toPx(),
                seed = 741,
                wobble = 0.7.dp.toPx(),
            )
        }

        Text(
            text = "9",
            color = colors.ink,
            fontSize = 22.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SketchDialog(
    onDismiss: () -> Unit,
    seed: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SudokuTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            SketchSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                fill = colors.paperAlt,
                border = colors.ink,
                strokeWidth = 2.dp,
                seed = seed,
                shadow = colors.shadow,
                contentPadding = PaddingValues(18.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    seed: Int,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.ink,
            fontSize = 28.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        SketchButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = "Закрыть" },
            fill = colors.paperSunken,
            border = colors.gridLine,
            seed = seed,
            contentPadding = PaddingValues(0.dp),
        ) {
            SketchIcon(
                icon = CLOSE,
                tint = colors.inkSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RecordsDialog(
    bestTimes: Map<Difficulty, Long?>,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchDialog(onDismiss = onDismiss, seed = 800) {
        DialogHeader(
            title = "Лучшие результаты",
            seed = 801,
            onDismiss = onDismiss,
        )

        Spacer(Modifier.height(12.dp))

        Difficulty.entries.forEach { difficulty ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = difficulty.label,
                    color = colors.inkSoft,
                    fontSize = 18.sp,
                    fontFamily = HandFont,
                )
                Text(
                    text = bestTimes[difficulty]?.let(::formatTime) ?: "—",
                    color = colors.accent,
                    fontSize = 20.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Рекорды хранятся только на этом устройстве.",
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
        )
    }
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    val colors = SudokuTheme.colors

    SketchDialog(onDismiss = onDismiss, seed = 810) {
        DialogHeader(
            title = "Как играть",
            seed = 811,
            onDismiss = onDismiss,
        )

        Spacer(Modifier.height(12.dp))

        RuleLine("1–9 — вписать цифру в выбранную клетку")
        RuleLine("Заметки — записывать возможные варианты в пустой клетке")
        RuleLine("Пауза — остановить таймер и скрыть поле")
        RuleLine("Подсказка — открыть правильную цифру")

        Spacer(Modifier.height(10.dp))

        Text(
            text = "В каждой строке, каждом столбце и каждом блоке 3 × 3 цифры от 1 до 9 должны встречаться ровно один раз. Ошибки подсвечиваются красным.",
            color = colors.inkSoft,
            fontSize = 15.sp,
            fontFamily = HandFont,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun RuleLine(text: String) {
    val colors = SudokuTheme.colors

    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "•",
            color = colors.accent,
            fontSize = 18.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = colors.ink,
            fontSize = 16.sp,
            fontFamily = HandFont,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun SettingsDialog(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRules: () -> Unit,
    onRecords: () -> Unit,
    onResetRecords: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }

    SketchDialog(onDismiss = onDismiss, seed = 820) {
        DialogHeader(
            title = "Настройки",
            seed = 821,
            onDismiss = onDismiss,
        )

        SectionLabel("Тема")

        ThemeMode.entries.forEachIndexed { index, mode ->
            val active = themeMode == mode

            SketchButton(
                onClick = { onThemeModeChange(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                active = active,
                seed = 830 + index,
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeRadio(active = active)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = mode.label,
                        color = if (active) colors.accent else colors.ink,
                        fontSize = 17.sp,
                        fontFamily = HandFont,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        SectionLabel("Игра")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SettingsActionButton(
                icon = INFO,
                label = "Как играть",
                seed = 840,
                modifier = Modifier.weight(1f),
                onClick = onRules,
            )
            SettingsActionButton(
                icon = TROPHY,
                label = "Рекорды",
                seed = 841,
                modifier = Modifier.weight(1f),
                onClick = onRecords,
            )
        }

        Spacer(Modifier.height(8.dp))

        SketchButton(
            onClick = {
                confirmReset = true
                resetDone = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            fill = colors.dangerWash.copy(alpha = 0.55f),
            border = colors.danger.copy(alpha = 0.6f),
            seed = 842,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                text = "Сбросить рекорды",
                color = colors.danger,
                fontSize = 16.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            )
        }

        if (confirmReset) {
            Spacer(Modifier.height(8.dp))

            SketchSurface(
                modifier = Modifier.fillMaxWidth(),
                fill = colors.dangerWash,
                border = colors.danger,
                strokeWidth = 1.6.dp,
                seed = 843,
                contentPadding = PaddingValues(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Сбросить все рекорды?",
                        color = colors.danger,
                        fontSize = 16.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SketchButton(
                            onClick = {
                                onResetRecords()
                                confirmReset = false
                                resetDone = true
                            },
                            modifier = Modifier.height(40.dp),
                            fill = colors.dangerWash,
                            border = colors.danger,
                            seed = 844,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Text(
                                text = "Да",
                                color = colors.danger,
                                fontSize = 15.sp,
                                fontFamily = HandFont,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        SketchButton(
                            onClick = { confirmReset = false },
                            modifier = Modifier.height(40.dp),
                            fill = colors.paperSunken,
                            border = colors.gridLine,
                            seed = 845,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Text(
                                text = "Отмена",
                                color = colors.inkSoft,
                                fontSize = 15.sp,
                                fontFamily = HandFont,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        if (resetDone) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Рекорды сброшены.",
                color = colors.success,
                fontSize = 15.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        SectionLabel("О приложении")

        Text(
            text = "Судоку 9×9 · версия 1.0",
            color = colors.inkSoft,
            fontSize = 15.sp,
            fontFamily = HandFont,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Рекорды и текущая игра хранятся только на этом устройстве.",
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = SudokuTheme.colors

    Text(
        text = text,
        color = colors.inkSoft,
        fontSize = 13.sp,
        fontFamily = HandFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ThemeRadio(active: Boolean) {
    val colors = SudokuTheme.colors

    Canvas(Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        sketchOval(
            center = Offset(size.width / 2f, size.height / 2f),
            radiusX = size.width / 2f - stroke,
            radiusY = size.height / 2f - stroke,
            color = if (active) colors.accent else colors.inkSoft,
            strokeWidth = stroke,
            seed = if (active) 900 else 901,
            wobble = 0.5f,
        )

        if (active) {
            drawCircle(
                color = colors.accent,
                radius = size.width * 0.2f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
    }
}

@Composable
private fun SettingsActionButton(
    icon: SketchIcon,
    label: String,
    seed: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SketchIcon(
                icon = icon,
                tint = colors.inkSoft,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                color = colors.ink,
                fontSize = 14.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Как в системе"
        ThemeMode.LIGHT -> "Светлая"
        ThemeMode.DARK -> "Тёмная"
    }
