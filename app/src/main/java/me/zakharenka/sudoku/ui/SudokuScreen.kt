package me.zakharenka.sudoku.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.SudokuViewModel
import me.zakharenka.sudoku.data.ThemeMode
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.StatusKind
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.ui.sketch.SketchButton
import me.zakharenka.sudoku.ui.sketch.SketchIcon
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOSE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.INFO
import me.zakharenka.sudoku.ui.sketch.SketchIcon.LANGUAGE
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
    var showNewGameDialog by rememberSaveable { mutableStateOf(false) }
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
                onNewGameClick = { showNewGameDialog = true },
                viewModel = viewModel,
            )
        }
    }

    if (showRecords) {
        RecordsDialog(
            bestTimes = state.bestTimes,
            solvedCounts = state.solvedCounts,
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
            onResetRecords = viewModel::clearBestTimes,
            onDismiss = { showSettings = false },
        )
    }

    if (showNewGameDialog) {
        NewGameDialog(
            initialDifficulty = state.difficulty,
            onStart = { difficulty ->
                showNewGameDialog = false
                viewModel.newGame(difficulty)
            },
            onDismiss = { showNewGameDialog = false },
        )
    }
}

@Composable
private fun GamePanel(
    state: GameUiState,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    onNewGameClick: () -> Unit,
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
            TopToolbar(
                difficulty = state.difficulty,
                mistakes = state.mistakes,
                elapsedSeconds = state.elapsedSeconds,
                paused = state.paused,
                enabled = !state.loading && !state.won,
                onTogglePause = viewModel::togglePause,
                onNewGame = onNewGameClick,
            )

            Spacer(Modifier.height(10.dp))

            BoardBox(
                state = state,
                winDismissed = winDismissed,
                onDismissWin = onDismissWin,
                onSelect = viewModel::select,
                onResume = viewModel::resume,
                onNewGame = onNewGameClick,
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

        Text(
            text = stringResource(R.string.app_name),
            color = colors.ink,
            fontSize = 34.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        HeaderIconButton(
            icon = INFO,
            description = stringResource(R.string.header_rules),
            seed = 700,
            onClick = onRules,
        )

        Spacer(Modifier.width(4.dp))

        HeaderIconButton(
            icon = TROPHY,
            description = stringResource(R.string.header_records),
            seed = 701,
            onClick = onRecords,
        )

        Spacer(Modifier.width(4.dp))

        HeaderIconButton(
            icon = SETTINGS,
            description = stringResource(R.string.header_settings),
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
        val closeDescription = stringResource(R.string.dialog_close)

        SketchButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = closeDescription },
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
    solvedCounts: Map<Difficulty, Int>,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchDialog(onDismiss = onDismiss, seed = 800) {
        DialogHeader(
            title = stringResource(R.string.records_title),
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
                Column {
                    Text(
                        text = stringResource(difficulty.labelRes()),
                        color = colors.inkSoft,
                        fontSize = 18.sp,
                        fontFamily = HandFont,
                    )
                    Text(
                        text = stringResource(
                            R.string.records_solved_level,
                            solvedCounts[difficulty] ?: 0,
                        ),
                        color = colors.inkSoft.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = HandFont,
                    )
                }
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
            text = stringResource(R.string.records_note),
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
            title = stringResource(R.string.rules_title),
            seed = 811,
            onDismiss = onDismiss,
        )

        Spacer(Modifier.height(12.dp))

        RuleLine(stringResource(R.string.rules_1))
        RuleLine(stringResource(R.string.rules_2))
        RuleLine(stringResource(R.string.rules_3))
        RuleLine(stringResource(R.string.rules_4))

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.rules_text),
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
    onResetRecords: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0]
    val currentLanguage = if (currentLocale.language == "ru") {
        stringResource(R.string.language_russian)
    } else {
        stringResource(R.string.language_english)
    }
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }

    SketchDialog(onDismiss = onDismiss, seed = 820) {
        DialogHeader(
            title = stringResource(R.string.settings_title),
            seed = 821,
            onDismiss = onDismiss,
        )

        SectionLabel(stringResource(R.string.settings_theme))

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
                    SketchRadio(active = active)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(mode.labelRes()),
                        color = if (active) colors.accent else colors.ink,
                        fontSize = 17.sp,
                        fontFamily = HandFont,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        SectionLabel(stringResource(R.string.settings_language))

        Text(
            text = stringResource(R.string.settings_language_current, currentLanguage),
            color = colors.ink,
            fontSize = 16.sp,
            fontFamily = HandFont,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_language_note),
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        SettingsActionButton(
            icon = LANGUAGE,
            label = stringResource(R.string.settings_language_open),
            seed = 850,
            modifier = Modifier.fillMaxWidth(),
            onClick = { openAppLanguageSettings(context) },
        )

        SectionLabel(stringResource(R.string.settings_game))

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
                text = stringResource(R.string.settings_reset_records),
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
                        text = stringResource(R.string.settings_reset_confirm),
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
                                text = stringResource(R.string.settings_reset_yes),
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
                                text = stringResource(R.string.settings_reset_cancel),
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
                text = stringResource(R.string.settings_reset_done),
                color = colors.success,
                fontSize = 15.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        SectionLabel(stringResource(R.string.settings_about))

        Text(
            text = stringResource(R.string.settings_about_version),
            color = colors.inkSoft,
            fontSize = 15.sp,
            fontFamily = HandFont,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_about_storage),
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun NewGameDialog(
    initialDifficulty: Difficulty,
    onStart: (Difficulty) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    var selected by remember(initialDifficulty) { mutableStateOf(initialDifficulty) }

    SketchDialog(onDismiss = onDismiss, seed = 860) {
        DialogHeader(
            title = stringResource(R.string.new_game),
            seed = 861,
            onDismiss = onDismiss,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.new_game_confirm),
            color = colors.inkSoft,
            fontSize = 15.sp,
            fontFamily = HandFont,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(12.dp))

        Difficulty.entries.forEachIndexed { index, difficulty ->
            val active = difficulty == selected

            SketchButton(
                onClick = { selected = difficulty },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                active = active,
                seed = 870 + index,
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SketchRadio(active = active)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(difficulty.labelRes()),
                            color = if (active) colors.accent else colors.ink,
                            fontSize = 17.sp,
                            fontFamily = HandFont,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = stringResource(difficulty.noteRes()),
                            color = colors.inkSoft,
                            fontSize = 12.sp,
                            fontFamily = HandFont,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SketchButton(
                onClick = { onStart(selected) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fill = colors.accentWash,
                border = colors.accent,
                active = true,
                seed = 880,
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_game_start),
                    color = colors.accent,
                    fontSize = 16.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
            }

            SketchButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                seed = 881,
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_game_cancel),
                    color = colors.inkSoft,
                    fontSize = 16.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
private fun SketchRadio(active: Boolean) {
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

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

private fun Difficulty.noteRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy_note
    Difficulty.MEDIUM -> R.string.difficulty_medium_note
    Difficulty.HARD -> R.string.difficulty_hard_note
    Difficulty.EXPERT -> R.string.difficulty_expert_note
}

private const val APP_LOCALE_SETTINGS_ACTION = "android.settings.APP_LOCALE_SETTINGS"

private fun openAppLanguageSettings(context: Context) {
    val appLocaleIntent = Intent(APP_LOCALE_SETTINGS_ACTION).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    val fallbackIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    } else {
        Intent(Settings.ACTION_LOCALE_SETTINGS)
    }

    runCatching { context.startActivity(appLocaleIntent) }
        .recoverCatching { context.startActivity(fallbackIntent) }
}
