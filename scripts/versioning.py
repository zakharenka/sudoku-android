"""Shared helpers for the release scripts."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE_PROPERTIES = ROOT / "gradle.properties"
FDROID_VERSION = ROOT / "fdroid" / "version.json"

VERSION_RE = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
APP_VERSION_RE = re.compile(r"^app\.version=([^\r\n]+)$", re.MULTILINE)
MAX_VERSION_CODE = 2_147_483_647


class VersionError(ValueError):
    """Raised when a version value is invalid or out of sync."""


def parse_version(value: str) -> tuple[int, int, int]:
    match = VERSION_RE.fullmatch(value.strip())
    if not match:
        raise VersionError(
            f"invalid version {value!r}; expected MAJOR.MINOR.PATCH"
        )

    major, minor, patch = (int(part) for part in match.groups())
    if minor > 999 or patch > 999:
        raise VersionError(
            "minor and patch must be between 0 and 999 so versionCode stays unique"
        )
    if version_code(major, minor, patch) > MAX_VERSION_CODE:
        raise VersionError("versionCode does not fit into a signed 32-bit integer")
    return major, minor, patch


def version_code(major: int, minor: int, patch: int) -> int:
    return major * 1_000_000 + minor * 1_000 + patch


def format_version(parts: tuple[int, int, int]) -> str:
    return ".".join(str(part) for part in parts)


def read_app_version(path: Path = GRADLE_PROPERTIES) -> str:
    text = path.read_text(encoding="utf-8")
    match = APP_VERSION_RE.search(text)
    if not match:
        raise VersionError(f"app.version not found in {path}")
    return match.group(1).strip()


def write_app_version(version: str, path: Path = GRADLE_PROPERTIES) -> None:
    text = path.read_text(encoding="utf-8")
    match = APP_VERSION_RE.search(text)
    if not match:
        raise VersionError(f"app.version not found in {path}")
    path.write_text(
        text[: match.start(1)] + version + text[match.end(1) :],
        encoding="utf-8",
    )


def read_fdroid_version(path: Path = FDROID_VERSION) -> tuple[str, int]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return str(data["versionName"]), int(data["versionCode"])
    except (KeyError, TypeError, ValueError) as error:
        raise VersionError(f"invalid {path}: {error}") from error


def write_fdroid_version(
    version: str,
    code: int,
    path: Path = FDROID_VERSION,
) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    data["versionName"] = version
    data["versionCode"] = code
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def check_sync() -> tuple[str, int]:
    version = read_app_version()
    major, minor, patch = parse_version(version)
    expected_code = version_code(major, minor, patch)
    fdroid_version, fdroid_code = read_fdroid_version()

    if fdroid_version != version:
        raise VersionError(
            f"fdroid/version.json has versionName {fdroid_version!r}, "
            f"expected {version!r}"
        )
    if fdroid_code != expected_code:
        raise VersionError(
            f"fdroid/version.json has versionCode {fdroid_code}, "
            f"expected {expected_code}"
        )

    return version, expected_code
