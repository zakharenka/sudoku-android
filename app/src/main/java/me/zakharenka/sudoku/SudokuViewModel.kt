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
import me.zakharenka.sudoku.data.SavedGameStore
import me.zakharenka.sudoku.data.ThemeMode
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.MoveOutcome
import me.zakharenka.sudoku.game.SavedGame
import me.zakharenka.sudoku.game.SudokuGenerator
import me.zakharenka.sudoku.game.SudokuSession

data class GameUiState(
    val difficulty: Difficulty = Difficulty.EASY,
    val cells: List<Cell> = emptyList(),
    val selectedIndex: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val hintsRemaining: Int = SudokuSession.MAX_HINTS,
    val elapsedSeconds: Long = 0,
    val paused: Boolean = false,
    val won: Boolean = false,
    val loading: Boolean = true,
    val bestTimes: Map<Difficulty, Long?> = emptyMap(),
    val solvedCounts: Map<Difficulty, Int> = emptyMap(),
    val isNewRecord: Boolean = false,
    val canUndo: Boolean = false,
    val canErase: Boolean = false,
    val gameId: Long = 0,
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val records = RecordsStore(application)
    private val savedGames = SavedGameStore(application)
    private val generator = SudokuGenerator()

    private var session: SudokuSession? = null
    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var elapsedMillis = 0L
    private var timerStartedAt = 0L
    private var bestTimes = records.loadBestTimes()
    private var solvedCounts = records.loadSolvedCounts()
    private var notesMode = records.notesMode

    var themeMode by mutableStateOf(records.themeMode)
        private set

    var straightGrid by mutableStateOf(records.straightGrid)
        private set

    var sameHighlight by mutableStateOf(records.sameHighlight)
        private set

    var handwrittenDigits by mutableStateOf(records.handwrittenDigits)
        private set

    var uiState by mutableStateOf(
        GameUiState(
            notesMode = notesMode,
            bestTimes = bestTimes,
            solvedCounts = solvedCounts,
        ),
    )
        private set

    init {
        val saved = savedGames.load()
        if (saved == null || !restoreSavedGame(saved)) {
            newGame(Difficulty.EASY)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        records.themeMode = mode
    }

    fun updateStraightGrid(enabled: Boolean) {
        straightGrid = enabled
        records.straightGrid = enabled
    }

    fun updateSameHighlight(enabled: Boolean) {
        sameHighlight = enabled
        records.sameHighlight = enabled
    }

    fun updateHandwrittenDigits(enabled: Boolean) {
        handwrittenDigits = enabled
        records.handwrittenDigits = enabled
    }

    fun clearBestTimes() {
        records.clearBestTimes()
        bestTimes = records.loadBestTimes()
        solvedCounts = records.loadSolvedCounts()
        uiState = uiState.copy(
            bestTimes = bestTimes,
            solvedCounts = solvedCounts,
        )
    }

    fun newGame(difficulty: Difficulty = uiState.difficulty) {
        generationJob?.cancel()
        stopTimer()
        savedGames.clear()
        session = null
        elapsedMillis = 0
        timerStartedAt = 0

        uiState = uiState.copy(
            difficulty = difficulty,
            cells = emptyList(),
            selectedIndex = null,
            notesMode = notesMode,
            mistakes = 0,
            hintsRemaining = SudokuSession.MAX_HINTS,
            elapsedSeconds = 0,
            paused = false,
            won = false,
            loading = true,
            isNewRecord = false,
            canUndo = false,
            canErase = false,
            gameId = uiState.gameId + 1,
        )

        generationJob = viewModelScope.launch {
            val generated = withContext(Dispatchers.Default) { generator.generate(difficulty) }

            if (!isActive) {
                return@launch
            }

            session = SudokuSession(
                difficulty = difficulty,
                solution = generated.solution,
                puzzle = generated.puzzle,
                notesMode = notesMode,
            )
            uiState = uiState.copy(loading = false)
            refresh()
            startTimer()
        }
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

        session.toggleNotes()
        notesMode = session.notesMode
        records.notesMode = notesMode
        refresh()
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
        uiState = uiState.copy(paused = true)
    }

    fun resume() {
        if (!uiState.paused) {
            return
        }

        uiState = uiState.copy(paused = false)
        startTimer()
    }

    fun onAppBackgrounded() {
        if (!uiState.loading && !uiState.won) {
            if (!uiState.paused) {
                pause()
            }
            saveCurrentGame()
        } else {
            saveCurrentGame()
        }
    }

    private fun handleOutcome(session: SudokuSession, outcome: MoveOutcome) {
        if (outcome.won) {
            finishWin(session)
            return
        }

        if (outcome.changed) {
            refresh()
        }
    }

    private fun finishWin(session: SudokuSession) {
        stopTimer()
        savedGames.clear()

        val seconds = elapsedMillis / 1000
        val isRecord = records.saveBestTime(session.difficulty, seconds)
        solvedCounts = records.incrementSolvedCount(session.difficulty)

        if (isRecord) {
            bestTimes = records.loadBestTimes()
        }

        refresh(isNewRecord = isRecord)
    }

    private fun refresh(
        isNewRecord: Boolean = uiState.isNewRecord,
    ) {
        val session = session ?: return

        uiState = uiState.copy(
            difficulty = session.difficulty,
            cells = session.cells.toList(),
            selectedIndex = session.selected,
            notesMode = session.notesMode,
            mistakes = session.mistakes,
            hintsRemaining = session.hintsRemaining,
            won = session.won,
            bestTimes = bestTimes,
            solvedCounts = solvedCounts,
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

    private fun restoreSavedGame(saved: SavedGame): Boolean {
        if (saved.won) {
            savedGames.clear()
            return false
        }

        val restored = runCatching { SudokuSession.restore(saved) }.getOrNull()
        if (restored == null) {
            savedGames.clear()
            return false
        }

        session = restored
        notesMode = restored.notesMode
        records.notesMode = notesMode
        elapsedMillis = saved.elapsedSeconds * 1000
        timerStartedAt = 0

        uiState = uiState.copy(
            difficulty = restored.difficulty,
            cells = restored.cells.toList(),
            selectedIndex = restored.selected,
            notesMode = restored.notesMode,
            mistakes = restored.mistakes,
            hintsRemaining = restored.hintsRemaining,
            elapsedSeconds = saved.elapsedSeconds,
            paused = true,
            won = false,
            loading = false,
            bestTimes = bestTimes,
            solvedCounts = solvedCounts,
            isNewRecord = false,
            canUndo = false,
            canErase = restored.canErase,
            gameId = uiState.gameId + 1,
        )

        return true
    }

    private fun saveCurrentGame() {
        val current = session

        if (current == null || current.won || uiState.loading) {
            savedGames.clear()
            return
        }

        savedGames.save(current.toSavedGame(elapsedMillis / 1000))
    }

    override fun onCleared() {
        if (!uiState.loading && !uiState.won) {
            stopTimer()
            saveCurrentGame()
        }

        generationJob?.cancel()
        timerJob?.cancel()
    }
}
