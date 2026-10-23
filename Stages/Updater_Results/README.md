# Background app updates — 0.12.0-updates (code 20)

Install [Sung-0.12.0-updates-debug.apk](Sung-0.12.0-updates-debug.apk) manually over your current installation once. Application ID and signing certificate are unchanged; do not uninstall. Phone acceptance is pending.

Automatic updates are enabled by default in Settings → App updates. Android schedules checks/downloads on unmetered networks with battery not low, approximately every 12 hours. You confirm installation in Android; installation is not silent. The toggle disables scheduled work. A manual check is also available.

See [phone checks](PHONE_TEST_CHECKLIST.md), [implementation report](../../docs/updates/REPORT.md) and [version numbering/publishing guide](../../docs/updates/VERSIONING_AND_RELEASES.md).

## Release assets prepared locally

- APK above and `update.json`, plus `SHA256SUMS` for local verification.
- Metadata targets the proposed exact GitHub tag **v0.12.0-updates** and the APK filename above. Regenerate it if either changes. No release has been published by this task.
- A future update must have **versionCode 21 or higher**, keep the app ID and signer, and upload both its APK and generated `update.json` to a stable/latest release.
- The old prerelease cannot exercise an end-to-end upgrade from code 20. Manual phone testing and a future higher-code release are still needed.

Code checks are recorded in `verification/`. No screenshots or device installation were performed.
