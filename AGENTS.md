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
        ├── RecordsStore       — records, counters, theme, notes
        └── SavedGameStore     — save and restore the current game

SudokuScreen
  ├── SudokuBoard            — 9×9 board rendering
  ├── SudokuControls         — top bar, digits, actions, overlays
  └── sketch/                — hand-drawn surfaces, buttons, icons
```

Separation rules:

- `game/` — pure Kotlin with no Android imports. All rules, generation and
  formatting live here. This code must remain testable with plain JUnit tests.
- `data/` — SharedPreferences storage. It contains no game rules.
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
- The dialog must warn that the current progress will be lost.
- Do not bring back starting a game immediately when a difficulty row is
  tapped.

### Top bar and header

- The header is one row: a hand-drawn icon and the app name.
- There must be no "Logic without rush" slogan.
- The in-game top bar is one row:
  - on the left, one block: difficulty, mistakes, timer;
  - on the right, two square unlabelled buttons: pause and new game.
- Icon buttons must have a `contentDescription` from string resources.

### Settings

- Settings contain only the theme, board line style, same-digit highlight,
  handwritten digits, language, records reset, "About" and a link to the
  developer website `https://zakharenka.me`.
- The 9×9 board uses straight grid lines by default. The settings toggle can
  switch them back to hand-drawn wobbly lines; this affects only the board, not
  buttons, panels or icons.
- Same-digit highlighting can be disabled. This removes the highlight and the
  special text colour from cells with the selected digit; peer highlighting
  remains.
- "Rules" and "Records" live in the header and are not duplicated in settings.
- The language is changed only through the native Android screen
  (`android.settings.APP_LOCALE_SETTINGS`), with no in-app language picker.

### Records

- The best time is stored separately for each difficulty.
- The number of solved puzzles is stored separately for each difficulty
  (`solved:easy`, `solved:medium`, `solved:hard`, `solved:expert`).
- "Best results" shows `Solved: N` under each difficulty.
- The counter increases exactly once on victory.
- Resetting records clears both the best times and the counters for all
  difficulties.

### Hints

- Each game has at most `SudokuSession.MAX_HINTS` (3) hints.
- The remaining count is shown as a badge on the hint button; at zero the
  button is disabled.
- `hintsUsed` is persisted in `SavedGame` and restored with the game.
- Undo does not refund a hint.
- A new game resets the counter.

### Game persistence

- When the app goes to the background (`MainActivity.onStop`, except for a
  configuration change), `onAppBackgrounded()` is called: the game is paused
  and saved.
- On the next launch the game is restored **on pause**; the timer is not
  running.
- A won or missing game is not saved.
- The `SavedGameStore` format is versioned. When the format changes, increase
  `VERSION` and ensure incompatible data is rejected correctly.
- Validate array sizes and values on load; corrupted data must lead to a new
  game, not a crash.

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

`SharedPreferences`:

| File | Keys |
| --- | --- |
| `sudoku` | `best:<difficulty>`, `solved:<difficulty>`, `theme_mode`, `notes_mode`, `straight_grid` |
| `sudoku_saved_game` | `saved_game` (JSON, version 2) |

`<difficulty>`: `easy`, `medium`, `hard`, `expert`.
Do not rename keys without a migration: it would break existing records.

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
| `SudokuViewModel.kt` | state, timer, generation, persistence |
| `game/SudokuSession.kt` | rules, history, victory, game snapshot |
| `game/SudokuGenerator.kt` | generation and solution counting |
| `data/RecordsStore.kt` | records, per-difficulty counters, theme, notes |
| `data/SavedGameStore.kt` | JSON persistence of the current game |
| `ui/SudokuScreen.kt` | header, screen, settings, rules, records, new-game dialog |
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
