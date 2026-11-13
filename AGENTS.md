# AGENTS.md

Instructions for AI agents and developers working with this repository.

## Read first

| File | Contents |
| --- | --- |
| [`README.md`](README.md) | product overview, features, stack, build and test commands |
| [`docs/architecture.md`](docs/architecture.md) | module boundaries, data flow and key files |
| [`docs/data-storage.md`](docs/data-storage.md) | settings, saved game format, persistence and backup |
| [`docs/game-rules.md`](docs/game-rules.md) | rules, hints, records and history |
| [`docs/ui-guidelines.md`](docs/ui-guidelines.md) | dialogs, top bar, keyboard, tablets, sketch style and icon |
| [`docs/release-process.md`](docs/release-process.md) | versioning, release checklist, Google Play and F-Droid |
| [`docs/screenshots.md`](docs/screenshots.md) | store screenshot sizes, capture steps and fixture data |
| [`docs/languages.md`](docs/languages.md) | supported languages and translation plan |

The docs above are the source of truth. Update them together with the code when
behavior changes; do not duplicate their content here.

## What this project is

A native Android Sudoku app built with Kotlin and Jetpack Compose. It is a
standalone project, not a wrapper around a browser game: no WebView, JavaScript
or web game engine is used here.

## Non-negotiable rules

### Architecture

- `game/` is pure Kotlin with no Android imports. All rules, generation,
  history models and formatting live here and must stay testable with plain
  JUnit.
- `data/` contains SharedPreferences settings and the atomic saved-game file.
  It contains no game rules; parsing and validation are shared by normal
  loading and import.
- `SudokuViewModel` is the only place where rules, the timer, background
  generation and persistence meet.
- `ui/` only renders `GameUiState` and sends actions to the ViewModel. The UI
  must not compute move legality, victory or mistakes.

### Product behavior

- The new-game dialog is specified in
  [`docs/ui-guidelines.md`](docs/ui-guidelines.md); a difficulty row only
  selects a level, and only the Start button begins a new game.
- Records, history, hints and game lifecycle are specified in
  [`docs/game-rules.md`](docs/game-rules.md).
- Saved data, validation, import and export are specified in
  [`docs/data-storage.md`](docs/data-storage.md).

### Versioning and release

- The single source of the version is `app.version` in `gradle.properties`.
- Do not hardcode the version in interface strings: use
  `BuildConfig.VERSION_NAME`.
- Use `python3 scripts/set-version.py X.Y.Z` to update the version and keep
  `fdroid/version.json` in sync.
- Release steps and the store-package script are in
  [`docs/release-process.md`](docs/release-process.md).
- Do not edit `metadata/me.zakharenka.sudoku.yml` for a normal release; F-Droid
  updates the build entry from the tag and `fdroid/version.json`.

### UI and style

- Keep the hand-drawn sketch style: paper texture, wobbly lines, square grid,
  no rounded corners and the Neucha font.
- Use `SudokuTheme.colors` and `HandFont`, not default Material colours.
- Use the helpers in `ui/sketch/Sketch.kt` and `ui/sketch/SketchIcons.kt`.
  Do not use Material Icons.
- All user-facing strings, including `contentDescription`, come from resource
  files. Do not hardcode text in Compose code.

### Dependencies and identity

- Do not change `applicationId` or `namespace` (`me.zakharenka.sudoku`).
- Do not add dependencies without a concrete need. The project uses Compose,
  lifecycle, core-ktx and test libraries.
- Do not commit `local.properties`, `build/`, `.gradle/`, `.cxx/`,
  `store_out/` or other build artifacts.

## Commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Android Lint
./gradlew :app:lintDebug

# Release build with R8 (unsigned)
./gradlew :app:assembleRelease

# Full check before completing a task
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Installing on a device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n me.zakharenka.sudoku/.MainActivity
```

Requirements: JDK 17+, Android SDK 37, Gradle Wrapper 9.6. `local.properties`
contains the local SDK path and is not committed.

## Tests

- Unit tests live in `app/src/test/java/me/zakharenka/sudoku/`; game logic tests
  are under `game/`.
- `FdroidVersionTest` keeps `fdroid/version.json` in sync with `BuildConfig`.
- When changing rules, the generator, persistence or formatting, add or update
  tests.
- There are no UI tests yet; do not add instrumentation tests unless necessary.
- Lint warnings about available Gradle, AGP or dependency updates are expected
  and are not errors.

## Git

- Commit messages follow Conventional Commits, in English, in the imperative
  mood: `feat: ...`, `fix: ...`.
- List the changes in the commit body.
- Commit only when the user asks. Do not amend, rebase or force-push without an
  explicit request.
- Before committing, check `git diff --check` and make sure no stray files are
  included.

## Emulator and visual checks

- For screenshots: `adb exec-out screencap -p > /tmp/opencode/screen.png`.
- Switching the app language:

```bash
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ru
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ""
```

- In this environment the emulator sometimes crashes with `SIGSEGV` or
  `SIGKILL`. This is an environment problem, not an app problem. The source of
  truth is a successful build, tests and Lint; do not change app code to work
  around an emulator crash.
- If the emulator is unavailable, the visual check can be postponed, but the
  build and tests must still be run.
