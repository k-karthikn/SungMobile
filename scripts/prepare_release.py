#!/usr/bin/env python3
"""Test, version, tag, and push a SungMobile production release.

Run from a clean main checkout, for example:
    python3 scripts/prepare_release.py 0.16.1

The supplied version name determines the Git tag (v0.16.1). By default the
Android version code is increased by one; pass --version-code when the next
production code must be higher than a code already distributed outside Git.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BUILD_FILE = ROOT / "app/build.gradle.kts"
VERSION_NAME = re.compile(
    r'^(?P<prefix>[ \t]*versionName\s*=\s*")(?P<name>[^"]+)(?P<suffix>")[ \t]*$', re.MULTILINE
)
VERSION_CODE = re.compile(
    r'^(?P<prefix>[ \t]*versionCode\s*=\s*)(?P<code>\d+)(?P<suffix>[ \t]*)$', re.MULTILINE
)
SAFE_VERSION = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\Z")


class ReleasePreparationError(Exception):
    pass


def run(*command: str, capture: bool = False) -> str:
    completed = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        check=True,
    )
    return completed.stdout or ""


def require_clean_main_checkout() -> None:
    if run("git", "status", "--porcelain", capture=True).strip():
        raise ReleasePreparationError("Commit or stash all changes before preparing a release")
    if run("git", "branch", "--show-current", capture=True).strip() != "main":
        raise ReleasePreparationError("Prepare releases from the main branch")


def remote_tag_exists(tag: str) -> bool:
    return bool(run("git", "ls-remote", "--tags", "origin", f"refs/tags/{tag}", capture=True).strip())


def read_version() -> tuple[int, str, str]:
    source = BUILD_FILE.read_text(encoding="utf-8")
    code = VERSION_CODE.search(source)
    name = VERSION_NAME.search(source)
    if code is None or name is None:
        raise ReleasePreparationError("Could not find exactly one versionCode and versionName in app/build.gradle.kts")
    if len(VERSION_CODE.findall(source)) != 1 or len(VERSION_NAME.findall(source)) != 1:
        raise ReleasePreparationError("Expected exactly one versionCode and versionName in app/build.gradle.kts")
    return int(code.group("code")), name.group("name"), source


def update_version(source: str, code: int, name: str) -> None:
    updated = VERSION_CODE.sub(lambda match: f"{match.group('prefix')}{code}{match.group('suffix')}", source)
    updated = VERSION_NAME.sub(lambda match: f"{match.group('prefix')}{name}{match.group('suffix')}", updated)
    BUILD_FILE.write_text(updated, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version_name", help="New Android versionName and tag suffix, for example 0.16.1")
    parser.add_argument("--version-code", type=int,
                        help="New Android versionCode (defaults to the current code plus one)")
    args = parser.parse_args()

    if SAFE_VERSION.fullmatch(args.version_name) is None:
        parser.error("version_name must contain only letters, numbers, dots, underscores, or hyphens")

    require_clean_main_checkout()
    current_code, current_name, source = read_version()
    new_code = args.version_code if args.version_code is not None else current_code + 1
    tag = f"v{args.version_name}"
    if new_code <= current_code:
        raise ReleasePreparationError(f"versionCode must exceed the current value {current_code}")
    if args.version_name == current_name:
        raise ReleasePreparationError("versionName must change for a new release")
    if run("git", "rev-parse", "-q", "--verify", f"refs/tags/{tag}", capture=True).strip() or remote_tag_exists(tag):
        raise ReleasePreparationError(f"Tag {tag} already exists")

    print("Running host tests and release lint…", flush=True)
    run("./gradlew", "--no-daemon", "--stacktrace", "--console=plain", "test", ":app:lintRelease")

    update_version(source, new_code, args.version_name)
    run("git", "add", "app/build.gradle.kts")
    run("git", "commit", "-m", f"release: prepare SungMobile {args.version_name}")
    run("git", "tag", "-a", tag, "-m", f"SungMobile {tag}")
    run("git", "push", "origin", "HEAD")
    run("git", "push", "origin", tag)
    print(f"Pushed {tag}. GitHub Actions will build, sign, and publish the release.")


if __name__ == "__main__":
    try:
        main()
    except (ReleasePreparationError, OSError, subprocess.CalledProcessError) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        sys.exit(1)
