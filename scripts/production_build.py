#!/usr/bin/env python3
"""Build and verify a production APK, then prepare its GitHub release assets.

Run from Android Studio's Terminal with ``python3 scripts/production_build.py``.
The script never publishes a release or reads the production key itself; Gradle
uses the existing signing.properties configuration.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.karthi.sungmobile"
SIGNER_SHA256 = "409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd"
APK_LIMIT = 150 * 1024 * 1024
BADGING = re.compile(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'")
SIMPLE_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\Z")
CERT_SHA256 = re.compile(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]{64})")


class ReleaseError(Exception):
    pass


def java_environment(explicit: Path | None) -> dict[str, str]:
    environment = os.environ.copy()
    candidate = explicit or (Path(environment["JAVA_HOME"]) if environment.get("JAVA_HOME") else None)
    if candidate is None and Path("/opt/android-studio/jbr/bin/java").is_file():
        candidate = Path("/opt/android-studio/jbr")
    if candidate is not None:
        candidate = candidate.expanduser().resolve()
        if not (candidate / "bin/java").is_file():
            raise ReleaseError(f"Java was not found under {candidate}; use --java-home to select the Android Studio JBR")
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
    raise ReleaseError(f"Could not find aapt, apksigner and zipalign in {parent}")


def checked(command: list[str], *, environment: dict[str, str], capture: bool = False) -> str:
    result = subprocess.run(command, cwd=ROOT, env=environment, text=True,
        stdout=subprocess.PIPE if capture else None, check=True)
    return result.stdout or ""


def metadata(apk: Path, aapt: Path, environment: dict[str, str]) -> tuple[int, str]:
    if not apk.is_file():
        raise ReleaseError(f"APK does not exist: {apk}")
    if apk.stat().st_size > APK_LIMIT:
        raise ReleaseError("APK exceeds the updater's 150 MiB download limit")
    badging = checked([str(aapt), "dump", "badging", str(apk)], environment=environment, capture=True)
    match = BADGING.search(badging)
    if match is None:
        raise ReleaseError("Could not read package and version from the APK")
    package, code, name = match.groups()
    if package != PACKAGE:
        raise ReleaseError(f"Expected production package {PACKAGE}, found {package}")
    if not 0 < int(code) <= 2_100_000_000:
        raise ReleaseError(f"Invalid versionCode: {code}")
    if SIMPLE_NAME.fullmatch(name) is None:
        raise ReleaseError(f"Version name cannot be used safely in an APK filename: {name}")
    return int(code), name


def verify_apk(apk: Path, apksigner: Path, zipalign: Path, environment: dict[str, str]) -> None:
    signing = checked([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)],
        environment=environment, capture=True)
    certificate = CERT_SHA256.search(signing)
    if certificate is None:
        raise ReleaseError("Could not read the APK signing certificate")
    if certificate.group(1).lower() != SIGNER_SHA256:
        raise ReleaseError("APK is signed with a different key and cannot update the existing production app")
    checked([str(zipalign), "-c", "4", str(apk)], environment=environment, capture=True)


def sha256(apk: Path) -> str:
    with apk.open("rb") as contents:
        return hashlib.file_digest(contents, "sha256").hexdigest()


def prepare_assets(source: Path, destination: Path, tag: str, aapt: Path, apksigner: Path,
    zipalign: Path,
    environment: dict[str, str], replace: bool, version_code: int, version_name: str) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    source_digest = sha256(source)
    if destination.exists() and sha256(destination) != source_digest and not replace:
        raise ReleaseError(f"A different APK already exists at {destination}; use a new version or --replace before publishing")
    if source != destination:
        temporary = destination.with_name(destination.name + ".tmp")
        try:
            shutil.copyfile(source, temporary)
            os.replace(temporary, destination)
        finally:
            temporary.unlink(missing_ok=True)

    if sha256(destination) != source_digest:
        raise ReleaseError("The copied APK does not match the build output")
    verify_apk(destination, apksigner, zipalign, environment)

    manifest = destination.parent / "update.json"
    temporary_manifest = destination.parent / "update.json.tmp"
    try:
        checked([sys.executable, str(ROOT / "scripts/make_update_manifest.py"),
            "--apk", str(destination), "--tag", tag, "--package-name", PACKAGE,
            "--aapt", str(aapt), "--output", str(temporary_manifest)],
            environment=environment, capture=True)
        data = json.loads(temporary_manifest.read_text(encoding="utf-8"))
        if (data.get("versionCode") != version_code or data.get("versionName") != version_name
            or data.get("packageName") != PACKAGE or data.get("sha256") != source_digest
            or not data.get("apkUrl", "").endswith(f"/{tag}/{destination.name}")):
            raise ReleaseError("Generated update.json does not match the packaged APK")
        os.replace(temporary_manifest, manifest)
    finally:
        temporary_manifest.unlink(missing_ok=True)
    (destination.parent / "SHA256SUMS").write_text(
        f"{source_digest}  {destination.name}\n", encoding="utf-8")
    print(f"\nReady to upload to GitHub release {tag}:")
    print(f"  {destination}")
    print(f"  {manifest}")
    print(f"  SHA-256: {source_digest}")
    print("Publish them as assets of the matching stable release; this script does not publish it.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, help="Package an already-built APK without running Gradle checks")
    parser.add_argument("--tag", help="Exact GitHub release tag (default: v followed by the APK versionName)")
    parser.add_argument("--output-dir", type=Path,
        help="Asset directory (default: release-assets/<tag> under this project)")
    parser.add_argument("--sdk-root", type=Path, help="Android SDK directory if local.properties does not specify it")
    parser.add_argument("--java-home", type=Path, help="Android Studio JBR directory if JAVA_HOME is not set")
    parser.add_argument("--replace", action="store_true",
        help="Replace a different local APK for this version before publication")
    args = parser.parse_args()

    environment = java_environment(args.java_home)
    aapt, apksigner, zipalign = build_tools(sdk_root(args.sdk_root))
    if args.apk is None:
        print("Running production tests, lint and release assembly…", flush=True)
        checked([str(ROOT / "gradlew"), "--console=plain",
            ":core:model:test", ":core:youtube:test", ":core:playback:testDebugUnitTest",
            ":core:data:testDebugUnitTest", ":app:testDebugUnitTest", ":app:lintRelease",
            ":app:assembleRelease"], environment=environment)
        source = ROOT / "app/build/outputs/apk/release/app-release.apk"
    else:
        source = args.apk.expanduser().resolve()
        print("Using the supplied APK; Gradle tests, lint and assembly were skipped.", flush=True)

    version_code, version_name = metadata(source, aapt, environment)
    verify_apk(source, apksigner, zipalign, environment)
    tag = args.tag or f"v{version_name}"
    if SIMPLE_NAME.fullmatch(tag) is None:
        raise ReleaseError(f"Invalid release tag: {tag}")
    directory = (args.output_dir.expanduser().resolve() if args.output_dir
        else ROOT / "release-assets" / tag)
    destination = directory / f"SungMobile-{version_name}-code{version_code}-release.apk"
    prepare_assets(source, destination, tag, aapt, apksigner, zipalign,
        environment, args.replace, version_code, version_name)


if __name__ == "__main__":
    try:
        main()
    except (ReleaseError, OSError, subprocess.CalledProcessError, json.JSONDecodeError) as error:
        print(f"Production build failed: {error}", file=sys.stderr)
        sys.exit(1)
