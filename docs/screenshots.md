# Screenshots

Store screenshots live in `fastlane/metadata/android/en-US/images/`. Only the
English listing has screenshots; other locales use the shared feature graphic.

## Inventory

| File | Size | Shows |
| --- | --- | --- |
| `phoneScreenshots/01-game.jpg` | 1080 × 1920 | light theme, wobbly grid, Expert game in progress, fast mode armed, notes |
| `phoneScreenshots/02-game-straight.jpg` | 1080 × 1920 | the same game with straight grid lines and fast mode off |
| `phoneScreenshots/03-history.jpg` | 1080 × 2160 | game history with a current game, another unfinished game and solved games |
| `phoneScreenshots/04-records.jpg` | 1080 × 2160 | best results without mistakes |
| `phoneScreenshots/05-new-game.jpg` | 1080 × 2160 | new game dialog |
| `phoneScreenshots/06-rules.jpg` | 1080 × 2160 | How to play, including fast mode |
| `phoneScreenshots/07-dark-game.jpg` | 1080 × 1920 | dark theme, wobbly grid, fast mode off |
| `sevenInchScreenshots/01-game-portrait.jpg` | 1200 × 1920 | real 7-inch profile (600 dp), wobbly grid, fast mode |
| `sevenInchScreenshots/02-game-landscape.jpg` | 1920 × 1200 | real 7-inch profile (960 dp), wobbly grid, fast mode |
| `sevenInchScreenshots/03-game-straight-portrait.jpg` | 1200 × 1920 | real 7-inch profile, straight grid |
| `sevenInchScreenshots/04-history-portrait.jpg` | 1200 × 1920 | real 7-inch profile, history |
| `tenInchScreenshots/01-game-portrait.jpg` | 1600 × 2560 | 10-inch profile (800 dp), wobbly grid, fast mode |
| `tenInchScreenshots/02-game-landscape.jpg` | 2560 × 1600 | 10-inch profile (1280 dp), wobbly grid, fast mode |
| `tenInchScreenshots/03-game-straight-portrait.jpg` | 1600 × 2560 | 10-inch profile, straight grid |
| `tenInchScreenshots/04-history-portrait.jpg` | 1600 × 2560 | 10-inch profile, history |

The feature graphic and the icon are not screenshots and are not regenerated
here.

## Store rules

- Use JPEG or 24-bit PNG without transparency.
- The current set uses JPEG with `-quality 80 -sampling-factor 2x2`. It is
  lossy, but at 1080–1600 px the text and paper texture stay sharp and the
  set is about 2.85 MB instead of 5.2 MB as PNG.
- `-quality 80` is the practical floor for this sketch style. At `75` the
  small history text and thin digits start to soften when zoomed in; below
  `70` the loss becomes visible on a large screen.

Measured on the current 15-image set with `4:2:0`:

| JPEG quality | Total size | Assessment |
| ---: | ---: | --- |
| 90 | 3.85 MB | no visible loss |
| 85 | 3.24 MB | no visible loss at 100 % |
| 80 | 2.85 MB | current setting; still sharp when zoomed |
| 75 | 2.57 MB | small text and thin digits soften at 2–3× zoom |
| 70 | 2.38 MB | visible softening, not recommended |
- Do not use WebP for store screenshots. Google Play accepts only JPEG or
  24-bit PNG, and Fastlane uploads only `.png`, `.jpg` and `.jpeg`; a WebP file
  would be ignored even though it compresses better.
- PNG is accepted too, but it is larger. Use it only when lossless output is
  required.
- Keep every side between 320 and 3840 px and the aspect ratio no wider than
  `2:1` or taller than `1:2`. Google Play rejects anything outside that range.
- Use at most eight phone screenshots. The current set has seven.
- Keep `01-game.jpg` first: it is the screenshot most people see.
- Show the real app in English, with realistic data. Do not add marketing
  captions, device frames or a browser-style background.
- Do not show personal data, debug builds with test banners, or an emulator
  that is still booting.

The phone emulator is 1080 × 2400, which is taller than `2:1`. Game screenshots
are cropped to 1080 × 1920; tall dialogs are cropped to 1080 × 2160. Both are
accepted by Google Play and keep the whole dialog inside the image.

## Requirements

- the debug APK from `./gradlew :app:assembleDebug`;
- `adb` and the phone AVD `Medium_Phone_API_36`;
- the tablet AVD `Medium_Tablet` (2560 × 1600, 320 dpi);
- Python 3 for `scripts/screenshot-data.py`;
- ImageMagick (`magick`) for cropping and resizing.

## Prepare the device

The screenshots use English, the light theme, the wobbly grid and font scale
`1.0`. Reset anything left over from a previous run:

```bash
adb shell wm size reset
adb shell wm density reset
adb shell settings put system font_scale 1.0
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales en-US
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The tablet starts in landscape. For portrait screenshots set
`adb shell settings put system user_rotation 1`; for landscape set it back to
`0` and wait for the activity to recreate.

## Generate the data

`scripts/screenshot-data.py` creates a deterministic current game, solved games
and unfinished games, plus the matching settings file. Timestamps are relative
to the moment it runs, so history looks recent:

```bash
python3 scripts/screenshot-data.py --out /tmp/sudoku-screenshots/light
python3 scripts/screenshot-data.py --out /tmp/sudoku-screenshots/straight \
  --straight-grid --no-fast-input
python3 scripts/screenshot-data.py --out /tmp/sudoku-screenshots/dark \
  --dark --no-fast-input
```

The fixture contains:

- an Expert game in progress with notes and a few empty cells;
- another unfinished game marked with a clock in history;
- solved Easy, Medium, Hard and Expert games for the records screen;
- settings for the requested grid, theme and fast-mode state.

Do not commit the generated JSON or XML; they are temporary files.

## Install the fixture

`run-as` works only for the debug build. Stop the app before replacing its
private files:

```bash
adb shell am force-stop me.zakharenka.sudoku
adb push /tmp/sudoku-screenshots/light/sudoku_data.json /data/local/tmp/sudoku_data.json
adb push /tmp/sudoku-screenshots/light/sudoku.xml /data/local/tmp/sudoku.xml
adb shell "run-as me.zakharenka.sudoku sh -c '
  mkdir -p files shared_prefs;
  cp /data/local/tmp/sudoku_data.json files/sudoku_data.json;
  cp /data/local/tmp/sudoku.xml shared_prefs/sudoku.xml;
  rm -f files/sudoku_data.json.bak
'"
adb shell am start -n me.zakharenka.sudoku/.MainActivity
```

Repeat with the `straight` or `dark` directory for those screenshots. The game
opens paused after launch; tap the play button in the top bar to show the board.

## Capture

Save every raw screenshot first, then crop and convert. Raw captures from the
phone are 1080 × 2400:

```bash
adb exec-out screencap -p > /tmp/sudoku-screenshots/raw/01-game.png
```

Use `uiautomator dump` when you need the tap target; every icon button has a
`content-desc` from the string resources:

```bash
adb shell uiautomator dump /sdcard/window.xml
adb shell cat /sdcard/window.xml | sed 's/></>\n</g' | grep 'content-desc="History"'
```

### Phone

1. `01-game.jpg`: install the `light` fixture, open the app, tap **Resume**,
   choose a digit that still has a free place (the fixture leaves digit 4 with
   two places), then capture. The armed digit, the selected cell and the notes
   all belong to the fast-mode story.
2. `02-game-straight.jpg`: install the `straight` fixture, launch, resume and
   capture without arming a digit.
3. `03-history.jpg`: install the `light` fixture, launch and open **History**.
   The list must show the current game, another unfinished game and solved
   games.
4. `04-records.jpg`: from history tap the trophy button. Keep the default
   **No mistakes** tab; Easy and Hard have best times in the fixture.
5. `05-new-game.jpg`: close the dialogs and tap the new-game button in the top
   bar.
6. `06-rules.jpg`: close the dialog and tap **Rules**.
7. `07-dark-game.jpg`: install the `dark` fixture, launch, resume and capture.

Crop the raw files. Game views fit in 16:9; tall dialogs need the extra height:

```bash
magick /tmp/sudoku-screenshots/raw/01-game.png \
  -crop 1080x1920+0+0 +repage \
  -alpha off -strip -quality 80 -sampling-factor 2x2 \
  fastlane/metadata/android/en-US/images/phoneScreenshots/01-game.jpg

magick /tmp/sudoku-screenshots/raw/03-history.png \
  -crop 1080x2160+0+0 +repage \
  -alpha off -strip -quality 80 -sampling-factor 2x2 \
  fastlane/metadata/android/en-US/images/phoneScreenshots/03-history.jpg
```

`screencap` produces RGBA PNGs. JPEG has no alpha channel, so use `-alpha off`
when saving. Keep the raw captures as PNG; only the store files are JPEG.

### Tablet

The 7-inch and 10-inch sets are captured on different dp widths, not by
downscaling one from the other. The 7-inch layout is 600 dp wide in portrait
and 960 dp in landscape, so the action row uses a 3+2 grid and the board is
smaller. The 10-inch layout is 800 dp / 1280 dp, where the action row fits in
one row of five.

Use the tablet AVD `Medium_Tablet` for both profiles. Start from its native
display and change only the logical size and density:

```bash
# 7-inch: 1200 x 1920 at 320 dpi = 600 x 960 dp
adb shell wm size 1200x1920
adb shell wm density 320
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0   # portrait
# landscape: user_rotation 1

# 10-inch: native 2560 x 1600 at 320 dpi = 800 x 1280 dp
adb shell wm size reset
adb shell wm density reset
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # portrait
# landscape: user_rotation 0
```

For each profile install the `light` fixture and capture:

- `01-game-portrait.jpg` and `02-game-landscape.jpg`: wobbly grid, resume the
  game; arm a digit in portrait if you want the same fast-mode story.
- `03-game-straight-portrait.jpg`: install the `straight` fixture, launch,
  resume and capture in portrait.
- `04-history-portrait.jpg`: install the `light` fixture, open **History** and
  capture in portrait.

Convert the raw captures directly, without resizing between the two profiles:

```bash
magick /tmp/sudoku-screenshots/raw/tablet-portrait.png \
  -alpha off -strip -quality 80 -sampling-factor 2x2 \
  fastlane/metadata/android/en-US/images/sevenInchScreenshots/01-game-portrait.jpg

magick /tmp/sudoku-screenshots/raw/tablet-landscape.png \
  -alpha off -strip -quality 80 -sampling-factor 2x2 \
  fastlane/metadata/android/en-US/images/sevenInchScreenshots/02-game-landscape.jpg

# Repeat with tenInchScreenshots/ and the 10-inch captures.
```

## Check the result

```bash
identify \
  fastlane/metadata/android/en-US/images/phoneScreenshots/*.jpg \
  fastlane/metadata/android/en-US/images/sevenInchScreenshots/*.jpg \
  fastlane/metadata/android/en-US/images/tenInchScreenshots/*.jpg

identify -format '%f %wx%h %[channels]\n' \
  fastlane/metadata/android/en-US/images/phoneScreenshots/*.jpg
```

Every file must be `srgb`/`rgb` (no alpha), within the size limits and the
`2:1` ratio. Then open each image and check:

- the status bar and the app are sharp, with no dialog clipped at the bottom;
- the board has realistic digits and notes, not an empty test grid;
- `01` shows fast mode, `02` and `03` (tablet) show straight lines;
- the 7-inch and 10-inch sets have different layouts: 3+2 action buttons and
  a smaller board versus five buttons in one row;
- history contains both a current/unfinished game and solved games;
- records show a best time, not only dashes;
- the How to play line about fast mode is complete;
- no debug UI, personal data or placeholder text is visible.

Finally, check the diff:

```bash
git status --short
git diff --stat
```

Only the screenshot files and the documentation for this process should change.

## When the UI changes

- A new feature usually means replacing `01-game.jpg` and adding a dialog shot.
  Keep the order and the first slot stable.
- If a dialog grows, recapture it and crop to 1080 × 2160. Never go beyond
  `2:1`; if the content still does not fit, change the data or split the shot,
  do not stretch the image.
- If the action row or the top bar changes height, recapture all phone and
  tablet game views so the set stays consistent.
- If the fast-mode wording changes, update `06-rules.jpg` and `01-game.jpg`.
- If the app gains a new supported language with store screenshots, add a
  matching `values-<code>` image folder; the English set stays the reference.
- Run the build and tests after documentation-only changes only if the code
  changed; screenshots themselves do not need a Gradle task.
