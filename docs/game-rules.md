# Game rules and behavior

## Puzzle

- The board is a classic 9×9 Sudoku.
- Every row, column and 3×3 box must contain the digits 1 to 9 exactly once.
- `SudokuGenerator` creates a complete solution and removes digits down to the
  target clue count while checking that the solution stays unique.
- There are four difficulty levels: Easy, Medium, Hard and Expert.

## Input and notes

- A digit fills the selected non-given cell, replacing a previous value if
  present.
- Notes mode writes candidate digits into an empty cell.
- Undo reverts the last move.
- Erase clears the selected normal cell. Givens cannot be erased.
- Mistakes are highlighted in red when the game shows mistakes. In hidden mode
  errors are not highlighted and the stored error count stays at zero.

## Hints

- Each game has at most `SudokuSession.MAX_HINTS` (3) hints.
- The remaining count is shown as a badge on the hint button; at zero the
  button is disabled.
- `hintsUsed` is persisted in the game record and restored with the game.
- A hint fills a normal, erasable cell: it is not a given and can be erased.
- Undo can revert the hint's cell value, but does not refund the hint counter.
- Erasing a hinted cell does not refund the hint.
- A new game resets the counter.

## Victory and records

- A game is solved when every cell matches the solution.
- On victory the game joins **No mistakes** when `errors == 0` and **With
  mistakes** otherwise.
- Each solved game belongs to exactly one category and can set or update the
  best time in that category.
- `Best results` shows `Solved: N` under each difficulty for the selected
  category; the sum of both categories is the total number of solved games.
- Hidden-errors games keep `errors == 0`, so they land in the **No mistakes**
  category.
- Hints do not affect the category or forbid a record.
- There is no "Reset records" action: records always reflect the whole game
  history.

## History

- The history contains the current game, unfinished games and solved games.
- Games are grouped by the start date in descending order.
- Each row shows a mini puzzle grid, difficulty, a solved or in-progress icon,
  start date and time, solved date (or last-update date for unfinished games)
  and elapsed time.
- The bottom-right corner shows mistakes and used hints in one line, for
  example `1 mistake, 3 hints`. Zero values are omitted, so the line may show
  only mistakes, only hints or nothing.
- Starting a new game keeps the previous unfinished game in the history.
- Tapping a history row loads that game on pause and makes it current,
  including solved games. The previously open game is saved first.
- `currentGameId` always points to the game currently open on the screen, even
  when that game is already solved.
- The puzzle mini-grid in a row is large enough to match the height of the
  information block, and the elapsed time is aligned to the right edge.

## Game lifecycle

- The current game is saved automatically and restored on pause next time the
  app starts.
- The game is paused while a dialog is open, when the app goes to the
  background and when the user opens pause.
- A solved current game stays in the history and remains current until another
  game is opened.
