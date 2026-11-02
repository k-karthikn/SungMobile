#!/usr/bin/env python3
"""Build and verify the installable SungMobile debug APK.

Run from the project root with ``python3 scripts/debug_build.py``. The script
runs only the app's debug assembly task; it does not run tests or lint.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.karthi.sungmobile.stage0"
BADGING = re.compile(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'")


class DebugBuildError(Exception):
    pass


def java_environment(explicit: Path | None) -> dict[str, str]:
    environment = os.environ.copy()
    candidate = explicit or (Path(environment["JAVA_HOME"]) if environment.get("JAVA_HOME") else None)
    if candidate is None and Path("/opt/android-studio/jbr/bin/java").is_file():
        candidate = Path("/opt/android-studio/jbr")
    if candidate is not None:
        candidate = candidate.expanduser().resolve()
        if not (candidate / "bin/java").is_file():
            raise DebugBuildError(
                f"Java was not found under {candidate}; use --java-home to select the Android Studio JBR"
            )
        environment["JAVA_HOME"] = str(candidate)
        environment["PATH"] = str(candidate / "bin") + os.pathsep + environment.get("PATH", "")
    return environment


def sdk_root(explicit: Path | None) -> Path:
    if explicit is not None:
        return explicit.expanduser().resolve()
    properties = ROOT / "local.properties"
    if properties.is_file():
        for line in properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                return Path(line.partition("=")[2].strip()).expanduser().resolve()
    for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(key):
            return Path(os.environ[key]).expanduser().resolve()
    return Path.home() / "Android/Sdk"


def build_tools(sdk: Path) -> tuple[Path, Path, Path]:
    parent = sdk / "build-tools"
    candidates = sorted(
        (folder for folder in parent.iterdir() if folder.is_dir()) if parent.is_dir() else (),
        key=lambda folder: tuple(int(part) for part in re.findall(r"\d+", folder.name)),
        reverse=True,
    )
    for folder in candidates:
        tools = tuple(folder / name for name in ("aapt", "apksigner", "zipalign"))
        if all(tool.is_file() for tool in tools):
            return tools
    raise DebugBuildError(f"Could not find aapt, apksigner and zipalign in {parent}")


def checked(command: list[str], environment: dict[str, str], *, capture: bool = False) -> str:
    result = subprocess.run(
        command,
        cwd=ROOT,
        env=environment,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        check=True,
    )
    return result.stdout or ""


def inspect_apk(apk: Path, aapt: Path, apksigner: Path, zipalign: Path,
    environment: dict[str, str]) -> tuple[int, str, str]:
    if not apk.is_file():
        raise DebugBuildError(f"Gradle did not produce the expected APK: {apk}")
    badging = checked([str(aapt), "dump", "badging", str(apk)], environment, capture=True)
    match = BADGING.search(badging)
    if match is None:
        raise DebugBuildError("Could not read package and version from the APK")
    package, version_code, version_name = match.groups()
    if package != PACKAGE:
        raise DebugBuildError(f"Expected debug package {PACKAGE}, found {package}")
    checked([str(apksigner), "verify", "--verbose", str(apk)], environment, capture=True)
    checked([str(zipalign), "-c", "4", str(apk)], environment, capture=True)
    with apk.open("rb") as contents:
        digest = hashlib.file_digest(contents, "sha256").hexdigest()
    return int(version_code), version_name, digest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk-root", type=Path, help="Android SDK directory if local.properties does not specify it")
    parser.add_argument("--java-home", type=Path, help="Android Studio JBR directory if JAVA_HOME is not set")
    parser.add_argument("--gradle-user-home", type=Path,
        help="Gradle cache directory; defaults to Gradle's normal user home or GRADLE_USER_HOME")
    args = parser.parse_args()

    environment = java_environment(args.java_home)
    if args.gradle_user_home is not None:
        environment["GRADLE_USER_HOME"] = str(args.gradle_user_home.expanduser().resolve())
    aapt, apksigner, zipalign = build_tools(sdk_root(args.sdk_root))

    print("Running :app:assembleDebug only (no tests or lint)…", flush=True)
    checked([str(ROOT / "gradlew"), "--no-daemon", "--console=plain", ":app:assembleDebug"], environment)

    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    version_code, version_name, digest = inspect_apk(apk, aapt, apksigner, zipalign, environment)
    print("\nDebug APK is ready:")
    print(f"  {apk}")
    print(f"  Package: {PACKAGE}")
    print(f"  Version: {version_name} (code {version_code})")
    print(f"  SHA-256: {digest}")


if __name__ == "__main__":
    try:
        main()
    except (DebugBuildError, OSError, subprocess.CalledProcessError) as error:
        print(f"Debug build failed: {error}", file=sys.stderr)
        sys.exit(1)
