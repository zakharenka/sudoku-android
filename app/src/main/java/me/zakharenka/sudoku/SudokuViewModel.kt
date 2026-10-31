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
import me.zakharenka.sudoku.game.GameMessage
import me.zakharenka.sudoku.game.MoveOutcome
import me.zakharenka.sudoku.game.SavedGame
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
    val status: String = "",
    val statusKind: StatusKind = StatusKind.NEUTRAL,
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
    private var lastLocale = currentLocaleTag()

    var themeMode by mutableStateOf(records.themeMode)
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
        uiState = uiState.copy(
            status = getApplication<Application>().getString(R.string.status_initial),
        )

        val saved = savedGames.load()
        if (saved == null || !restoreSavedGame(saved)) {
            newGame(Difficulty.EASY)
        }
    }

    fun onLocaleChanged() {
        val current = currentLocaleTag()
        if (lastLocale == current) {
            return
        }

        val app = getApplication<Application>()
        val status = when {
            uiState.loading -> app.getString(R.string.status_generating)
            uiState.won -> app.getString(
                if (uiState.isNewRecord) R.string.status_new_record else R.string.status_solved,
                formatTime(uiState.elapsedSeconds),
            )
            uiState.paused -> app.getString(R.string.status_paused)
            else -> app.getString(R.string.status_initial)
        }

        uiState = uiState.copy(
            status = status,
            statusKind = if (uiState.won) StatusKind.SUCCESS else StatusKind.NEUTRAL,
        )
        lastLocale = current
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        records.themeMode = mode
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
            elapsedSeconds = 0,
            paused = false,
            won = false,
            loading = true,
            status = getApplication<Application>().getString(R.string.status_generating),
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
                val app = getApplication<Application>()
                val label = app.getString(difficulty.labelRes())
                refresh(
                    status = app.getString(
                        R.string.status_new_game,
                        label,
                        generated.clues,
                    ),
                )
                startTimer()
            }.onFailure {
                uiState = uiState.copy(
                    loading = false,
                    status = getApplication<Application>().getString(R.string.status_generation_failed),
                    statusKind = StatusKind.ERROR,
                )
            }
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

        val outcome = session.toggleNotes()
        notesMode = session.notesMode
        records.notesMode = notesMode
        refresh(outcome.message?.let(::localize), outcome.kind)
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
            status = getApplication<Application>().getString(R.string.status_paused),
            statusKind = StatusKind.NEUTRAL,
        )
    }

    fun resume() {
        if (!uiState.paused) {
            return
        }

        uiState = uiState.copy(
            paused = false,
            status = getApplication<Application>().getString(R.string.status_resumed),
            statusKind = StatusKind.NEUTRAL,
        )
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

        val message = outcome.message?.let(::localize)
        if (outcome.changed || message != null) {
            refresh(
                status = message,
                kind = outcome.kind,
            )
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

        val app = getApplication<Application>()
        val time = formatTime(seconds)
        refresh(
            status = if (isRecord) {
                app.getString(R.string.status_new_record, time)
            } else {
                app.getString(R.string.status_solved, time)
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
            elapsedSeconds = saved.elapsedSeconds,
            paused = true,
            won = false,
            loading = false,
            status = getApplication<Application>().getString(R.string.status_restored),
            statusKind = StatusKind.NEUTRAL,
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

    private fun localize(message: GameMessage): String {
        val app = getApplication<Application>()

        return when (message) {
            GameMessage.SelectCellFirst -> app.getString(R.string.message_select_cell)
            GameMessage.CellAlreadyFilled -> app.getString(R.string.message_cell_filled)
            GameMessage.EraseNumberFirst -> app.getString(R.string.message_erase_first)
            is GameMessage.NoteAdded -> app.getString(R.string.message_note_added, message.number)
            is GameMessage.NoteRemoved -> app.getString(R.string.message_note_removed, message.number)
            GameMessage.Correct -> app.getString(R.string.message_correct)
            is GameMessage.WrongNumber -> app.getString(
                R.string.message_wrong_number,
                message.mistakes,
            )
            GameMessage.CellCleared -> app.getString(R.string.message_cell_cleared)
            GameMessage.MoveUndone -> app.getString(R.string.message_move_undone)
            GameMessage.BoardAlreadyCorrect -> app.getString(R.string.message_board_correct)
            is GameMessage.Hint -> app.getString(
                R.string.message_hint,
                message.number,
                message.row,
                message.col,
            )
            GameMessage.CannotEraseGiven -> app.getString(R.string.message_cannot_erase)
            GameMessage.NotesEnabled -> app.getString(R.string.message_notes_enabled)
            GameMessage.NotesDisabled -> app.getString(R.string.message_notes_disabled)
            GameMessage.Solved -> app.getString(R.string.message_solved)
        }
    }

    private fun Difficulty.labelRes(): Int = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    }

    private fun currentLocaleTag(): String =
        getApplication<Application>().resources.configuration.locales[0].toLanguageTag()

    override fun onCleared() {
        if (!uiState.loading && !uiState.won) {
            stopTimer()
            saveCurrentGame()
        }

        generationJob?.cancel()
        timerJob?.cancel()
    }
}
