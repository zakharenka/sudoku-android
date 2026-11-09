#!/usr/bin/env python3
"""Set the app version in gradle.properties and fdroid/version.json."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from versioning import (  # noqa: E402
    VersionError,
    check_sync,
    format_version,
    parse_version,
    read_app_version,
    read_fdroid_version,
    version_code,
    write_app_version,
    write_fdroid_version,
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Set app.version and keep fdroid/version.json in sync. "
            "The version must use MAJOR.MINOR.PATCH."
        )
    )
    parser.add_argument(
        "version",
        nargs="?",
        help="new version, for example 1.2.0",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="only verify that gradle.properties and fdroid/version.json match",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="print the planned changes without writing files",
    )
    parser.add_argument(
        "--allow-downgrade",
        action="store_true",
        help="allow a versionCode lower than the current one",
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        if args.check:
            if args.version:
                parser.error("--check does not accept a version argument")
            version, code = check_sync()
            print(f"OK: {version} (versionCode {code})")
            return 0

        if not args.version:
            parser.error("the version argument is required unless --check is used")

        new_version = args.version.strip()
        new_parts = parse_version(new_version)
        new_code = version_code(*new_parts)

        old_version = read_app_version()
        old_code = version_code(*parse_version(old_version))
        old_fdroid_version, old_fdroid_code = read_fdroid_version()

        if new_code < old_code and not args.allow_downgrade:
            raise VersionError(
                f"refusing to downgrade from {old_version} ({old_code}) "
                f"to {new_version} ({new_code}); use --allow-downgrade if intended"
            )

        if old_fdroid_version != old_version or old_fdroid_code != old_code:
            print(
                "warning: fdroid/version.json was already out of sync: "
                f"{old_fdroid_version} ({old_fdroid_code})",
                file=sys.stderr,
            )

        if args.dry_run:
            print(f"would update app.version: {old_version} -> {new_version}")
            print(f"would update fdroid/version.json: {new_code}")
            return 0

        write_app_version(new_version)
        write_fdroid_version(new_version, new_code)
        print(f"app.version: {old_version} -> {new_version}")
        print(f"versionCode: {old_code} -> {new_code}")
        print("updated: gradle.properties, fdroid/version.json")
        return 0
    except VersionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
