# Publishing on F-Droid

This checklist describes how Sudoku is prepared for the official F-Droid
repository. F-Droid builds the APK itself from the tagged source; no signed APK
is uploaded.

## Prerequisites

- The source repository is public:
  <https://github.com/zakharenka/sudoku-android>.
- The source is licensed under GPL-3.0-or-later; see [`LICENSE`](../LICENSE).
- Every release has an annotated git tag, for example `1.0.1`.
- The app contains no ads, tracking, analytics or proprietary dependencies.
- The published terms of use must not contradict the GPL. In particular, they
  must not forbid copying, modification or redistribution of the source code.

## Files in this repository

| Path | Purpose |
| --- | --- |
| `metadata/me.zakharenka.sudoku.yml` | F-Droid build recipe, ready to copy into `fdroiddata` |
| `fastlane/metadata/android/<locale>/` | Store title, descriptions, changelogs and graphics |
| `docs/languages.md` | Translation status and language plan |

The fastlane layout is read by F-Droid directly from the app repository:

```text
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt
├── full_description.txt
├── changelogs/1000001.txt
└── images/
    ├── icon.png
    ├── featureGraphic.png
    ├── phoneScreenshots/
    ├── sevenInchScreenshots/
    └── tenInchScreenshots/
```

Localized folders use BCP 47 tags: `en-US`, `ru-RU`, `es-ES`, `pt-BR`,
`fr-FR`, `de-DE`, `it-IT`, `uk`, `be`. If a resource is missing in a locale,
F-Droid falls back to `en-US`.

Description limits:

- title: 50 characters;
- short description: 80 characters;
- full description: 4000 characters.

## Submitting the app

1. Fork <https://gitlab.com/fdroid/fdroiddata>.
2. Copy `metadata/me.zakharenka.sudoku.yml` into the fork's `metadata/`
   directory.
3. Check the recipe and the source tree:

   ```bash
   fdroid lint me.zakharenka.sudoku
   fdroid build -v -l me.zakharenka.sudoku
   ```

4. Commit the metadata file and open a merge request against `fdroiddata`.
5. Answer the reviewer questions and fix the recipe if the build fails.

Useful references:

- <https://f-droid.org/en/docs/Inclusion_Policy/>
- <https://f-droid.org/en/docs/Build_Metadata_Reference/>
- <https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/>

## Releasing a new version

1. Update `app.version` in `gradle.properties` and keep the semantic version
   format.
2. Update the corresponding fastlane texts and add
   `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Commit the release and create an annotated git tag:

   ```bash
   git tag -a 1.0.2 -m "Sudoku 1.0.2"
   git push origin master --tags
   ```

4. Add a new `Builds` entry to the F-Droid metadata with the full commit hash
   and the new version name and code. Do not use a branch or tag name in
   `commit`.
5. Update `CurrentVersion` and `CurrentVersionCode`.
6. Open a merge request against `fdroiddata` (or push to the existing one).

## Signing

F-Droid signs its builds with its own key. This means an F-Droid installation
cannot be updated over a Google Play installation, and vice versa. This is
expected and is not a bug.

Reproducible build verification can be added later with
`AllowedAPKSigningKeys` once an F-Droid-signed APK exists.
