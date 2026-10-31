package me.zakharenka.sudoku

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zakharenka.sudoku.data.RecordsStore
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.MoveOutcome
import me.zakharenka.sudoku.game.StatusKind
import me.zakharenka.sudoku.game.SudokuGenerator
import me.zakharenka.sudoku.game.SudokuSession
import me.zakharenka.sudoku.game.formatTime

data class GameUiState(
    val difficulty: Difficulty = Difficulty.EASY,
    val cells: List<Cell> = emptyList(),
    val selectedIndex: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val elapsedSeconds: Long = 0,
    val paused: Boolean = false,
    val won: Boolean = false,
    val loading: Boolean = true,
    val status: String = "Выберите клетку и введите цифру",
    val statusKind: StatusKind = StatusKind.NEUTRAL,
    val bestTimes: Map<Difficulty, Long?> = emptyMap(),
    val isNewRecord: Boolean = false,
    val canUndo: Boolean = false,
    val canErase: Boolean = false,
    val gameId: Long = 0,
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val records = RecordsStore(application)
    private val generator = SudokuGenerator()

    private var session: SudokuSession? = null
    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var elapsedMillis = 0L
    private var timerStartedAt = 0L
    private var bestTimes = records.loadBestTimes()
    private var notesMode = records.notesMode

    var uiState by mutableStateOf(
        GameUiState(
            notesMode = notesMode,
            bestTimes = bestTimes,
        ),
    )
        private set

    init {
        newGame(Difficulty.EASY)
    }

    fun newGame(difficulty: Difficulty = uiState.difficulty) {
        generationJob?.cancel()
        stopTimer()
        session = null
        elapsedMillis = 0
        timerStartedAt = 0

        uiState = uiState.copy(
            difficulty = difficulty,
            cells = emptyList(),
            selectedIndex = null,
            notesMode = notesMode,
            mistakes = 0,
            elapsedSeconds = 0,
            paused = false,
            won = false,
            loading = true,
            status = "Генерация поля…",
            statusKind = StatusKind.NEUTRAL,
            isNewRecord = false,
            canUndo = false,
            canErase = false,
            gameId = uiState.gameId + 1,
        )

        generationJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) { generator.generate(difficulty) }
            }

            if (!isActive) {
                return@launch
            }

            result.onSuccess { generated ->
                val newSession = SudokuSession(
                    difficulty = difficulty,
                    solution = generated.solution,
                    puzzle = generated.puzzle,
                    notesMode = notesMode,
                )

                session = newSession
                uiState = uiState.copy(loading = false)
                refresh(
                    status = "Новая игра: ${difficulty.label.lowercase()}. Подсказок: ${generated.clues}",
                )
                startTimer()
            }.onFailure {
                uiState = uiState.copy(
                    loading = false,
                    status = "Не удалось создать поле. Попробуйте ещё раз.",
                    statusKind = StatusKind.ERROR,
                )
            }
        }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        if (difficulty == uiState.difficulty && !uiState.won) {
            uiState = uiState.copy(
                status = "Этот уровень уже выбран",
                statusKind = StatusKind.NEUTRAL,
            )
            return
        }

        newGame(difficulty)
    }

    fun select(index: Int) {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        if (session.won) {
            return
        }

        session.select(index)
        refresh()
    }

    fun enter(number: Int) {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        handleOutcome(session, session.enter(number))
    }

    fun erase() {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        handleOutcome(session, session.erase())
    }

    fun undo() {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        handleOutcome(session, session.undo())
    }

    fun hint() {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        handleOutcome(session, session.hint())
    }

    fun toggleNotes() {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        if (session.won) {
            return
        }

        val outcome = session.toggleNotes()
        notesMode = session.notesMode
        records.notesMode = notesMode
        refresh(outcome.message, outcome.kind)
    }

    fun togglePause() {
        if (uiState.paused) {
            resume()
        } else {
            pause()
        }
    }

    fun pause() {
        if (uiState.loading || uiState.paused || uiState.won) {
            return
        }

        stopTimer()
        uiState = uiState.copy(
            paused = true,
            status = "Пауза",
            statusKind = StatusKind.NEUTRAL,
        )
    }

    fun resume() {
        if (!uiState.paused) {
            return
        }

        uiState = uiState.copy(
            paused = false,
            status = "Продолжаем",
            statusKind = StatusKind.NEUTRAL,
        )
        startTimer()
    }

    fun onAppBackgrounded() {
        if (!uiState.loading && !uiState.won && !uiState.paused) {
            pause()
        }
    }

    private fun handleOutcome(session: SudokuSession, outcome: MoveOutcome) {
        if (outcome.won) {
            finishWin(session)
            return
        }

        if (outcome.changed || outcome.message.isNotEmpty()) {
            refresh(
                status = outcome.message.ifEmpty { null },
                kind = outcome.kind,
            )
        }
    }

    private fun finishWin(session: SudokuSession) {
        stopTimer()

        val seconds = elapsedMillis / 1000
        val isRecord = records.saveBestTime(session.difficulty, seconds)

        if (isRecord) {
            bestTimes = records.loadBestTimes()
        }

        val time = formatTime(seconds)
        refresh(
            status = if (isRecord) {
                "Решено! Новый рекорд: $time"
            } else {
                "Решено! Время: $time"
            },
            kind = StatusKind.SUCCESS,
            isNewRecord = isRecord,
        )
    }

    private fun refresh(
        status: String? = null,
        kind: StatusKind? = null,
        isNewRecord: Boolean = uiState.isNewRecord,
    ) {
        val session = session ?: return

        uiState = uiState.copy(
            difficulty = session.difficulty,
            cells = session.cells.toList(),
            selectedIndex = session.selected,
            notesMode = session.notesMode,
            mistakes = session.mistakes,
            won = session.won,
            status = status ?: uiState.status,
            statusKind = kind ?: uiState.statusKind,
            bestTimes = bestTimes,
            isNewRecord = isNewRecord,
            canUndo = session.canUndo,
            canErase = session.canErase,
            elapsedSeconds = elapsedMillis / 1000,
        )
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) {
            return
        }

        val session = session ?: return
        if (session.won || uiState.paused || uiState.loading) {
            return
        }

        timerStartedAt = SystemClock.elapsedRealtime() - elapsedMillis
        timerJob = viewModelScope.launch {
            while (isActive) {
                elapsedMillis = SystemClock.elapsedRealtime() - timerStartedAt
                val seconds = elapsedMillis / 1000

                if (seconds != uiState.elapsedSeconds) {
                    uiState = uiState.copy(elapsedSeconds = seconds)
                }

                delay(250)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null

        if (timerStartedAt > 0) {
            elapsedMillis = maxOf(elapsedMillis, SystemClock.elapsedRealtime() - timerStartedAt)
            timerStartedAt = 0
        }

        uiState = uiState.copy(elapsedSeconds = elapsedMillis / 1000)
    }

    override fun onCleared() {
        generationJob?.cancel()
        timerJob?.cancel()
    }
}
