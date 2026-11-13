# Release process

## Version

- The single source is `app.version` in `gradle.properties`.
- The format is `MAJOR.MINOR.PATCH`; `versionCode` is calculated as
  `major * 1_000_000 + minor * 1_000 + patch`.
- `minor` and `patch` must stay below `1000`.
- `fdroid/version.json` mirrors the version and `versionCode`.

```bash
python3 scripts/set-version.py X.Y.Z
python3 scripts/set-version.py --check
```

## Per-release checklist

1. Set the version:

   ```bash
   python3 scripts/set-version.py X.Y.Z
   ```

2. Add a changelog for every locale:

   ```text
   fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt
   ```

3. Add `docs/releases/X.Y.Z.md` and one short line in
   [`releases/README.md`](releases/README.md).

4. Validate the version and store metadata:

   ```bash
   python3 scripts/set-version.py --check
   python3 scripts/build-store-package.py --check
   ```

5. Run the full check:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   ```

6. Build the AAB for Google Play:

   ```bash
   ./gradlew :app:bundleRelease
   ```

7. Build the local store package:

   ```bash
   python3 scripts/build-store-package.py
   ```

8. Commit and tag:

   ```bash
   git commit -m "chore: release X.Y.Z"
   git tag -a X.Y.Z -m "Sudoku X.Y.Z"
   git push origin master --tags
   ```

Only the version files, fastlane changelogs and release notes change for a
normal release. README, AGENTS and the reference docs are updated with the code,
not with the version number.

## Google Play

- The AAB is at `app/build/outputs/bundle/release/app-release.aab`; sign it with
  the upload key before uploading.
- The store package is in `store_out/`: `listings.csv` and
  `release-notes.txt`. Icons and screenshots are used directly from
  `fastlane/metadata/android/`.
- Upload the AAB to the chosen track and apply the listing and release notes.
- Update titles, descriptions and images only when the product changes. If the
  in-game UI changed, refresh the store screenshots as described in
  [`screenshots.md`](screenshots.md).

## F-Droid

F-Droid builds from the tagged source; no signed APK is uploaded. The repository
must stay public and the app must not contain ads, tracking or proprietary
dependencies.

- The build recipe is `metadata/me.zakharenka.sudoku.yml`.
- `fdroid/version.json` drives the tag-based auto-update.
- After a tag, F-Droid adds the build entry automatically. Do not edit `Builds`,
  `CurrentVersion` or `CurrentVersionCode` by hand.
- If the app is not in F-Droid yet, copy the recipe into a
  [fdroiddata](https://gitlab.com/fdroid/fdroiddata) fork, run
  `fdroid lint me.zakharenka.sudoku`, and open a merge request.
