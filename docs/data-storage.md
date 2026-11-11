# Data storage

## Settings

Global settings are stored in `SharedPreferences` under the name `sudoku`:

| Key | Meaning |
| --- | --- |
| `theme_mode` | `system`, `light` or `dark` |
| `notes_mode` | global notes mode |
| `straight_grid` | board line style |
| `same_highlight` | same-digit highlight |
| `show_errors` | default error mode for a new game |

## Game data

One versioned file, `filesDir/sudoku_data.json`, stores the current game and the
full game history. The format version is 1:

```text
{
  "version": 1,
  "currentGameId": "uuid or null",
  "games": [
    {
      "id": "uuid",
      "difficulty": "easy",
      "showErrors": true,
      "errors": 0,
      "puzzle": "530070000...",
      "solution": "534678912...",
      "values": "530070000...",
      "notes": { "3": [1, 5, 9] },
      "hintsUsed": 1,
      "elapsedSeconds": 300,
      "createdAt": 1762700000000,
      "updatedAt": 1762700300000,
      "solvedAt": null
    }
  ]
}
```

- `currentGameId` always points to the game currently open on the screen, even
  when that game is solved. A missing game starts a new game.
- `solvedAt == null` marks an unfinished game.
- `records` is not stored: best times and solved counters are derived from
  solved games.
- `notes` is sparse: only non-empty cells are present.
- `hintsUsed` counts hints spent in the game; zero means no hints were used.
- `selected`, `notesMode`, `givens` and per-cell error flags are not stored.
  `notesMode` is a global setting; givens come from `puzzle`; errors are derived
  from `values` and `solution` and are always zero in hidden mode.

## Writing and restoring

- Writes go through `AtomicFile`, on `Dispatchers.IO`, under a `Mutex`.
- When the app goes to the background (`MainActivity.onStop`, except for a
  configuration change), `onAppBackgrounded()` pauses and saves the game.
- On the next launch the game referenced by `currentGameId` is restored **on
  pause**; the timer is not running. A solved current game opens on its win
  screen without a running timer.
- The current game is saved after every board change, every 10 seconds while
  the timer runs, on pause or dialog open, when history is opened, when
  switching games, on victory, before export and in `onStop`.
- The JSON has a top-level `version`. Increase it when the format changes and
  reject incompatible data.
- Validate versions, array sizes, digit ranges, notes, UUID references and
  current-game state on load. Corrupted data must lead to a new game or an
  import error, never a crash.

## Import and export

- Both use the system file picker: `ACTION_CREATE_DOCUMENT` and
  `ACTION_OPEN_DOCUMENT` with MIME `application/json`.
- Export saves the current game first and writes the same `version`,
  `currentGameId` and `games` structure. The filename is
  `sudoku-backup-YYYYMMDD-HHmm.json`.
- Import uses the same parser and validator as normal loading.
- After confirmation, imported data replaces the current data. A
  `currentGameId` that points to a missing game is reset to `null`; a solved
  game is a valid current game.
- An invalid file changes nothing and shows a localized error message.
- Theme, language and display modes are not included in the backup.

## Migration

Old SharedPreferences records (`best:*`, `solved:*`) and `saved_game` are not
migrated and are ignored. Do not rename the settings keys without a migration.
