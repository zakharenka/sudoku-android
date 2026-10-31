package me.zakharenka.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.zakharenka.sudoku.GameUiState
import me.zakharenka.sudoku.R
import me.zakharenka.sudoku.SudokuViewModel
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.formatTime
import me.zakharenka.sudoku.ui.theme.Accent
import me.zakharenka.sudoku.ui.theme.AccentSoft
import me.zakharenka.sudoku.ui.theme.Background
import me.zakharenka.sudoku.ui.theme.Line
import me.zakharenka.sudoku.ui.theme.LineStrong
import me.zakharenka.sudoku.ui.theme.PanelBottom
import me.zakharenka.sudoku.ui.theme.PanelDeep
import me.zakharenka.sudoku.ui.theme.PanelTop
import me.zakharenka.sudoku.ui.theme.TextMuted
import me.zakharenka.sudoku.ui.theme.TextPrimary
import me.zakharenka.sudoku.ui.theme.Violet

@Composable
fun SudokuScreen(viewModel: SudokuViewModel = viewModel()) {
    val state = viewModel.uiState

    var showRecords by rememberSaveable { mutableStateOf(false) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    var dismissedGameId by rememberSaveable { mutableStateOf<Long?>(null) }
    val winDismissed = dismissedGameId == state.gameId

    BackHandler(enabled = state.paused) {
        viewModel.resume()
    }

    BackHandler(enabled = state.won && !winDismissed) {
        dismissedGameId = state.gameId
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Background,
                        Color(0xFF0A0F1B),
                        Background,
                    ),
                ),
            ),
    ) {
        BackgroundGlow()

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
}

@Composable
private fun GamePanel(
    state: GameUiState,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    viewModel: SudokuViewModel,
) {
    val panelShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .shadow(
                elevation = 24.dp,
                shape = panelShape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.55f),
            )
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelTop,
                        PanelBottom,
                    ),
                ),
            )
            .border(1.dp, Line, panelShape)
            .padding(8.dp),
    ) {
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

        Spacer(Modifier.height(6.dp))

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

@Composable
private fun BoardBox(
    state: GameUiState,
    winDismissed: Boolean,
    onDismissWin: () -> Unit,
    onSelect: (Int) -> Unit,
    onResume: () -> Unit,
    onNewGame: () -> Unit,
) {
    val boardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(boardShape)
            .background(Color(0xFF070B14))
            .border(2.dp, LineStrong, boardShape),
    ) {
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

@Composable
private fun Header(
    onRules: () -> Unit,
    onRecords: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SudokuLogo(52.dp)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Судоку",
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                lineHeight = 32.sp,
            )
            Text(
                text = "Логика без спешки",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
        }

        HeaderIconButton(
            icon = R.drawable.ic_info,
            description = "Правила",
            onClick = onRules,
        )

        Spacer(Modifier.width(6.dp))

        HeaderIconButton(
            icon = R.drawable.ic_trophy,
            description = "Рекорды",
            onClick = onRecords,
        )
    }
}

@Composable
private fun HeaderIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(PanelDeep.copy(alpha = 0.6f))
            .border(1.dp, Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = TextMuted,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SudokuLogo(size: Dp) {
    val shape = RoundedCornerShape(size * 0.25f)
    val dark = isSystemInDarkTheme()
    val background = if (dark) {
        R.drawable.ic_launcher_background_dark
    } else {
        R.drawable.ic_launcher_background
    }
    val foreground = if (dark) {
        R.drawable.ic_launcher_foreground_dark
    } else {
        R.drawable.ic_launcher_foreground
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BackgroundGlow() {
    Canvas(Modifier.fillMaxSize()) {
        val cyanCenter = Offset(size.width * 0.12f, size.height * 0.04f)
        val cyanRadius = size.width * 0.9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.10f), Color.Transparent),
                center = cyanCenter,
                radius = cyanRadius,
            ),
            radius = cyanRadius,
            center = cyanCenter,
        )

        val violetCenter = Offset(size.width * 0.95f, size.height * 0.30f)
        val violetRadius = size.width * 0.8f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Violet.copy(alpha = 0.08f), Color.Transparent),
                center = violetCenter,
                radius = violetRadius,
            ),
            radius = violetRadius,
            center = violetCenter,
        )
    }
}

@Composable
private fun RecordsDialog(
    bestTimes: Map<Difficulty, Long?>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelTop,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Лучшие результаты",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = difficulty.label,
                            color = TextMuted,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = bestTimes[difficulty]?.let(::formatTime) ?: "—",
                            color = AccentSoft,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Рекорды хранятся только на этом устройстве.",
                    color = TextMuted.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Закрыть",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelTop,
        titleContentColor = TextPrimary,
        textContentColor = TextMuted,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Как играть",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RuleRow("1–9", "вписать цифру в выбранную клетку")
                RuleRow("Заметки", "записывать возможные варианты в пустой клетке")
                RuleRow("Пауза", "остановить таймер и скрыть поле")
                RuleRow("Подсказка", "открыть правильную цифру")

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "В каждой строке, каждом столбце и каждом блоке 3 × 3 цифры от 1 до 9 должны встречаться ровно один раз. Ошибки подсвечиваются красным.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Понятно",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

@Composable
private fun RuleRow(key: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .widthIn(min = 76.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(PanelDeep)
                .border(1.dp, Line, RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = key,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = text,
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
