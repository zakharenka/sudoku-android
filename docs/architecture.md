# Architecture

Sudoku is a single-module Android app built with Kotlin and Jetpack Compose. The
code is split so that game rules stay independent of Android and can be tested
with plain JUnit.

## Layers

```text
MainActivity
  └── SudokuViewModel (GameUiState)
        ├── SudokuSession      — game rules, history, victory
        ├── SudokuGenerator    — puzzle generation with a single solution
        ├── SettingsStore      — global settings only
        ├── SudokuDataStore    — atomic file storage for the current game and history
        └── SudokuDataCodec    — shared JSON parser/validator for load and import

SudokuScreen
  ├── SudokuBoard            — 9×9 board rendering
  ├── SudokuControls         — top bar, digits, actions, overlays
  └── sketch/                — hand-drawn surfaces, buttons, icons
```

## Boundaries

- `game/` — pure Kotlin with no Android imports. All rules, generation,
  history models and formatting live here. This code must remain testable with
  plain JUnit tests.
- `data/` — SharedPreferences settings and the atomic JSON file. It contains no
  game rules; JSON parsing and validation are shared by normal loading and
  import.
- `SudokuViewModel` — the only place where rules, the timer, background
  generation and persistence meet.
- `ui/` — only renders `GameUiState` and sends actions to the ViewModel. The UI
  must not compute move legality, victory or mistakes.

## Data flow

A user action goes to `SudokuViewModel`. The ViewModel asks `SudokuSession` to
apply the move, then publishes a new `GameUiState`. The UI renders that state
and never keeps a second copy of the rules.

`SudokuViewModel` also owns the timer, background generation and persistence.
`SudokuDataStore` writes the current game and history through `AtomicFile` on
`Dispatchers.IO` under a `Mutex`; `SudokuDataCodec` is the single parser and
validator used by both normal loading and import.

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
| `ui/SudokuBoard.kt` | 9×9 board rendering |
| `ui/SudokuControls.kt` | top bar, keypad, actions, overlays |
| `ui/sketch/Sketch.kt` | hand-drawn surfaces, buttons, lines, texture |
| `ui/sketch/SketchIcons.kt` | hand-drawn icons |
| `ui/sketch/HandwrittenDigits.kt` | animated pencil digits and stroke data |
| `ui/theme/Theme.kt` | light and dark palettes |
| `ui/theme/Type.kt` | handwritten font |
| `res/values*/strings.xml` | localized strings |
| `res/values*/plurals.xml` | localized plural forms |
| `res/xml/locales_config.xml` | languages for the native Android language picker |
| `gradle.properties` | app version (`app.version`) |
| `app/build.gradle.kts` | `versionCode` calculation and AAB build |
