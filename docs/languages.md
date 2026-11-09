# App languages

This document describes the current localization and the plan for adding new
languages.

## Current state

The app ships nine languages. All of them are connected to the native Android
"App language" screen through
`app/src/main/res/xml/locales_config.xml`.

| Language | Code | Resource folder | Status |
| --- | --- | --- | --- |
| English | `en` | `values/` | default language |
| Russian | `ru` | `values-ru/` | ready |
| Spanish | `es` | `values-es/` | ready |
| Portuguese (Brazil) | `pt-BR` | `values-pt-rBR/` | ready |
| French | `fr` | `values-fr/` | ready |
| German | `de` | `values-de/` | ready |
| Italian | `it` | `values-it/` | ready |
| Ukrainian | `uk` | `values-uk/` | ready |
| Belarusian | `be` | `values-be/` | ready |

The current languages are fully covered: the Neucha font contains the needed
diacritics for Spanish, Portuguese, French, German and Italian, plus Cyrillic
for Ukrainian and Belarusian.

## How to choose the next languages

Priority should be based not only on the number of speakers, but also on the
cost of maintenance:

1. **Audience reach** — how many people speak the language and how large the
   Android market is in those countries.
2. **Font** — whether Neucha covers all required characters. If not, Android
   will fall back to a system font and the sketch style will look uneven.
3. **Script and direction** — Latin and Cyrillic are cheaper; CJK, Arabic,
   Devanagari and RTL need separate testing.
4. **Plural forms** — some languages have more categories than `one`/`other`.
5. **String length** — German and French texts are longer than English; this
   matters for the top bar, action buttons and dialogs.
6. **Translation quality** — game terms ("notes", "hint", "difficulty") are
   best checked by a native speaker, not only by machine translation.

## Second wave

These are the languages that make sense to add next. They are popular, but some
of them require a font decision.

| Language | Code | Audience | Difficulty | Comment |
| --- | --- | --- | --- | --- |
| Indonesian | `id` | ~200M | low | Latin script, almost no diacritics; a good early candidate |
| Turkish | `tr` | ~90M | medium | needs `ğ`, `ş`, `İ`; Neucha does not include them |
| Polish | `pl` | ~40M | medium | needs `ą`, `ć`, `ę`, `ł`, `ń`, `ś`, `ź`, `ż` |
| Vietnamese | `vi` | ~100M | high | many stacked diacritics; needs an extended font |
| Czech | `cs` | ~10M | medium | Latin Extended; check string lengths |
| Slovak | `sk` | ~5M | medium | Latin Extended |
| Hungarian | `hu` | ~13M | medium | Latin Extended, long vowels |
| Romanian | `ro` | ~25M | medium | needs `ș`, `ț` |
| Dutch | `nl` | ~25M | low | Latin script, font fits |
| Swedish | `sv` | ~10M | low | Latin script, font fits |
| Danish | `da` | ~6M | low | Latin script, font fits |
| Norwegian (Bokmål) | `nb` | ~5M | low | Latin script, font fits |
| Finnish | `fi` | ~5M | low | Latin script, font fits |
| Bulgarian | `bg` | ~8M | low | Cyrillic, font fits |
| Serbian | `sr` | ~9M | medium | Cyrillic fits; Latin needs Latin Extended |
| Greek | `el` | ~13M | high | needs a Greek font |

### Recommended second-wave order

1. **Indonesian (`id`)** — maximum reach with minimal work.
2. **Turkish (`tr`)** and **Polish (`pl`)** — large markets, but the Latin
   Extended question must be solved first.
3. **Vietnamese (`vi`)** — a large market, but the hardest one because of the
   diacritics.
4. **Dutch, Swedish, Czech, Hungarian, Romanian** — add them as good
   translations become available.

## Third wave

These languages offer the largest reach but require dedicated work on fonts,
layout and translation. They should not be added "on the side".

| Language | Code | Audience | What is needed |
| --- | --- | --- | --- |
| Simplified Chinese | `zh-CN` | ~1.1B | CJK font, string-length review, translation |
| Traditional Chinese | `zh-TW` | ~30M | separate CJK font and translation |
| Japanese | `ja` | ~125M | CJK font; Sudoku is especially popular in Japan |
| Korean | `ko` | ~80M | Hangul font |
| Arabic | `ar` | ~400M | Arabic font, RTL, plural categories |
| Hindi | `hi` | ~600M | Devanagari font |
| Bengali | `bn` | ~300M | Bengali font |
| Persian | `fa` | ~100M | Arabic script and RTL |
| Urdu | `ur` | ~70M | Arabic script and RTL |
| Thai | `th` | ~70M | Thai font |
| Hebrew | `he` | ~10M | RTL and Hebrew font |

### Recommended third-wave order

1. **Simplified Chinese (`zh-CN`)** — the largest market.
2. **Japanese (`ja`)** — a strong connection to Sudoku and a loyal audience.
3. **Korean (`ko`)** — completes the largest East Asian markets.
4. **Arabic (`ar`)** — large reach, but requires a full RTL review.
5. **Hindi (`hi`)** — a large market; the Devanagari font is required.

## Fonts and scripts

Neucha is a handwritten font with basic Latin, Cyrillic and common diacritics.
Based on the current `app/src/main/res/font/neucha.ttf`:

- fully supported: English, Russian, Spanish, Portuguese, French, German,
  Italian, Ukrainian, Belarusian;
- full Latin Extended is missing: Polish `ą`, `ć`, `ę`, `ł`, `ń`, `ś`, `ź`,
  `ż`, Turkish `ğ`, `ş`, `İ`, and Vietnamese stacked diacritics;
- CJK, Arabic script, Devanagari, Bengali, Thai, Hebrew and full Greek are
  missing.

Options for the next waves:

1. **Keep the system fallback.** Fast, but some letters will look like a
   different font. This may be acceptable for Turkish and Polish, but it is
   already noticeable for Vietnamese.
2. **Find a handwritten font with Latin Extended.** The best option for the
   second wave: it keeps a consistent style for Turkish, Polish, Czech,
   Hungarian and Romanian.
3. **Add separate fonts for other scripts.** CJK, Arabic, Devanagari and Thai
   need their own fonts with a license that allows distribution inside the
   app. SIL OFL or similar free licenses are preferred.

When adding a font, check not only the letters but also the digit shapes: in
Sudoku the digits are the main content, and they must stay legible in both
light and dark themes.

## RTL

`android:supportsRtl="true"` is already set in the manifest, but that is not
enough for Arabic, Persian, Urdu and Hebrew. The following must be checked:

- mirroring of `Row` and horizontal padding;
- the position of buttons and icons in the top bar;
- hand-drawn icons in `SketchIcons.kt`;
- text alignment in dialogs;
- that the Sudoku grid and digits keep their usual order.

Use the `ar-XB` pseudolocale to test RTL.

## Plurals

The app currently has only the `mistakes` plural. When adding languages, check
the CLDR categories:

| Languages | Categories |
| --- | --- |
| English, Spanish, German, Italian, Portuguese | `one`, `other` |
| French | `one`, `other` (special rule for 0 and 1) |
| Russian, Ukrainian, Belarusian, Polish | `one`, `few`, `many`, `other` |
| Arabic | `zero`, `one`, `two`, `few`, `many`, `other` |
| Chinese, Japanese, Korean | `other` |
| Hindi | `one`, `other` |

If a category is missing, Android falls back to `other`; for Russian and
Ukrainian this produces grammatically wrong forms, so `few` and `many` are
mandatory.

## Language addition checklist

1. Create `app/src/main/res/values-<code>/strings.xml`.
2. Create `app/src/main/res/values-<code>/plurals.xml` with the required
   categories.
3. Translate every string, including `contentDescription`, rules, settings,
   the win screen and the pause screen.
4. Keep all positional parameters (`%1$s`, `%2$d`, `%3$s`).
5. Add the locale to `app/src/main/res/xml/locales_config.xml` in BCP 47
   format (`pt-BR`, `zh-CN`, `ar`).
6. Check string lengths in the top bar, action buttons and dialogs.
7. Check the font: no missing glyphs and no broken sketch style.
8. For RTL languages, check interface mirroring.
9. Run the full check:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

10. Check the language on an emulator:

```bash
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales es
```

Reset the app language selection:

```bash
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ""
```

11. Update this document, `README.md` and the fastlane store texts.

## Pseudolocales for layout testing

- `en-XA` — stretches text, helping to find clipped buttons and bad line
  breaks;
- `ar-XB` — enables RTL and helps to check mirroring.

```bash
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales en-XA
adb shell cmd locale set-app-locales me.zakharenka.sudoku --locales ar-XB
```
