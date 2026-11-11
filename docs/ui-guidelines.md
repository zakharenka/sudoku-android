# UI guidelines

## Header and top bar

- The header is one row: a hand-drawn icon and the app name.
- There is no "Logic without rush" slogan.
- The in-game top bar is one row:
  - on the left, one block: difficulty, mistakes (only when errors are shown)
    and timer;
  - on the right, two square unlabelled buttons: pause and new game.
- Icon buttons must have a `contentDescription` from string resources.
- "Rules", "History" and "Settings" live in the header and are not duplicated
  in settings. "Records" opens from the History dialog header, next to Close.
- Dialog headers accept extra action buttons before the Close button; History
  uses this for Records.

## New game dialog

- The new-game button (the rightmost icon in the top bar) opens a dialog.
- Tapping the current difficulty in the top bar also opens this dialog.
- In the dialog, tapping a difficulty only **selects** it.
- The game starts only with the **Start** button.
- "Cancel" closes the dialog without changes.
- The dialog must explain that the current game will be saved to history.
- The dialog has a **Show mistakes** checkbox. Its value becomes the default
  for the next new game (`show_errors` in SharedPreferences) and is stored in
  the game data for the whole game.
- Do not bring back starting a game immediately when a difficulty row is tapped.

## Settings

- Settings contain only the theme, board line style, same-digit highlight,
  language, data import/export, "About" and a link to the developer website
  `https://zakharenka.me`.
- The 9×9 board uses straight grid lines by default. The settings toggle can
  switch them back to hand-drawn wobbly lines; this affects only the board, not
  buttons, panels or icons.
- Same-digit highlighting can be disabled. This removes the highlight and the
  special text colour from cells with the selected digit; peer highlighting
  remains.
- The language is changed only through the native Android screen
  (`android.settings.APP_LOCALE_SETTINGS`), with no in-app language picker.
- Import and export use the system file picker (SAF).

## Font scaling

- Support system font scale up to `2.0`.
- UI text uses `sp` and must wrap instead of being clipped. Do not give
  text-bearing rows a fixed height; use `heightIn(min = ...)` and vertical
  padding.
- Single-line titles use auto-sizing to keep the full word or dialog title
  visible.
- Game glyphs — board digits and number-pad digits — are vector drawings sized
  from their cell or key, not from the system font scale.
- Counts inside number-pad keys are sized from the key width with a minimum
  readable size, so they never become larger than the digit.
- At font scale `1.3` and above the game top bar switches to two lines. At
  `1.5` and above the action row switches to a 2×2 grid so labels stay whole.
- Never disable system font scaling globally.

## Keyboard

- Digits `1–9` on the main row and numpad enter the selected digit.
- `0`, Backspace and Delete erase the selected cell.
- Arrow keys move the selection one cell and stop at the board edges.
- Keyboard input is ignored while a dialog is open, during loading, pause or
  victory.
- Focus must return to the board after a dialog closes and after resume.

## Tablets

- At widths of `900.dp` and above, use the two-column layout: board on the
  left, keypad and actions on the right.
- Below `900.dp` — a single centred column (phones and portrait tablets).
- The panel is capped at `1120.dp` on wide screens and `720.dp` in compact
  mode.
- In wide mode the board size is limited by width, height and a maximum of
  `760.dp`: `min(760.dp, availableWidth * 0.56, availableHeight - 160.dp)`,
  so the bottom row is not clipped on 7-inch tablets in landscape.
- Test changes on `Medium_Tablet` in portrait and landscape.

## Theme and style

- The interface style is a hand-drawn sketch: paper texture, wobbly lines, a
  square grid with no rounded corners and the Neucha font.
- Use `SudokuTheme.colors` and `HandFont`, not default Material colours.
- For panels, buttons, lines and overlays, use the helpers in
  `ui/sketch/Sketch.kt`; for icons, use `ui/sketch/SketchIcons.kt`.
- Do not add rounded corners, gradients or "glossy" elements that break the
  sketch style.
- Draw new icons in the same hand-drawn style; do not use Material Icons.
- All large digits (board cells and number-pad keys) are drawn by
  `ui/sketch/HandwrittenDigits.kt` and animate on input and erase. There is no
  plain-text digit mode and no handwritten-digits setting.
- The number-pad key shows the large handwritten digit with its remaining count
  centered underneath; zero is shown and the key stays disabled.

## App icon

- The icon is adaptive: light, dark and monochrome layers.
- The foreground content is scaled to 80% around the centre
  (`scaleX`/`scaleY` = 0.8, `pivotX`/`pivotY` = 54) so masks do not clip the
  artwork. Do not remove this padding without a visual check.
- The monochrome layer must contain a digit.

## Localization

All user-facing text must come from resource files. See
[`languages.md`](languages.md) for the supported languages and the checklist for
adding a new one.
