package me.zakharenka.sudoku

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.zakharenka.sudoku.data.DataDecodeResult
import me.zakharenka.sudoku.data.DataError
import me.zakharenka.sudoku.data.DataTransferResult
import me.zakharenka.sudoku.data.SettingsStore
import me.zakharenka.sudoku.data.SudokuDataCodec
import me.zakharenka.sudoku.data.SudokuDataStore
import me.zakharenka.sudoku.data.ThemeMode
import me.zakharenka.sudoku.game.Cell
import me.zakharenka.sudoku.game.Difficulty
import me.zakharenka.sudoku.game.GameRecord
import me.zakharenka.sudoku.game.MoveOutcome
import me.zakharenka.sudoku.game.RecordStats
import me.zakharenka.sudoku.game.SudokuData
import me.zakharenka.sudoku.game.SudokuGenerator
import me.zakharenka.sudoku.game.SudokuSession

data class GameUiState(
    val difficulty: Difficulty = Difficulty.EASY,
    val cells: List<Cell> = emptyList(),
    val selectedIndex: Int? = null,
    val notesMode: Boolean = false,
    val showErrors: Boolean = true,
    val mistakes: Int = 0,
    val hintsRemaining: Int = SudokuSession.MAX_HINTS,
    val elapsedSeconds: Long = 0,
    val paused: Boolean = false,
    val won: Boolean = false,
    val loading: Boolean = true,
    val records: Map<Difficulty, RecordStats> = emptyMap(),
    val history: List<GameRecord> = emptyList(),
    val currentGameId: String? = null,
    val isNewRecord: Boolean = false,
    val canUndo: Boolean = false,
    val canErase: Boolean = false,
    val gameId: Long = 0,
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val dataStore = SudokuDataStore(application)
    private val generator = SudokuGenerator()
    private val saveMutex = Mutex()

    private var data = SudokuData()
    private var session: SudokuSession? = null
    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var elapsedMillis = 0L
    private var timerStartedAt = 0L
    private var notesMode = settings.notesMode

    var themeMode by mutableStateOf(settings.themeMode)
        private set

    var straightGrid by mutableStateOf(settings.straightGrid)
        private set

    var sameHighlight by mutableStateOf(settings.sameHighlight)
        private set

    var showErrorsDefault by mutableStateOf(settings.showErrors)
        private set

    var uiState by mutableStateOf(
        GameUiState(
            notesMode = notesMode,
            showErrors = showErrorsDefault,
        ),
    )
        private set

    init {
        viewModelScope.launch {
            data = dataStore.load()

            val current = data.currentGame()

            if (current != null) {
                restoreGame(current)
            } else {
                startGame(Difficulty.EASY, showErrorsDefault)
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        settings.themeMode = mode
    }

    fun updateStraightGrid(enabled: Boolean) {
        straightGrid = enabled
        settings.straightGrid = enabled
    }

    fun updateSameHighlight(enabled: Boolean) {
        sameHighlight = enabled
        settings.sameHighlight = enabled
    }

    fun openRecords() {
        uiState = uiState.copy(records = data.recordsByDifficulty())
    }

    fun newGame(
        difficulty: Difficulty = uiState.difficulty,
        showErrors: Boolean = showErrorsDefault,
    ) {
        showErrorsDefault = showErrors
        settings.showErrors = showErrors
        startGame(difficulty, showErrors)
    }

    fun openHistory() {
        val current = session?.takeIf { !it.won && !uiState.loading }

        if (current != null) {
            val record = current.toGameRecord(
                elapsedSeconds = elapsedMillis / 1000,
                updatedAt = System.currentTimeMillis(),
            )
            data = data.upsertGame(record).copy(currentGameId = record.id)
            saveData()
        }

        uiState = uiState.copy(
            history = data.games.sortedByDescending { it.createdAt },
            currentGameId = data.currentGameId,
        )
    }

    fun loadGame(gameId: String) {
        val game = data.games.firstOrNull { it.id == gameId } ?: return

        if (session?.id == gameId) {
            return
        }

        generationJob?.cancel()
        stopTimer()
        persistCurrentGame()

        data = data.copy(currentGameId = game.id)
        saveData()
        restoreGame(game, startPaused = !game.solved)
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

    fun moveSelection(rowDelta: Int, colDelta: Int) {
        if (uiState.loading || uiState.paused) {
            return
        }

        val session = session ?: return
        if (session.won) {
            return
        }

        session.moveSelection(rowDelta, colDelta)
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
        settings.notesMode = notesMode
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
        persistCurrentGame()
        saveData()
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
            } else {
                persistCurrentGame()
                saveData()
            }
        } else {
            saveData()
        }
    }

    fun exportData(uri: Uri, onResult: (DataTransferResult) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                persistCurrentGame()
                saveCurrentData()
                val payload = SudokuDataCodec.encode(data)

                withContext(Dispatchers.IO) {
                    val output = getApplication<Application>().contentResolver
                        .openOutputStream(uri, "wt")
                        ?: throw IOException("Cannot open export stream")

                    output.use { stream ->
                        stream.write(payload.toByteArray())
                    }
                }
            }

            onResult(
                if (result.isSuccess) {
                    DataTransferResult.Success
                } else {
                    DataTransferResult.Failure(DataError.IO_ERROR)
                },
            )
        }
    }

    fun importData(uri: Uri, onResult: (DataTransferResult) -> Unit) {
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }

            if (raw == null) {
                onResult(DataTransferResult.Failure(DataError.IO_ERROR))
                return@launch
            }

            when (val decoded = SudokuDataCodec.decode(raw)) {
                is DataDecodeResult.Failure -> {
                    onResult(DataTransferResult.Failure(decoded.error))
                    return@launch
                }

                is DataDecodeResult.Success -> {
                    val imported = decoded.data.normalized()
                    val saved = runCatching { saveCurrentData(imported) }

                    if (saved.isFailure) {
                        onResult(DataTransferResult.Failure(DataError.IO_ERROR))
                        return@launch
                    }

                    generationJob?.cancel()
                    stopTimer()
                    session = null
                    elapsedMillis = 0
                    timerStartedAt = 0
                    data = imported
                    uiState = uiState.copy(
                        history = data.games.sortedByDescending { it.createdAt },
                        currentGameId = data.currentGameId,
                    )

                    val current = data.currentGame()

                    if (current != null) {
                        restoreGame(current)
                    } else {
                        startGame(uiState.difficulty, showErrorsDefault, startPaused = true)
                    }

                    onResult(DataTransferResult.Success)
                }
            }
        }
    }

    private fun startGame(
        difficulty: Difficulty,
        showErrors: Boolean,
        startPaused: Boolean = false,
    ) {
        generationJob?.cancel()
        stopTimer()

        val previous = session?.takeIf { !it.won && !uiState.loading }

        if (previous != null) {
            data = data.upsertGame(
                previous.toGameRecord(
                    elapsedSeconds = elapsedMillis / 1000,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        session = null
        elapsedMillis = 0
        timerStartedAt = 0

        uiState = uiState.copy(
            difficulty = difficulty,
            showErrors = showErrors,
            cells = emptyList(),
            selectedIndex = null,
            notesMode = notesMode,
            mistakes = 0,
            hintsRemaining = SudokuSession.MAX_HINTS,
            elapsedSeconds = 0,
            paused = startPaused,
            won = false,
            loading = true,
            isNewRecord = false,
            canUndo = false,
            canErase = false,
            currentGameId = data.currentGameId,
            gameId = uiState.gameId + 1,
        )

        saveData()

        generationJob = viewModelScope.launch {
            val generated = withContext(Dispatchers.Default) { generator.generate(difficulty) }

            if (!isActive) {
                return@launch
            }

            val now = System.currentTimeMillis()
            val newSession = SudokuSession(
                difficulty = difficulty,
                solution = generated.solution,
                puzzle = generated.puzzle,
                notesMode = notesMode,
                showErrors = showErrors,
            )

            session = newSession
            data = data.upsertGame(
                newSession.toGameRecord(
                    elapsedSeconds = 0,
                    updatedAt = now,
                ),
            ).copy(currentGameId = newSession.id)
            saveData()

            uiState = uiState.copy(loading = false)
            refresh()

            if (!startPaused) {
                startTimer()
            }
        }
    }

    private fun handleOutcome(session: SudokuSession, outcome: MoveOutcome) {
        if (outcome.won) {
            finishWin(session)
            return
        }

        if (outcome.changed) {
            refresh()
            persistCurrentGame()
            saveData()
        }
    }

    private fun finishWin(session: SudokuSession) {
        stopTimer()

        val now = System.currentTimeMillis()
        val record = session.toGameRecord(
            elapsedSeconds = elapsedMillis / 1000,
            updatedAt = now,
            solvedAt = now,
        )
        val win = data.withWin(record)
        data = win.data
        saveData()

        refresh(isNewRecord = win.isNewRecord)
    }

    private fun refresh(
        isNewRecord: Boolean = uiState.isNewRecord,
    ) {
        val session = session ?: return

        uiState = uiState.copy(
            difficulty = session.difficulty,
            showErrors = session.showErrors,
            cells = session.cells.toList(),
            selectedIndex = session.selected,
            notesMode = session.notesMode,
            mistakes = session.mistakes,
            hintsRemaining = session.hintsRemaining,
            won = session.won,
            isNewRecord = isNewRecord,
            canUndo = session.canUndo,
            canErase = session.canErase,
            elapsedSeconds = elapsedMillis / 1000,
            currentGameId = data.currentGameId,
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
        var lastCheckpointAt = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (isActive) {
                elapsedMillis = SystemClock.elapsedRealtime() - timerStartedAt
                val seconds = elapsedMillis / 1000

                if (seconds != uiState.elapsedSeconds) {
                    uiState = uiState.copy(elapsedSeconds = seconds)
                }

                if (SystemClock.elapsedRealtime() - lastCheckpointAt >= CHECKPOINT_INTERVAL_MS) {
                    lastCheckpointAt = SystemClock.elapsedRealtime()
                    persistCurrentGame()
                    saveData()
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

    private fun restoreGame(
        game: GameRecord,
        startPaused: Boolean = true,
    ) {
        val restored = runCatching { SudokuSession.restore(game, notesMode) }.getOrNull()

        if (restored == null) {
            data = data.copy(currentGameId = null)
            saveData()
            startGame(Difficulty.EASY, showErrorsDefault)
            return
        }

        session = restored
        notesMode = restored.notesMode
        settings.notesMode = notesMode
        elapsedMillis = game.elapsedSeconds * 1000
        timerStartedAt = 0
        data = data.copy(currentGameId = game.id)

        uiState = uiState.copy(
            difficulty = restored.difficulty,
            showErrors = restored.showErrors,
            cells = restored.cells.toList(),
            selectedIndex = restored.selected,
            notesMode = restored.notesMode,
            mistakes = restored.mistakes,
            hintsRemaining = restored.hintsRemaining,
            elapsedSeconds = game.elapsedSeconds,
            paused = startPaused && !restored.won,
            won = restored.won,
            loading = false,
            isNewRecord = false,
            canUndo = false,
            canErase = restored.canErase,
            currentGameId = data.currentGameId,
            gameId = uiState.gameId + 1,
        )

        if (!startPaused && !restored.won) {
            startTimer()
        }
    }

    private fun persistCurrentGame() {
        val current = session ?: return

        if (current.won || uiState.loading) {
            return
        }

        val record = current.toGameRecord(
            elapsedSeconds = elapsedMillis / 1000,
            updatedAt = System.currentTimeMillis(),
        )
        data = data.upsertGame(record).copy(currentGameId = record.id)
    }

    private fun saveData() {
        viewModelScope.launch {
            saveMutex.withLock {
                runCatching { dataStore.save(data) }
            }
        }
    }

    private suspend fun saveCurrentData(dataToSave: SudokuData = data) {
        saveMutex.withLock {
            dataStore.save(dataToSave)
        }
    }

    private companion object {
        const val CHECKPOINT_INTERVAL_MS = 10_000L
    }

    override fun onCleared() {
        stopTimer()
        generationJob?.cancel()
        timerJob?.cancel()
    }
}
