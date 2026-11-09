#!/usr/bin/env python3
"""Build the local Google Play store package from fastlane metadata."""

from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from versioning import (  # noqa: E402
    ROOT,
    VersionError,
    check_sync,
)

FASTLANE = ROOT / "fastlane" / "metadata" / "android"
STORE_OUT = ROOT / "store_out"
AAB = ROOT / "app" / "build" / "outputs" / "bundle" / "release" / "app-release.aab"

# Play Console expects the language code in the first column.
PLAY_LOCALES: list[tuple[str, str]] = [
    ("en", "en-US"),
    ("ru", "ru-RU"),
    ("es", "es-ES"),
    ("pt-BR", "pt-BR"),
    ("fr", "fr-FR"),
    ("de", "de-DE"),
    ("it", "it-IT"),
    ("uk", "uk"),
    ("be", "be"),
]

# Keep release notes in the same stable order as the previous store package.
RELEASE_NOTE_LOCALES = [
    "en-US",
    "be",
    "de-DE",
    "es-ES",
    "fr-FR",
    "it-IT",
    "pt-BR",
    "ru-RU",
    "uk",
]

LIMITS = {
    "title.txt": 50,
    "short_description.txt": 80,
    "full_description.txt": 4000,
}


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def collect_locale(folder: str, version_code: int) -> dict[str, str]:
    root = FASTLANE / folder
    data: dict[str, str] = {}

    for filename, limit in LIMITS.items():
        path = root / filename
        if not path.is_file():
            raise VersionError(f"missing {path}")
        value = read_text(path)
        if len(value) > limit:
            raise VersionError(
                f"{path} is {len(value)} characters; limit is {limit}"
            )
        data[filename] = value

    changelog = root / "changelogs" / f"{version_code}.txt"
    if not changelog.is_file():
        raise VersionError(f"missing changelog for {folder}: {changelog}")
    changelog_text = read_text(changelog)
    if len(changelog_text) > 500:
        raise VersionError(
            f"{changelog} is {len(changelog_text)} characters; limit is 500"
        )
    data["changelog"] = changelog_text
    return data


def write_listings(locales: dict[str, dict[str, str]]) -> Path:
    path = STORE_OUT / "listings.csv"
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(["language", "title", "short_description", "full_description"])
        for play_code, folder in PLAY_LOCALES:
            data = locales[folder]
            writer.writerow(
                [
                    play_code,
                    data["title.txt"],
                    data["short_description.txt"],
                    data["full_description.txt"],
                ]
            )
    return path


def write_release_notes(locales: dict[str, dict[str, str]]) -> Path:
    path = STORE_OUT / "release-notes.txt"
    blocks = []
    for folder in RELEASE_NOTE_LOCALES:
        blocks.append(f"<{folder}>\n{locales[folder]['changelog']}\n</{folder}>")
    path.write_text("\n".join(blocks) + "\n", encoding="utf-8")
    return path


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Validate fastlane metadata and build store_out/listings.csv "
            "and store_out/release-notes.txt."
        )
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate metadata without writing the store package",
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        version, code = check_sync()
        missing = [
            folder
            for _, folder in PLAY_LOCALES
            if not (FASTLANE / folder).is_dir()
        ]
        if missing:
            raise VersionError(f"missing fastlane locale folders: {', '.join(missing)}")

        locales = {
            folder: collect_locale(folder, code) for _, folder in PLAY_LOCALES
        }

        if args.check:
            print(f"OK: {version} (versionCode {code}), {len(locales)} locales")
            return 0

        STORE_OUT.mkdir(parents=True, exist_ok=True)
        listings = write_listings(locales)
        release_notes = write_release_notes(locales)

        print(f"Sudoku {version} (versionCode {code})")
        print(f"store package: {STORE_OUT}")
        print(f"  {listings.name}")
        print(f"  {release_notes.name}")
        print(f"AAB: {AAB.relative_to(ROOT)}")
        print("Sign the AAB with the upload key before uploading it to Google Play.")
        return 0
    except VersionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
