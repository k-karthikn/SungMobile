#!/usr/bin/env python3
"""Generate release metadata from the exact APK to upload (does not publish anything)."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from urllib.parse import quote

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path, required=True)
parser.add_argument('--tag', required=True, help='Exact GitHub release tag, e.g. v0.12.1')
parser.add_argument('--aapt', required=True, help='SDK build-tools aapt path')
parser.add_argument('--package-name', choices=['com.karthi.sungmobile', 'com.karthi.sungmobile.stage0'],
                    default='com.karthi.sungmobile', help='Package expected in this APK (default: production)')
parser.add_argument('--output', type=Path, default=Path('update.json'))
parser.add_argument('--build-output', type=Path,
                    help='Optional build.json output with release provenance for this APK')
args = parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*', args.tag):
    parser.error('Use a simple release tag containing letters, numbers, dots, underscores or hyphens.')
if args.build_output is not None and args.build_output.resolve() == args.output.resolve():
    parser.error('--build-output must not overwrite update.json')
badging = subprocess.check_output([args.aapt, 'dump', 'badging', str(args.apk)], text=True)
match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
if not match:
    parser.error('Could not read APK version metadata')
package, code, name = match.groups()
if package != args.package_name:
    parser.error(f'Wrong application ID; expected {args.package_name}')
if not 0 < int(code) <= 2_100_000_000:
    parser.error('Invalid versionCode')
if args.apk.stat().st_size > 150 * 1024 * 1024:
    parser.error('APK exceeds updater download limit (150 MiB)')
with args.apk.open('rb') as apk:
    digest = hashlib.file_digest(apk, 'sha256').hexdigest()
manifest = dict(versionCode=int(code), versionName=name, packageName=package,
    apkUrl=f'https://github.com/k-karthikn/SungMobile/releases/download/{quote(args.tag)}/{quote(args.apk.name)}',
    sha256=digest)
args.output.write_text(json.dumps(manifest, indent=2) + '\n')
if args.build_output is not None:
    build_manifest = dict(schemaVersion=1, tag=args.tag, apkName=args.apk.name, **manifest)
    args.build_output.write_text(json.dumps(build_manifest, indent=2) + '\n')
print(f'Wrote {args.output}: {name}, code {code}. Upload alongside {args.apk.name} to a stable release.')
if args.build_output is not None:
    print(f'Wrote {args.build_output}: release provenance for {args.apk.name}.')
