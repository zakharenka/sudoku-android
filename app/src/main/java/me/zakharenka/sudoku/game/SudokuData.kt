package me.zakharenka.sudoku.game

data class DifficultyRecord(
    val bestSeconds: Long? = null,
    val solved: Int = 0,
)

data class RecordStats(
    val withoutMistakes: DifficultyRecord = DifficultyRecord(),
    val withMistakes: DifficultyRecord = DifficultyRecord(),
) {
    fun forMistakes(hasMistakes: Boolean): DifficultyRecord =
        if (hasMistakes) withMistakes else withoutMistakes
}

data class WinResult(
    val data: SudokuData,
    val isNewRecord: Boolean,
)

/**
 * Все сохранённые данные приложения: текущая игра и история.
 *
 * Records are not stored: they are derived from solved games. A game belongs to
 * the "without mistakes" category when `errors == 0`, otherwise to the
 * "with mistakes" category. Hidden-errors games have `errors == 0`, so they
 * land in the clean category.
 */
data class SudokuData(
    val currentGameId: String? = null,
    val games: List<GameRecord> = emptyList(),
) {
    fun currentGame(): GameRecord? {
        val id = currentGameId ?: return null
        return games.firstOrNull { it.id == id }
    }

    fun recordsByDifficulty(): Map<Difficulty, RecordStats> =
        Difficulty.entries.associateWith { difficulty ->
            var cleanBest: Long? = null
            var cleanSolved = 0
            var mistakeBest: Long? = null
            var mistakeSolved = 0

            games.forEach { game ->
                if (game.solvedAt == null || game.difficulty != difficulty) {
                    return@forEach
                }

                if (game.errors == 0) {
                    cleanSolved += 1
                    cleanBest = minOfNullable(cleanBest, game.elapsedSeconds)
                } else {
                    mistakeSolved += 1
                    mistakeBest = minOfNullable(mistakeBest, game.elapsedSeconds)
                }
            }

            RecordStats(
                withoutMistakes = DifficultyRecord(
                    bestSeconds = cleanBest,
                    solved = cleanSolved,
                ),
                withMistakes = DifficultyRecord(
                    bestSeconds = mistakeBest,
                    solved = mistakeSolved,
                ),
            )
        }

    fun upsertGame(game: GameRecord): SudokuData = copy(
        games = if (games.any { it.id == game.id }) {
            games.map { existing -> if (existing.id == game.id) game else existing }
        } else {
            games + game
        },
    )

    fun withWin(game: GameRecord): WinResult {
        require(game.solvedAt != null)

        val previousBest = games.asSequence()
            .filter { existing ->
                existing.id != game.id &&
                    existing.solvedAt != null &&
                    existing.difficulty == game.difficulty &&
                    (existing.errors == 0) == (game.errors == 0)
            }
            .map { it.elapsedSeconds }
            .minOrNull()
        val isNewRecord = previousBest == null || game.elapsedSeconds < previousBest

        return WinResult(
            data = upsertGame(game).copy(currentGameId = game.id),
            isNewRecord = isNewRecord,
        )
    }

    /**
     * Clears a current game reference that points to a missing game.
     */
    fun normalized(): SudokuData {
        val validCurrent = currentGameId?.takeIf { id -> games.any { it.id == id } }

        return if (validCurrent == currentGameId) this else copy(currentGameId = validCurrent)
    }

    private fun minOfNullable(current: Long?, candidate: Long): Long =
        if (current == null) candidate else minOf(current, candidate)
}
