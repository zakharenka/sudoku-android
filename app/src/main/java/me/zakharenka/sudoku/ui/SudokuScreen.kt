package me.zakharenka.sudoku.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.BuildConfig
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.SudokuViewModel
import me.zakharenka.sudoku.data.DataError
import me.zakharenka.sudoku.data.DataTransferResult
import me.zakharenka.sudoku.data.ThemeMode
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.GameRecord
import me.zakharenka.sudoku.game.RecordStats
import me.zakharenka.sudoku.game.formatDateTime
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.ui.sketch.SketchButton
import me.zakharenka.sudoku.ui.sketch.SketchIcon
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CHECK
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOCK
import me.zakharenka.sudoku.ui.sketch.SketchIcon.CLOSE
import me.zakharenka.sudoku.ui.sketch.SketchIcon.EXPORT
import me.zakharenka.sudoku.ui.sketch.SketchIcon.HISTORY
import me.zakharenka.sudoku.ui.sketch.SketchIcon.IMPORT
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
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNewGameDialog by rememberSaveable { mutableStateOf(false) }
    var dismissedGameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pausedByDialog by rememberSaveable { mutableStateOf(false) }
    val dialogOpen = showRecords || showHistory || showRules || showSettings || showNewGameDialog
    val winDismissed = dismissedGameId == state.gameId
    val keyboardFocus = remember { FocusRequester() }

    LaunchedEffect(dialogOpen, state.paused, state.won) {
        if (!dialogOpen && !state.paused && !state.won) {
            keyboardFocus.requestFocus()
        }
    }

    LaunchedEffect(dialogOpen, state.loading, state.won) {
        if (dialogOpen) {
            if (!state.paused && !state.loading && !state.won) {
                pausedByDialog = true
                viewModel.pause()
            }
        } else if (pausedByDialog) {
            pausedByDialog = false
            viewModel.resume()
        }
    }

    BackHandler(enabled = state.paused && !dialogOpen) {
        viewModel.resume()
    }

    BackHandler(enabled = state.won && !winDismissed && !dialogOpen) {
        dismissedGameId = state.gameId
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(keyboardFocus)
            .onPreviewKeyEvent { event ->
                handleBoardKey(
                    event = event,
                    state = state,
                    dialogOpen = dialogOpen,
                    viewModel = viewModel,
                )
            }
            .focusable(),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            paperTexture(
                base = colors.paper,
                speckle = colors.paperSpeckle,
                seed = 7,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val isWide = maxWidth >= 900.dp
            val wideBoardMax = maxOf(
                320.dp,
                minOf(760.dp, maxWidth * 0.56f, maxHeight - 160.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(
                    isWide = isWide,
                    onRules = { showRules = true },
                    onHistory = {
                        viewModel.openHistory()
                        showHistory = true
                    },
                    onSettings = { showSettings = true },
                )

                Spacer(Modifier.height(10.dp))

                GamePanel(
                    state = state,
                    isWide = isWide,
                    boardMax = wideBoardMax,
                    straightGrid = viewModel.straightGrid,
                    sameHighlight = viewModel.sameHighlight,
                    winDismissed = winDismissed,
                    onDismissWin = { dismissedGameId = state.gameId },
                    onNewGameClick = { showNewGameDialog = true },
                    viewModel = viewModel,
                )
            }
        }
    }

    if (showHistory) {
        HistoryDialog(
            games = state.history,
            currentGameId = state.currentGameId,
            onSelectGame = { gameId ->
                viewModel.loadGame(gameId)
                pausedByDialog = false
                showHistory = false
            },
            onRecords = {
                viewModel.openRecords()
                showRecords = true
            },
            onDismiss = { showHistory = false },
        )
    }

    if (showRecords) {
        RecordsDialog(
            records = state.records,
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
            straightGrid = viewModel.straightGrid,
            onStraightGridChange = viewModel::updateStraightGrid,
            sameHighlight = viewModel.sameHighlight,
            onSameHighlightChange = viewModel::updateSameHighlight,
            onExport = viewModel::exportData,
            onImport = viewModel::importData,
            onDismiss = { showSettings = false },
        )
    }

    if (showNewGameDialog) {
        NewGameDialog(
            initialDifficulty = state.difficulty,
            initialShowErrors = viewModel.showErrorsDefault,
            onStart = { difficulty, showErrors ->
                showNewGameDialog = false
                viewModel.newGame(difficulty, showErrors)
            },
            onDismiss = { showNewGameDialog = false },
        )
    }
}

private fun handleBoardKey(
    event: KeyEvent,
    state: GameUiState,
    dialogOpen: Boolean,
    viewModel: SudokuViewModel,
): Boolean {
    if (dialogOpen || state.loading || state.paused || state.won) {
        return false
    }

    if (event.type != KeyEventType.KeyDown) {
        return false
    }

    val digit = event.digitNumber()
    if (digit != null) {
        if (digit == 0) {
            viewModel.erase()
        } else {
            viewModel.enter(digit)
        }
        return true
    }

    return when (event.key) {
        Key.DirectionUp -> {
            viewModel.moveSelection(-1, 0)
            true
        }

        Key.DirectionDown -> {
            viewModel.moveSelection(1, 0)
            true
        }

        Key.DirectionLeft -> {
            viewModel.moveSelection(0, -1)
            true
        }

        Key.DirectionRight -> {
            viewModel.moveSelection(0, 1)
            true
        }

        Key.Backspace, Key.Delete -> {
            viewModel.erase()
            true
        }

        else -> false
    }
}

private fun KeyEvent.digitNumber(): Int? {
    val keyCode = nativeKeyEvent.keyCode

    return when (keyCode) {
        in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 ->
            keyCode - android.view.KeyEvent.KEYCODE_0

        in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 ->
            keyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0

        else -> null
    }
}

@Composable
private fun GamePanel(
    state: GameUiState,
    isWide: Boolean,
    boardMax: Dp,
    straightGrid: Boolean,
    sameHighlight: Boolean,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    onNewGameClick: () -> Unit,
    viewModel: SudokuViewModel,
) {
    val colors = SudokuTheme.colors

    SketchSurface(
        modifier = Modifier
            .widthIn(max = if (isWide) 1120.dp else 720.dp)
            .fillMaxWidth(),
        fill = colors.paperAlt,
        border = colors.inkSoft,
        strokeWidth = 1.6.dp,
        seed = 10,
        shadow = colors.shadow,
        contentPadding = PaddingValues(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        if (isWide) {
            Column(Modifier.fillMaxWidth()) {
                TopToolbar(
                    difficulty = state.difficulty,
                    mistakes = state.mistakes,
                    showErrors = state.showErrors,
                    elapsedSeconds = state.elapsedSeconds,
                    paused = state.paused,
                    enabled = !state.loading && !state.won,
                    onTogglePause = viewModel::togglePause,
                    onNewGame = onNewGameClick,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BoardBox(
                        state = state,
                        straightGrid = straightGrid,
                        sameHighlight = sameHighlight,
                        winDismissed = winDismissed,
                        onDismissWin = onDismissWin,
                        onSelect = viewModel::select,
                        onResume = viewModel::resume,
                        onNewGame = onNewGameClick,
                        modifier = Modifier
                            .width(boardMax)
                            .aspectRatio(1f),
                    )

                    GameControls(
                        state = state,
                        onNewGameClick = onNewGameClick,
                        viewModel = viewModel,
                        boardCellSize = boardMax / 9,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                TopToolbar(
                    difficulty = state.difficulty,
                    mistakes = state.mistakes,
                    showErrors = state.showErrors,
                    elapsedSeconds = state.elapsedSeconds,
                    paused = state.paused,
                    enabled = !state.loading && !state.won,
                    onTogglePause = viewModel::togglePause,
                    onNewGame = onNewGameClick,
                )

                Spacer(Modifier.height(10.dp))

                BoardBox(
                    state = state,
                    straightGrid = straightGrid,
                    sameHighlight = sameHighlight,
                    winDismissed = winDismissed,
                    onDismissWin = onDismissWin,
                    onSelect = viewModel::select,
                    onResume = viewModel::resume,
                    onNewGame = onNewGameClick,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                GameControls(
                    state = state,
                    onNewGameClick = onNewGameClick,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun GameControls(
    state: GameUiState,
    onNewGameClick: () -> Unit,
    viewModel: SudokuViewModel,
    boardCellSize: Dp? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        NumberPad(
            cells = state.cells,
            enabled = !state.loading && !state.paused && !state.won,
            onNumber = viewModel::enter,
            boardCellSize = boardCellSize,
        )

        Spacer(Modifier.height(8.dp))

        ActionRow(
            state = state,
            onUndo = viewModel::undo,
            onErase = viewModel::erase,
            onNotes = viewModel::toggleNotes,
            onHint = viewModel::hint,
        )
    }
}

@Composable
private fun BoardBox(
    state: GameUiState,
    straightGrid: Boolean,
    sameHighlight: Boolean,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    onSelect: (Int) -> Unit,
    onResume: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors

    Box(modifier = modifier.aspectRatio(1f)) {
        SketchSurface(
            modifier = Modifier.fillMaxSize(),
            fill = colors.paperAlt,
            border = null,
            strokeWidth = 2.dp,
            seed = 20,
            shadow = colors.shadow,
            straight = straightGrid,
            contentPadding = PaddingValues(0.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(Modifier.fillMaxSize()) {
                SudokuBoard(
                    cells = state.cells,
                    selectedIndex = state.selectedIndex,
                    straightGrid = straightGrid,
                    highlightSameDigits = sameHighlight,
                    gameId = state.gameId,
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

        SudokuBoardFrame(straightGrid = straightGrid)
    }
}

@Composable
private fun Header(
    isWide: Boolean,
    onRules: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val buttonSize = if (isWide) 44.dp else 40.dp
    val iconSpacing = if (isWide) 4.dp else 2.dp

    Row(
        modifier = Modifier
            .widthIn(max = if (isWide) 1120.dp else 720.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SketchLogo(if (isWide) 56.dp else 50.dp)

        Spacer(Modifier.width(if (isWide) 12.dp else 8.dp))

        BasicText(
            text = stringResource(R.string.app_name),
            style = TextStyle(
                color = colors.ink,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 20.sp,
                maxFontSize = if (isWide) 36.sp else 31.sp,
                stepSize = 1.sp,
            ),
            modifier = Modifier.weight(1f),
        )

        HeaderIconButton(
            icon = INFO,
            description = stringResource(R.string.header_rules),
            seed = 700,
            size = buttonSize,
            onClick = onRules,
        )

        Spacer(Modifier.width(iconSpacing))

        HeaderIconButton(
            icon = HISTORY,
            description = stringResource(R.string.header_history),
            seed = 703,
            size = buttonSize,
            onClick = onHistory,
        )

        Spacer(Modifier.width(iconSpacing))

        HeaderIconButton(
            icon = SETTINGS,
            description = stringResource(R.string.header_settings),
            seed = 702,
            size = buttonSize,
            onClick = onSettings,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: SketchIcon,
    description: String,
    seed: Int,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = description },
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = seed,
        contentPadding = PaddingValues(0.dp),
    ) {
        SketchIcon(
            icon = icon,
            tint = colors.inkSoft,
            modifier = Modifier.size(22.dp),
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
            fontSize = 24.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SketchDialog(
    title: String,
    onDismiss: () -> Unit,
    seed: Int,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
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
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SketchSurface(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight),
                    fill = colors.paperAlt,
                    border = colors.ink,
                    strokeWidth = 2.dp,
                    seed = seed,
                    shadow = colors.shadow,
                    contentPadding = PaddingValues(0.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        DialogHeader(
                            title = title,
                            seed = seed + 1,
                            onDismiss = onDismiss,
                            actions = actions,
                            modifier = Modifier.padding(
                                start = 18.dp,
                                top = 18.dp,
                                end = 18.dp,
                                bottom = 10.dp,
                            ),
                        )
                        DialogDivider(seed = seed + 2)
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = !scrollable)
                                .then(
                                    if (scrollable) {
                                        Modifier.verticalScroll(rememberScrollState())
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(
                                    start = 18.dp,
                                    top = 12.dp,
                                    end = 18.dp,
                                    bottom = 18.dp,
                                ),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogDivider(seed: Int) {
    val colors = SudokuTheme.colors

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(2.dp),
    ) {
        sketchLine(
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            color = colors.gridLine,
            strokeWidth = 1.dp.toPx(),
            seed = seed,
            wobble = 0.4.dp.toPx(),
            passes = 1,
        )
    }
}

@Composable
private fun DialogHeader(
    title: String,
    seed: Int,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = colors.ink,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 20.sp,
                maxFontSize = 30.sp,
                stepSize = 1.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        val closeDescription = stringResource(R.string.dialog_close)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
            SketchButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = closeDescription },
                fill = colors.paperSunken,
                border = colors.gridLine,
                seed = seed,
                contentPadding = PaddingValues(0.dp),
            ) {
                SketchIcon(
                    icon = CLOSE,
                    tint = colors.inkSoft,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordsDialog(
    records: Map<Difficulty, RecordStats>,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    var showMistakes by remember { mutableStateOf(false) }

    SketchDialog(
        title = stringResource(R.string.records_title),
        onDismiss = onDismiss,
        seed = 800,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ModeButton(
                label = stringResource(R.string.records_mode_hide_errors),
                active = !showMistakes,
                seed = 801,
                modifier = Modifier.weight(1f),
                onClick = { showMistakes = false },
            )
            ModeButton(
                label = stringResource(R.string.records_mode_show_errors),
                active = showMistakes,
                seed = 802,
                modifier = Modifier.weight(1f),
                onClick = { showMistakes = true },
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(
                if (showMistakes) {
                    R.string.records_hint_with_mistakes
                } else {
                    R.string.records_hint_without_mistakes
                },
            ),
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(12.dp))

        Difficulty.entries.forEach { difficulty ->
            val record = records[difficulty]?.forMistakes(showMistakes) ?: return@forEach

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(difficulty.labelRes()),
                        color = colors.inkSoft,
                        fontSize = 20.sp,
                        fontFamily = HandFont,
                    )
                    Text(
                        text = stringResource(
                            R.string.records_solved_level,
                            record.solved,
                        ),
                        color = colors.inkSoft.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontFamily = HandFont,
                    )
                }
                Text(
                    text = record.bestSeconds?.let(::formatTime) ?: "—",
                    color = colors.accent,
                    fontSize = 23.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    active: Boolean,
    seed: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        fill = colors.paperSunken,
        border = colors.gridLine,
        active = active,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (active) colors.accent else colors.inkSoft,
            fontSize = 16.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryDialog(
    games: List<GameRecord>,
    currentGameId: String?,
    onSelectGame: (String) -> Unit,
    onRecords: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val groups = remember(games, zone) {
        games.sortedByDescending { it.createdAt }
            .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
    }
    val useLazyList = games.size > HISTORY_LAZY_THRESHOLD

    SketchDialog(
        title = stringResource(R.string.history_title),
        onDismiss = onDismiss,
        seed = 807,
        scrollable = !useLazyList,
        actions = {
            HeaderIconButton(
                icon = TROPHY,
                description = stringResource(R.string.header_records),
                seed = 808,
                size = 44.dp,
                onClick = onRecords,
            )
        },
    ) {
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                color = colors.inkSoft,
                fontSize = 17.sp,
                fontFamily = HandFont,
            )
            return@SketchDialog
        }

        if (useLazyList) {
            LazyColumn(Modifier.fillMaxSize()) {
                groups.forEach { (date, dayGames) ->
                    item(key = "date-$date") {
                        HistoryDateHeader(date, dateFormatter)
                    }

                    items(dayGames, key = { it.id }) { game ->
                        HistoryRow(
                            game = game,
                            isCurrent = game.id == currentGameId,
                            locale = locale,
                            onClick = { onSelectGame(game.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        } else {
            groups.forEach { (date, dayGames) ->
                HistoryDateHeader(date, dateFormatter)

                dayGames.forEach { game ->
                    HistoryRow(
                        game = game,
                        isCurrent = game.id == currentGameId,
                        locale = locale,
                        onClick = { onSelectGame(game.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private const val HISTORY_LAZY_THRESHOLD = 50

@Composable
private fun HistoryDateHeader(date: LocalDate, formatter: DateTimeFormatter) {
    Text(
        text = date.format(formatter),
        color = SudokuTheme.colors.inkSoft,
        fontSize = 15.sp,
        fontFamily = HandFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun HistoryRow(
    game: GameRecord,
    isCurrent: Boolean,
    locale: Locale,
    onClick: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val mistakeText = if (game.showErrors && game.errors > 0) {
        pluralStringResource(R.plurals.mistakes, game.errors, game.errors)
    } else {
        null
    }
    val hintText = if (game.hintsUsed > 0) {
        pluralStringResource(R.plurals.hints, game.hintsUsed, game.hintsUsed)
    } else {
        null
    }
    val statsText = listOfNotNull(mistakeText, hintText).joinToString(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PuzzleThumbnail(
            puzzle = game.puzzle,
            modifier = Modifier.size(80.dp),
        )

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(game.difficulty.labelRes()),
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                HistoryStatusIcon(solved = game.solved)
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.history_current),
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.history_started,
                    formatDateTime(game.createdAt, locale),
                ),
                color = colors.inkSoft,
                fontSize = 13.sp,
                fontFamily = HandFont,
                lineHeight = 17.sp,
            )
            Text(
                text = if (game.solved) {
                    stringResource(
                        R.string.history_solved,
                        formatDateTime(game.solvedAt ?: game.updatedAt, locale),
                    )
                } else {
                    stringResource(
                        R.string.history_updated,
                        formatDateTime(game.updatedAt, locale),
                    )
                },
                color = colors.inkSoft,
                fontSize = 13.sp,
                fontFamily = HandFont,
                lineHeight = 17.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.history_time,
                        formatTime(game.elapsedSeconds),
                    ),
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
                if (statsText.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statsText,
                        color = colors.inkSoft,
                        fontSize = 13.sp,
                        fontFamily = HandFont,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusIcon(solved: Boolean) {
    val colors = SudokuTheme.colors
    val description = stringResource(
        if (solved) R.string.history_status_solved else R.string.history_status_in_progress,
    )

    SketchIcon(
        icon = if (solved) CHECK else CLOCK,
        tint = if (solved) colors.success else colors.accentSoft,
        modifier = Modifier
            .size(18.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun PuzzleThumbnail(
    puzzle: IntArray,
    modifier: Modifier = Modifier,
) {
    val colors = SudokuTheme.colors
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val description = stringResource(R.string.history_puzzle_preview)

    Canvas(
        modifier = modifier.semantics { contentDescription = description },
    ) {
        val cell = size.minDimension / 9f
        val lineWidth = 0.7.dp.toPx()
        val strongWidth = 1.3.dp.toPx()
        val textStyle = TextStyle(
            color = colors.ink,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { (cell * 0.78f).toSp() },
        )

        drawRect(color = colors.paperSunken)

        for (index in 1..8) {
            val thick = index % 3 == 0
            val width = if (thick) strongWidth else lineWidth
            val color = if (thick) colors.gridLineStrong else colors.gridLine
            val offset = index * cell

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
        }

        for (index in puzzle.indices) {
            val value = puzzle[index]

            if (value == 0) {
                continue
            }

            val row = index / 9
            val col = index % 9
            val layout = textMeasurer.measure(
                text = AnnotatedString(value.toString()),
                style = textStyle,
                maxLines = 1,
            )

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = col * cell + (cell - layout.size.width) / 2f,
                    y = row * cell + (cell - layout.size.height) / 2f,
                ),
            )
        }

        drawRect(
            color = colors.gridLineStrong,
            topLeft = Offset.Zero,
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strongWidth),
        )
    }
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    val colors = SudokuTheme.colors

    SketchDialog(
        title = stringResource(R.string.rules_title),
        onDismiss = onDismiss,
        seed = 810,
    ) {
        RuleLine(stringResource(R.string.rules_1))
        RuleLine(stringResource(R.string.rules_2))
        RuleLine(stringResource(R.string.rules_3))
        RuleLine(stringResource(R.string.rules_4))

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.rules_text),
            color = colors.inkSoft,
            fontSize = 17.sp,
            fontFamily = HandFont,
            lineHeight = 24.sp,
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
            fontSize = 20.sp,
            fontFamily = HandFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = colors.ink,
            fontSize = 18.sp,
            fontFamily = HandFont,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun SettingsDialog(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    straightGrid: Boolean,
    onStraightGridChange: (Boolean) -> Unit,
    sameHighlight: Boolean,
    onSameHighlightChange: (Boolean) -> Unit,
    onExport: (Uri, (DataTransferResult) -> Unit) -> Unit,
    onImport: (Uri, (DataTransferResult) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0]
    val currentLanguage = currentLocale
        .getDisplayLanguage(currentLocale)
        .replaceFirstChar { it.titlecase(currentLocale) }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var transferMessage by rememberSaveable { mutableStateOf<TransferMessage?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            transferMessage = null
            onExport(uri) { result ->
                transferMessage = result.toMessage(export = true)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            transferMessage = null
            pendingImport = uri.toString()
        }
    }

    SketchDialog(
        title = stringResource(R.string.settings_title),
        onDismiss = onDismiss,
        seed = 820,
    ) {
        SectionLabel(stringResource(R.string.settings_theme))

        ThemeMode.entries.forEachIndexed { index, mode ->
            val active = themeMode == mode

            SketchButton(
                onClick = { onThemeModeChange(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                active = active,
                seed = 830 + index,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                        fontSize = 19.sp,
                        fontFamily = HandFont,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        SectionLabel(stringResource(R.string.settings_board))

        SettingsCheckRow(
            label = stringResource(R.string.settings_straight_grid),
            checked = straightGrid,
            seed = 835,
            onToggle = { onStraightGridChange(!straightGrid) },
        )

        Spacer(Modifier.height(6.dp))

        SettingsCheckRow(
            label = stringResource(R.string.settings_same_digits),
            checked = sameHighlight,
            seed = 836,
            onToggle = { onSameHighlightChange(!sameHighlight) },
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.settings_language_current, currentLanguage),
            color = colors.ink,
            fontSize = 18.sp,
            fontFamily = HandFont,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        SettingsActionButton(
            icon = LANGUAGE,
            label = stringResource(R.string.settings_language_open),
            seed = 850,
            modifier = Modifier.fillMaxWidth(),
            onClick = { openAppLanguageSettings(context) },
        )

        SectionLabel(stringResource(R.string.settings_data))

        SettingsActionButton(
            icon = EXPORT,
            label = stringResource(R.string.settings_export),
            seed = 842,
            modifier = Modifier.fillMaxWidth(),
            onClick = { exportLauncher.launch(backupFileName()) },
        )

        Spacer(Modifier.height(6.dp))

        SettingsActionButton(
            icon = IMPORT,
            label = stringResource(R.string.settings_import),
            seed = 843,
            modifier = Modifier.fillMaxWidth(),
            onClick = { importLauncher.launch(arrayOf("application/json")) },
        )

        if (pendingImport != null) {
            Spacer(Modifier.height(10.dp))

            SketchSurface(
                modifier = Modifier.fillMaxWidth(),
                fill = colors.dangerWash,
                border = colors.danger,
                strokeWidth = 1.6.dp,
                seed = 844,
                contentPadding = PaddingValues(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.settings_import_confirm),
                        color = colors.danger,
                        fontSize = 17.sp,
                        fontFamily = HandFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SketchButton(
                            onClick = {
                                val uri = pendingImport?.let(Uri::parse)
                                pendingImport = null

                                if (uri != null) {
                                    transferMessage = null
                                    onImport(uri) { result ->
                                        transferMessage = result.toMessage(export = false)
                                    }
                                }
                            },
                            modifier = Modifier.height(44.dp),
                            fill = colors.dangerWash,
                            border = colors.danger,
                            seed = 845,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_import_yes),
                                color = colors.danger,
                                fontSize = 17.sp,
                                fontFamily = HandFont,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        SketchButton(
                            onClick = { pendingImport = null },
                            modifier = Modifier.height(44.dp),
                            fill = colors.paperSunken,
                            border = colors.gridLine,
                            seed = 846,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_import_no),
                                color = colors.inkSoft,
                                fontSize = 17.sp,
                                fontFamily = HandFont,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        if (transferMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(transferMessage!!.messageRes),
                color = if (transferMessage == TransferMessage.EXPORTED ||
                    transferMessage == TransferMessage.IMPORTED
                ) {
                    colors.success
                } else {
                    colors.danger
                },
                fontSize = 17.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        SectionLabel(stringResource(R.string.settings_about))

        Text(
            text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
            color = colors.inkSoft,
            fontSize = 17.sp,
            fontFamily = HandFont,
        )

        Spacer(Modifier.height(10.dp))

        SettingsActionButton(
            icon = LANGUAGE,
            label = stringResource(R.string.settings_developer_website),
            seed = 851,
            modifier = Modifier.fillMaxWidth(),
            onClick = { openDeveloperWebsite(context) },
        )
    }
}

private enum class TransferMessage(val messageRes: Int) {
    EXPORTED(R.string.transfer_exported),
    IMPORTED(R.string.transfer_imported),
    FAILED(R.string.transfer_failed),
    INVALID_JSON(R.string.transfer_invalid_json),
    INVALID_VERSION(R.string.transfer_invalid_version),
    INVALID_FIELDS(R.string.transfer_invalid_fields),
}

private fun DataTransferResult.toMessage(export: Boolean): TransferMessage = when (this) {
    is DataTransferResult.Success -> if (export) {
        TransferMessage.EXPORTED
    } else {
        TransferMessage.IMPORTED
    }

    is DataTransferResult.Failure -> when (error) {
        DataError.IO_ERROR -> TransferMessage.FAILED
        DataError.INVALID_JSON -> TransferMessage.INVALID_JSON
        DataError.UNSUPPORTED_VERSION -> TransferMessage.INVALID_VERSION
        DataError.INVALID_FIELDS -> TransferMessage.INVALID_FIELDS
    }
}

private fun backupFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "sudoku-backup-$stamp.json"
}

@Composable
private fun NewGameDialog(
    initialDifficulty: Difficulty,
    initialShowErrors: Boolean,
    onStart: (Difficulty, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SudokuTheme.colors
    var selected by remember(initialDifficulty) { mutableStateOf(initialDifficulty) }
    var showErrors by remember(initialShowErrors) { mutableStateOf(initialShowErrors) }

    SketchDialog(
        title = stringResource(R.string.new_game),
        onDismiss = onDismiss,
        seed = 860,
    ) {
        Text(
            text = stringResource(R.string.new_game_confirm),
            color = colors.inkSoft,
            fontSize = 17.sp,
            fontFamily = HandFont,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(12.dp))

        Difficulty.entries.forEachIndexed { index, difficulty ->
            val active = difficulty == selected

            SketchButton(
                onClick = { selected = difficulty },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                active = active,
                seed = 870 + index,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                            fontSize = 19.sp,
                            fontFamily = HandFont,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = stringResource(difficulty.noteRes()),
                            color = colors.inkSoft,
                            fontSize = 14.sp,
                            fontFamily = HandFont,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(4.dp))

        SettingsCheckRow(
            label = stringResource(R.string.new_game_show_errors),
            checked = showErrors,
            seed = 879,
            onToggle = { showErrors = !showErrors },
        )

        Text(
            text = stringResource(R.string.new_game_show_errors_note),
            color = colors.inkSoft,
            fontSize = 13.sp,
            fontFamily = HandFont,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 32.dp, top = 4.dp, end = 4.dp),
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SketchButton(
                onClick = { onStart(selected, showErrors) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                fill = colors.accentWash,
                border = colors.accent,
                active = true,
                seed = 880,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_game_start),
                    color = colors.accent,
                    fontSize = 18.sp,
                    fontFamily = HandFont,
                    fontWeight = FontWeight.Bold,
                )
            }

            SketchButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                fill = colors.paperSunken,
                border = colors.gridLine,
                seed = 881,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_game_cancel),
                    color = colors.inkSoft,
                    fontSize = 18.sp,
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
        fontSize = 14.sp,
        fontFamily = HandFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SketchRadio(active: Boolean) {
    val colors = SudokuTheme.colors

    Canvas(Modifier.size(22.dp)) {
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
private fun SketchCheck(checked: Boolean) {
    val colors = SudokuTheme.colors

    Canvas(Modifier.size(22.dp)) {
        val side = size.minDimension
        val stroke = 1.5.dp.toPx()
        val inset = stroke

        sketchRect(
            topLeft = Offset(inset, inset),
            size = Size(side - inset * 2f, side - inset * 2f),
            color = if (checked) colors.accent else colors.inkSoft,
            strokeWidth = stroke,
            seed = 905,
            wobble = 0.5.dp.toPx(),
            passes = 1,
        )

        if (checked) {
            sketchLine(
                start = Offset(side * 0.22f, side * 0.52f),
                end = Offset(side * 0.43f, side * 0.75f),
                color = colors.accent,
                strokeWidth = stroke,
                seed = 906,
                wobble = 0.4.dp.toPx(),
                passes = 1,
            )
            sketchLine(
                start = Offset(side * 0.43f, side * 0.75f),
                end = Offset(side * 0.8f, side * 0.25f),
                color = colors.accent,
                strokeWidth = stroke,
                seed = 907,
                wobble = 0.4.dp.toPx(),
                passes = 1,
            )
        }
    }
}

@Composable
private fun SettingsCheckRow(
    label: String,
    checked: Boolean,
    seed: Int,
    onToggle: () -> Unit,
) {
    val colors = SudokuTheme.colors

    SketchButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        fill = colors.paperSunken,
        border = colors.gridLine,
        active = checked,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SketchCheck(checked = checked)
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = if (checked) colors.accent else colors.ink,
                fontSize = 19.sp,
                fontFamily = HandFont,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
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
        modifier = modifier.heightIn(min = 50.dp),
        fill = colors.paperSunken,
        border = colors.gridLine,
        seed = seed,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SketchIcon(
                icon = icon,
                tint = colors.inkSoft,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                color = colors.ink,
                fontSize = 16.sp,
                fontFamily = HandFont,
                fontWeight = FontWeight.Bold,
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
private const val DEVELOPER_WEBSITE = "https://zakharenka.me"

private fun openDeveloperWebsite(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, DEVELOPER_WEBSITE.toUri())
    runCatching { context.startActivity(intent) }
}

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
