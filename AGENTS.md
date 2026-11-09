# AGENTS.md

Instructions for AI agents and developers working with this repository.

## What this project is

A native Android Sudoku app built with Kotlin and Jetpack Compose. It is a
standalone project, not a wrapper around a browser game: no WebView, JavaScript
or web game engine is used here.

The interface style is a hand-drawn sketch: paper texture, wobbly lines, a
square grid with no rounded corners, and the Neucha font. Keep this style in
every new screen and element.

## Quick commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Unit tests for game logic
./gradlew :app:testDebugUnitTest

# Android Lint
./gradlew :app:lintDebug

# Release build with R8 (unsigned)
./gradlew :app:assembleRelease

# Full check before committing
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Installing on a device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n me.zakharenka.sudoku/.MainActivity
```

Requirements: JDK 17+, Android SDK 37, Gradle Wrapper 9.6. `local.properties`
contains the local SDK path and is not committed.

## Versions and releases

- The single source of the version is `app.version` in `gradle.properties`.
- The format is strictly semantic: `MAJOR.MINOR.PATCH`.
- `versionName` comes from `app.version`, and `versionCode` is calculated in
  `app/build.gradle.kts` as
  `major * 1_000_000 + minor * 1_000 + patch`.
- Do not hardcode the version in interface strings: use
  `BuildConfig.VERSION_NAME`.
- Release versions are marked with an annotated git tag, for example `1.0.4`.
- For Google Play, build `./gradlew :app:bundleRelease`; the AAB in
  `app/build/outputs/bundle/release/` must be signed with the upload key.
- The publishing package is assembled in `store_out/`. The folder is in
  `.gitignore` and must not be committed.
- F-Droid builds from tagged source. The build recipe is
  `metadata/me.zakharenka.sudoku.yml`; the source repository must stay public.
  `fdroid/version.json` mirrors `app.version` and the computed `versionCode`
  for F-Droid's tag-based auto-update. Keep it in sync; `FdroidVersionTest`
  enforces this. After a release, F-Droid adds the build entry automatically.

## Architecture

```text
MainActivity
  └── SudokuViewModel (GameUiState)
        ├── SudokuSession      — game rules, history, victory
        ├── SudokuGenerator    — puzzle generation with a single solution
        ├── SettingsStore      — global settings only
        ├── SudokuDataStore    — atomic JSON storage for the current game and history
        └── SudokuDataCodec    — shared JSON parser/validator for load and import

SudokuScreen
  ├── SudokuBoard            — 9×9 board rendering
  ├── SudokuControls         — top bar, digits, actions, overlays
  └── sketch/                — hand-drawn surfaces, buttons, icons
```

Separation rules:

- `game/` — pure Kotlin with no Android imports. All rules, generation,
  history models and formatting live here. This code must remain testable with
  plain JUnit tests.
- `data/` — SharedPreferences settings and the atomic JSON file. It contains no
  game rules; JSON parsing/validation is shared by normal loading and import.
- `SudokuViewModel` — the only place where rules, the timer, background
  generation and persistence meet.
- `ui/` — only renders `GameUiState` and sends actions to the ViewModel. The UI
  must not compute move legality, victory or mistakes.

## Required invariants

### New game

- The new-game button (the rightmost icon in the top bar) opens a dialog.
- Tapping the **current difficulty** in the top bar also opens this dialog.
- In the dialog, tapping a difficulty only **selects** it.
- The game starts only with the **Start** button.
- "Cancel" closes the dialog without changes.
- The dialog must explain that the current game will be saved to history.
- The dialog has a **Show mistakes** checkbox. Its value becomes the default
  for the next new game (`show_errors` in SharedPreferences) and is stored in
  the game data for the whole game.
- Do not bring back starting a game immediately when a difficulty row is
  tapped.

### Top bar and header

- The header is one row: a hand-drawn icon and the app name.
- There must be no "Logic without rush" slogan.
- The in-game top bar is one row:
  - on the left, one block: difficulty, mistakes (only when errors are shown),
    timer;
  - on the right, two square unlabelled buttons: pause and new game.
- Icon buttons must have a `contentDescription` from string resources.

### Settings

- Settings contain only the theme, board line style, same-digit highlight,
  handwritten digits, language, data import/export, "About" and a link to the
  developer website `https://zakharenka.me`.
- The 9×9 board uses straight grid lines by default. The settings toggle can
  switch them back to hand-drawn wobbly lines; this affects only the board, not
  buttons, panels or icons.
- Same-digit highlighting can be disabled. This removes the highlight and the
  special text colour from cells with the selected digit; peer highlighting
  remains.
- "Rules", "Records" and "History" live in the header and are not duplicated in
  settings.
- The language is changed only through the native Android screen
  (`android.settings.APP_LOCALE_SETTINGS`), with no in-app language picker.
- Import and export use the system file picker (SAF) and are located in the
  settings dialog.

### Records

- Records are not stored separately. Best times and solved counters are derived
  from solved games in the history.
- There are two categories per difficulty:
  - **No mistakes** — wins with `errors == 0`;
  - **With mistakes** — wins with `errors > 0`.
- The records dialog has a switch between the two categories.
- Each solved game belongs to exactly one category and can set or update the
  best time in that category.
- "Best results" shows `Solved: N` under each difficulty for the selected
  category; the sum of both categories is the total number of solved games.
- Hidden-errors games keep `errors == 0`, so they land in the **No mistakes**
  category.
- Hints do not affect the category or forbid a record.
- There is no "Reset records" action: records always reflect the whole game
  history.

### History

- A separate header button next to records opens the game history.
- Games are grouped by the start date (descending). Each row shows a mini
  puzzle grid, difficulty, start date/time, solved date (or last-update date
  for unfinished games), elapsed time and mistakes (or "off" in hidden mode).
- The history contains the current game, unfinished games and solved games.
- `currentGameId` always points to the game currently open on the screen, even
  when that game is already solved. `solvedAt == null` marks an unfinished
  game.
- Starting a new game keeps the previous unfinished game in the history.
- Tapping a history row loads that game on pause and makes it current,
  including solved games. The previously open game is saved first.
- The puzzle mini-grid in a row is large enough to match the height of the
  information block, and the elapsed time is aligned to the right edge.

### Keyboard

- Digits `1–9` on the main row and numpad enter the selected digit.
- `0`, Backspace and Delete erase the selected cell.
- Arrow keys move the selection one cell and stop at the board edges.
- Keyboard input is ignored while a dialog is open, during loading, pause or
  victory.
- Focus must return to the board after a dialog closes and after resume.

### Hints

- Each game has at most `SudokuSession.MAX_HINTS` (3) hints.
- The remaining count is shown as a badge on the hint button; at zero the
  button is disabled.
- `hintsUsed` is persisted in the game record and restored with the game.
- A hint fills a normal, erasable cell: it is not a given and can be erased.
- Undo can revert the hint's cell value, but does not refund the hint counter.
- Erasing a hinted cell does not refund the hint.
- A new game resets the counter.

### Game persistence

- One JSON file `sudoku_data.json` in `filesDir` stores the current game and the
  full game history. Records are derived from solved games. Writes go through
  `AtomicFile`, on `Dispatchers.IO`, under a `Mutex`.
- When the app goes to the background (`MainActivity.onStop`, except for a
  configuration change), `onAppBackgrounded()` is called: the game is paused
  and saved.
- On the next launch the game referenced by `currentGameId` is restored **on
  pause**; the timer is not running. A solved current game opens on its win
  screen without a running timer.
- A missing current game starts a new game. The current game is saved after
  every board change, every 10 seconds while the timer runs, on pause/dialog
  open, when history is opened, when switching games, on win, before export and
  in `onStop`.
- The JSON has a top-level `version`. Increase it when the format changes and
  reject incompatible data.
- Validate versions, array sizes, digit ranges, notes, UUID references and
  current-game state on load. Corrupted data must lead to a new game or an
  import error, never a crash.
- Old SharedPreferences records (`best:*`, `solved:*`) and `saved_game` are not
  migrated and are ignored.
- `selected`, `notesMode`, `givens` and per-cell error flags are not stored.
  `notesMode` is a global setting; givens come from `puzzle`; errors are derived
  from `values` and `solution` and are always zero in hidden mode.

### Import and export

- Both use the system file picker: `ACTION_CREATE_DOCUMENT` and
  `ACTION_OPEN_DOCUMENT` with MIME `application/json`.
- Export saves the current game first and writes the same `version`,
  `currentGameId` and `games` structure. The filename is
  `sudoku-backup-YYYYMMDD-HHmm.json`.
- Import uses the same parser/validator as normal loading.
- After confirmation, imported data replaces the current data. A
  `currentGameId` that points to a missing game is reset to `null`; a solved
  game is a valid current game.
- An invalid file changes nothing and shows a localized error message.
- Theme, language and display modes are not included in the backup.

### Localization

- Supported languages: `en` (default), `ru`, `es`, `pt-BR`, `fr`, `de`, `it`,
  `uk`, `be`.
- All user-facing strings must be in `values*/strings.xml`; plural forms in
  `values*/plurals.xml`.
- When adding a language, update `locales_config.xml`, `README.md` and
  `docs/languages.md`.
- Use `plurals.xml` and `pluralStringResource` for plurals. Russian, Ukrainian
  and Belarusian need the `one`/`few`/`many`/`other` categories; Arabic needs
  all six CLDR categories.
- Do not hardcode text in Compose code, including `contentDescription`.

### Tablets

- At widths of `900.dp` and above, use the two-column layout: board on the
  left, keypad and actions on the right.
- Below `900.dp` — a single centred column (phones and portrait tablets).
- The panel is capped at `1120.dp` on wide screens and `720.dp` in compact
  mode.
- In wide mode the board size is limited by width, height and a maximum of
  `760.dp`: `min(760.dp, availableWidth * 0.56, availableHeight - 160.dp)`,
  so the bottom row is not clipped on 7-inch tablets in landscape.
- Test changes on `Medium_Tablet` in portrait and landscape.

### Theme and style

- Use `SudokuTheme.colors` and `HandFont`, not default Material colours.
- For panels, buttons, lines and overlays, use the helpers in
  `ui/sketch/Sketch.kt`; for icons, use `ui/sketch/SketchIcons.kt`.
- Do not add rounded corners, gradients or "glossy" elements that break the
  sketch style.
- Draw new icons in the same hand-drawn style; do not use Material Icons.
- Board digits are drawn by `ui/sketch/HandwrittenDigits.kt` and animate on
  input and erase. Handwritten digits are enabled by default; the settings
  toggle switches cell values to plain `Text` at the same visual size and
  disables the stroke animation. Do not remove the toggle.

### App icon

- The icon is adaptive: light, dark and monochrome layers.
- The foreground content is scaled to 80% around the centre
  (`scaleX`/`scaleY` = 0.8, `pivotX`/`pivotY` = 54) so masks do not clip the
  artwork. Do not remove this padding without a visual check.
- The monochrome layer must contain a digit.

## Data storage

`SharedPreferences` (`sudoku`) contains settings only:

| Key | Meaning |
| --- | --- |
| `theme_mode` | light, dark or system |
| `notes_mode` | global notes mode |
| `straight_grid` | board line style |
| `same_highlight` | same-digit highlight |
| `handwritten_digits` | handwritten digits toggle |
| `show_errors` | default error mode for a new game |

`filesDir/sudoku_data.json` is the single versioned JSON file (version 1):

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

`records` is not stored: it is derived from solved games. `notes` is sparse:
only non-empty cells are present. `solvedAt == null` means an unfinished game.
Old `best:*`, `solved:*` and `saved_game` keys are intentionally ignored; do not
rename the settings keys without a migration.

## Code style

- Kotlin official style, 4-space indentation, trailing commas in multiline
  parameter lists.
- Composable functions use `PascalCase`; regular functions use `camelCase`.
- Keep dialog state in `rememberSaveable` so it survives Activity recreation.
- Prefer small functions and existing helpers over new abstractions.
- Do not add dependencies without need. The project currently uses only
  Compose, lifecycle, core-ktx and test libraries.
- Do not change `applicationId` or `namespace` (`me.zakharenka.sudoku`).
- Do not commit `local.properties`, `build/`, `.gradle/`, `.cxx/` or other
  build artifacts.

## Tests

- Unit tests live in `app/src/test/java/me/zakharenka/sudoku/`; game logic
  tests are under `game/`.
- `FdroidVersionTest` keeps `fdroid/version.json` in sync with `BuildConfig`.
- When changing rules, the generator, persistence or formatting, add or update
  tests.
- There are no UI tests yet; do not add instrumentation tests unless necessary.
- Before completing a task, run the full check:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Lint warnings about available Gradle/AGP/dependency updates are expected and
are not errors.

## Git

- Commit messages follow Conventional Commits, in English, in the imperative
  mood: `feat: ...`, `fix: ...`.
- List the changes in the commit body.
- Commit only when the user asks. Do not amend, rebase or force-push without an
  explicit request.
- Before committing, check `git diff --check` and make sure no stray files are
  included.
- `store_out/` is a local Google Play package; it is in `.gitignore` and is
  not committed.

## Emulator and visual checks

- For screenshots: `adb exec-out screencap -p > /tmp/opencode/screen.png`.
- Switching the app language:

```bash
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ru
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ""
```

- In this environment the emulator sometimes crashes with
  `SIGSEGV`/`SIGKILL`. This is an environment problem, not an app problem. The
  source of truth is a successful build, tests and Lint; do not change app code
  to "work around" an emulator crash.
- If the emulator is unavailable, the visual check can be postponed, but the
  build and tests must still be run.

## Key files

| File | Responsibility |
| --- | --- |
| `SudokuViewModel.kt` | state, timer, generation, persistence, import/export |
| `game/SudokuSession.kt` | rules, undo, victory, game record snapshot |
| `game/SudokuData.kt` | history model and records derived from solved games |
| `game/GameRecord.kt` | one saved game and its validation |
| `game/SudokuGenerator.kt` | generation and solution counting |
| `data/SettingsStore.kt` | global settings in SharedPreferences |
| `data/SudokuDataStore.kt` | atomic `sudoku_data.json` reads and writes |
| `data/SudokuDataCodec.kt` | shared JSON serialization, validation and import errors |
| `ui/SudokuScreen.kt` | header, screen, settings, data transfer, rules, records, history, new-game dialog |
| `ui/SudokuControls.kt` | top bar, keypad, actions, overlays |
| `ui/sketch/Sketch.kt` | hand-drawn surfaces, buttons, lines, texture |
| `ui/sketch/HandwrittenDigits.kt` | animated pencil digits and their stroke data |
| `ui/theme/Theme.kt` | light and dark palettes |
| `res/values*/strings.xml` | localized strings |
| `docs/languages.md` | current languages and next waves |
| `docs/fdroid.md` | F-Droid submission checklist |
| `metadata/me.zakharenka.sudoku.yml` | F-Droid build recipe |
| `fdroid/version.json` | version name and code for F-Droid auto-update |
| `fastlane/metadata/android/` | F-Droid store texts and graphics |
| `gradle.properties` | app version (`app.version`) |
| `app/build.gradle.kts` | `versionCode` calculation, AAB build |
| `LICENSE` | GPL-3.0-or-later license text |
