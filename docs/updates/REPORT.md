# Background updater implementation — 22 October 2026

Build: **0.12.0-updates**, versionCode **20**. Application ID remains `com.karthi.sungmobile.stage0`; retained debug signer and Room schema 5 are unchanged.

## Behavior

Default-on automatic checks use Android JobScheduler, with a persisted roughly 12-hour unmetered/battery-not-low job and an initial eligible check. Settings provides a persistent toggle, manual check/download, optional update notifications and installation action. A shared in-app banner announces prepared updates. Android confirmation is required; the app never launches an installer automatically from the background.

The source is the stable latest release of `k-karthikn/SungMobile`. Its `update.json` references an APK asset from that release. Bounded HTTPS downloads stay on the permitted GitHub hosts. APK SHA-256, strictly higher versionCode, exact package, signer and minimum Android compatibility are checked before staging and again before installation. Partial downloads are discarded on cancellation/failure. A FileProvider grants access to the prepared APK for installation.

Implementation is split across `app/.../updates/` (manifest, HTTP, APK validation, scheduling, notifications and coordinator) and `app/.../feature/updates/` (settings/banner). Music downloads and deferred Stage 7 remain excluded.

## Delivery and limits

APK and manual checks (files removed 30 October 2026). [Versioning and release guide](VERSIONING_AND_RELEASES.md). `scripts/make_update_manifest.py` reads the exact APK metadata and checksum; it does not publish anything.

No release was published. An actual higher-code release and manual phone installation are still required to accept the complete update flow. Job timing depends on Android/POCO scheduling and battery restrictions. Signing-key rotation is not supported; retain the current key. Manual downloads are scoped to Settings and cancel when leaving it. No screenshot/visual/device verification was performed, as requested.

## Code verification

- Gradle nonvisual unit tests, lintDebug and assembleDebug: passed.
- Selected tests: 35; failures 0, errors 0, skipped 0. Includes updater safety/scheduling tests and selected playback, lyrics, link and collection regressions.
- Lint: 0 errors, 13 warnings. Full SARIF retained with test XML and build log under `Stages/Updater_Results/verification/` (files removed 30 October 2026).
- Packaged APK code 20 and retained certificate verified with aapt/apksigner; SHA256SUMS and update.json generated from the delivered APK.
