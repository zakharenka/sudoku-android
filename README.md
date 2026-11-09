# Sudoku for Android

A classic 9×9 Sudoku for Android built with Kotlin and Jetpack Compose. The
interface is drawn by hand in a sketch style: paper texture, wobbly lines, a
handwritten font and a square grid with no rounded corners. Nine languages are
supported: English, Russian, Spanish, Portuguese (Brazil), French, German,
Italian, Ukrainian and Belarusian. The default language comes from the system
and is changed through the native Android app-language settings.

The app is free software under the GNU General Public License v3.0 or later.
The source code is hosted on GitHub, and the project ships the metadata needed
for distribution on F-Droid.

## Features

- four difficulty levels: Easy, Medium, Hard and Expert;
- puzzle generation with a single solution;
- cell digits are drawn as hand-written pencil strokes with a writing and
  erasing animation; the setting can switch them back to plain digits of the
  same size;
- hardware keyboard support: digits and numpad enter values, `0`, Backspace and
  Delete erase, arrow keys move the selected cell;
- cell notes, hints (three per game), undo and clear cell;
- timer, a mistake counter (can be disabled per game), and pause with
  auto-pause when leaving the app;
- single-row top bar: difficulty, mistakes (when enabled) and timer in one
  block, with unlabelled pause and new-game buttons on the right;
- adaptive layout: on tablets in landscape the board is on the left and the
  controls on the right; in portrait it is a comfortable single column;
- new game through a dialog: confirmation, difficulty selection, a "show
  mistakes" option, and Start and Cancel buttons; the dialog opens from the
  new-game button or by tapping the current difficulty;
- records per difficulty and result: best time and number of solved games
  with and without mistakes, derived from the game history;
- game history grouped by day, with a mini puzzle preview, start and finish
  dates, elapsed time and mistakes; tapping a row loads that game on pause;
- JSON backup import/export through the system file picker;
- nine languages: English, Russian, Spanish, Portuguese (Brazil), French,
  German, Italian, Ukrainian and Belarusian; the default language comes from
  the system and is changed through the native Android "App language" screen;
- light and dark themes, plus a "follow system" mode;
- settings: theme selection, board line style, same-digit highlighting,
  handwritten digits, a shortcut to the system language picker, backup
  import/export and a link to the developer website;
- straight grid lines on the 9×9 board by default, with an option to switch
  back to hand-drawn wobbly lines;
- the current game is saved to a local JSON file when the app goes to the
  background: the timer stops and the game is restored on pause the next time
  it starts;
- win screen with the time and a new-record message;
- hand-drawn app icon: light and dark variants, plus a monochrome icon with a
  digit for Android themed icons.

## Stack

- Kotlin 2.3.21, Jetpack Compose (Material 3), Compose BOM 2026.09.00;
- AGP 9.4.1, Gradle 9.6, JDK 17+;
- minSdk 30, targetSdk 37, compileSdk 37;
- SharedPreferences for settings and one atomic JSON file for the current game
  and history;
- `android:localeConfig` and native Android 13+ per-app language settings.

The app does not use the network, does not include analytics and requests no
permissions.

## Version

The version is defined in one place — `gradle.properties`:

```properties
app.version=1.0.4
```

`app/build.gradle.kts` uses this value as `versionName` and derives
`versionCode` with this scheme:

```text
major * 1_000_000 + minor * 1_000 + patch
```

For `1.0.4` this is `versionCode = 1000004`. Versions follow semantic
versioning `MAJOR.MINOR.PATCH`; release versions are marked with git tags.

## Building

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Installing on a connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release build (R8, unsigned):

```bash
./gradlew :app:assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Publishing

### Google Play

The Google Play package is assembled locally in `store_out/` and is not
committed (the folder is listed in `.gitignore`). It contains the listing
texts, icon, feature graphic, screenshots, Play Console answers and the AAB.

```bash
./gradlew :app:bundleRelease
```

The AAB is written to `app/build/outputs/bundle/release/app-release.aab`. It
must be signed with the upload key before it is uploaded.

Play Console documents:

- privacy policy:
  `https://zakharenka.me/privacy/android_sudoku_privacy.html`;
- terms of use:
  `https://zakharenka.me/privacy/android_sudoku_terms.html`.

### F-Droid

The F-Droid build recipe is in
[`metadata/me.zakharenka.sudoku.yml`](metadata/me.zakharenka.sudoku.yml).
F-Droid builds the app from the tagged source on GitHub, so no signed APK needs
to be uploaded.

Store texts and graphics for F-Droid live in the standard fastlane layout:

```text
fastlane/metadata/android/<locale>/
├── title.txt
├── short_description.txt
├── full_description.txt
├── changelogs/<versionCode>.txt
└── images/
```

To submit or update the app in the official F-Droid repository, open a merge
request against
[`fdroid/fdroiddata`](https://gitlab.com/fdroid/fdroiddata) with the metadata
file. See [`docs/fdroid.md`](docs/fdroid.md) for the full checklist.

## Tests and checks

```bash
./gradlew :app:testDebugUnitTest
```

Covered: the puzzle generator, solution counting, game actions (input, notes,
clear, undo, hint limit, victory), hidden and visible error modes, game
record restore and record rules.

Full check before committing:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Structure

```text
app/src/main/java/me/zakharenka/sudoku/
├── MainActivity.kt          # entry point, edge-to-edge, save on onStop
├── SudokuViewModel.kt       # game state, timer, generation, persistence, import/export
├── data/
│   ├── SettingsStore.kt     # global settings in SharedPreferences
│   ├── SudokuDataStore.kt   # atomic JSON file for the current game and history
│   ├── SudokuDataCodec.kt   # shared JSON parser/validator
│   └── ThemeMode.kt         # system / light / dark theme
├── game/
│   ├── Difficulty.kt        # difficulty levels and clue count
│   ├── GameRecord.kt        # one saved game and its validation
│   ├── SudokuData.kt        # history and records derived from solved games
│   ├── SudokuGenerator.kt   # generator and solver
│   ├── SudokuSession.kt     # game rules with no Android dependencies
│   └── Formatters.kt        # time and date formatting
└── ui/
    ├── SudokuScreen.kt      # screen, header, settings and dialogs
    ├── SudokuBoard.kt       # 9×9 board with a hand-drawn grid
    ├── SudokuControls.kt    # top bar, digits, actions, overlays
    ├── sketch/
    │   ├── Sketch.kt        # hand-drawn lines, panels, buttons, texture
    │   ├── SketchIcons.kt   # hand-drawn icons
    │   └── HandwrittenDigits.kt  # animated pencil digits
    └── theme/
        ├── Theme.kt         # light and dark palettes
        └── Type.kt          # handwritten font

app/src/test/java/me/zakharenka/sudoku/game/   # JUnit tests for game logic

app/src/main/res/
├── values/                  # English (default)
├── values-ru/               # Russian
├── values-es/               # Spanish
├── values-pt-rBR/           # Portuguese (Brazil)
├── values-fr/               # French
├── values-de/               # German
├── values-it/               # Italian
├── values-uk/               # Ukrainian
├── values-be/               # Belarusian
├── xml/locales_config.xml   # languages for the native Android language picker
├── drawable/                # hand-drawn app icons
└── font/neucha.ttf          # Neucha handwritten font

fastlane/metadata/android/   # F-Droid store texts and graphics
metadata/                    # F-Droid build recipe
docs/
├── fdroid.md                # F-Droid submission checklist
├── languages.md             # current languages and next translation waves
└── releases/
    ├── README.md            # release notes index
    ├── 1.0.0.md
    ├── 1.0.1.md
    ├── 1.0.2.md
    ├── 1.0.3.md
    └── 1.0.4.md
```

Each `values-*` folder contains `strings.xml` and `plurals.xml`.

The game logic in `game/` has no Android dependencies and is covered by plain
JUnit tests. The UI does not hold game rules: it only renders `GameUiState` and
sends actions to `SudokuViewModel`.

## How a game works

- `SudokuGenerator` creates a complete solution and removes digits down to the
  target clue count while checking that the solution stays unique.
- `SudokuSession` stores cells, notes, mistakes and move history, and decides
  when the game is won. All rules live here.
- `SudokuViewModel` drives background generation, a timer based on
  `SystemClock.elapsedRealtime()`, pause, saving, restoring, history and data
  import/export.
- When the app goes to the background (`onStop`), the current game is saved and
  the timer stops. On the next launch the game referenced by `currentGameId` is
  restored on pause; a solved current game opens on its win screen.
- The current game is checkpointed after every board change, every 10 seconds
  while the timer runs, on pause or dialog open, when history is opened or a
  game is switched, on victory, before export and in `onStop`.
- Each game has three hints. The remaining count is shown on the hint button,
  persisted with the game and is not refunded by undo or erase.
- On victory the game joins **No mistakes** when `errors == 0` and
  **With mistakes** otherwise. It can set the best time in that category and
  increments its solved count. Hidden-errors games keep `errors == 0`, so they
  join **No mistakes**. The solved game stays in the history and remains the
  current game until another game is opened.

## Data storage

Settings are stored in `SharedPreferences` (`sudoku`):

| Key | Value |
| --- | --- |
| `theme_mode` | `system`, `light` or `dark` |
| `notes_mode` | whether notes mode is enabled |
| `straight_grid` | whether the 9×9 board uses straight grid lines (default `true`) |
| `same_highlight` | whether cells with the selected digit are highlighted (default `true`) |
| `handwritten_digits` | whether cell values use animated handwritten strokes (default `true`) |
| `show_errors` | default error mode for a new game (default `true`) |

The current game and the full game history live in one versioned JSON file,
`filesDir/sudoku_data.json`, written atomically. It contains `version`,
`currentGameId` and `games` (current, unfinished and solved). `currentGameId`
always references the game shown on the screen, including a solved one. Records
are not stored separately: best times and solved counters are derived from
solved games, split into "without mistakes" (`errors == 0`) and "with mistakes"
(`errors > 0`). Notes are stored as a sparse object and per-cell error flags are
derived from the saved values and solution. Old `best:*`, `solved:*` and
`saved_game` keys are ignored and not migrated.

## Localization

- English is the default language. The app currently supports `en`, `ru`, `es`,
  `pt-BR`, `fr`, `de`, `it`, `uk` and `be`.
- All user-facing strings live in `values*/strings.xml`; plural forms live in
  `plurals.xml`.
- `locales_config.xml` connects the languages to the native "App language"
  screen.
- The app settings have no language picker of their own: the "Change language"
  button opens the system `android.settings.APP_LOCALE_SETTINGS` screen.
- The plan for adding more languages and the font requirements are described
  in [`docs/languages.md`](docs/languages.md).

## Font and icon

The interface uses the **Neucha** font (SIL Open Font License 1.1). The font
file is `app/src/main/res/font/neucha.ttf` and the license is in
`app/src/main/assets/licenses/Neucha-OFL.txt`.

The app icon is an adaptive icon with hand-drawn light and dark variants and a
monochrome layer showing the digit 9. The content is scaled to 80% around the
centre to keep margins under masks of different shapes.

## License

Copyright (C) 2026 Aliaksandr Zakharenka.

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

The full text is in [`LICENSE`](LICENSE).

The Neucha font is licensed separately under the SIL Open Font License 1.1; see
`app/src/main/assets/licenses/Neucha-OFL.txt`.
