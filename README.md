# SungMobile

Latest production candidate: **0.14.1 (code 35)** — a vertical, colorful top-three ranking in Listening statistics; feedback with optional diagnostic logs; local-song artwork recovery; wallpaper-based color generation; top-aligned synced lyrics with a non-Latin fallback; and a cleaner Search actions button. Phone acceptance is pending.

Earlier production candidate: **0.14.0 (code 34)** — a dedicated Material 3 listening statistics dashboard with total time, a 7/30-day activity graph, compact metrics, top songs, and recent listening. Its Home entry is prominent, and the mini-player is hidden while the page is open. [Signed APK and verification](Stages/Listening_Statistics_Results/README.md). Phone acceptance is pending.

Earlier production candidate: **0.13.1 (code 33)** — a compact, regular Queue list with cleaned artist names, square artwork, and available durations; the Search duplicate button is removed and the player seek track is refined. [Signed APK and verification](Stages/Queue_UI_Results/README.md). Phone acceptance is pending.

Earlier production candidate: **0.13.0 (code 32)** — provider-backed Home shelves, animated artwork carousels, compact rounded-rectangle mood chips, and direct song/collection card actions. [Signed APK and verification](Stages/Provider_Home_Results/README.md); [Home implementation report](docs/ui/home/PROVIDER_HOME_IMPLEMENTATION_REPORT.md).

Earlier production candidate: **0.12.11 (code 31)** — local-song lyrics, a refined Material 3 Blue palette, and bounded recovery for online streams that fail or stall while buffering. The [signed APK](Stages/Playback_Reliability_Results/README.md) was installed over code 30 on the connected Android 15 phone; phone acceptance of these new features is pending.

Earlier production candidate: **0.12.10 (code 30)** — fixes music downloads on devices whose MediaStore rejects WebM audio. [Versioned APK and phone result](Stages/Download_Compatibility_Results/README.md). A download completed on the connected Android 15 phone; publication is pending.

Previous production candidate: **0.12.9 (code 29)** — a roomier Local songs tab and cached scans. [APK and phone checklist](Stages/Local_Audio_Viewport_Results/README.md).

Earlier production candidate: **0.12.7 (code 27)** — lighter theme-colored active lyrics and the same expressive play/pause control across the mini-player and Now Playing. [APK and manual checklist](Stages/Lyrics_Emphasis_Results/README.md). Phone acceptance pending; publishing was not performed in this task.

Previous production candidate: **0.12.6 (code 26)** — all five expressive UI slices, including Home, Search, Lyrics, loading and settings. [APK and manual checklist](Stages/Expressive_UI_Results/README.md).

Previous production candidate: **0.12.5 (code 25)** — shared expressive mini-player/navigation and Library tabs, Mixes and settings access. [APK and manual checklist](Stages/Expressive_Library_Results/README.md).

Previous production candidate: **0.12.4 (code 24)** — compact player transport and seek controls with a swipe-up Queue sheet. [APK and manual checklist](Stages/Player_Queue_UI_Results/README.md).

Production release candidate: **0.12.3 (code 23)**, signed and optimized for `com.karthi.sungmobile`. [Versioned APK and manual checklist](Stages/Production_Release_Results/README.md). This installs alongside the earlier `.stage0` debug app; its local library starts fresh. Phone acceptance and public release are pending.

Earlier icon build: **0.12.3-icon (code 23)** — corrected adaptive launcher icon placement. [APK and phone checklist](Stages/Icon_Results/README.md). Phone acceptance pending.

Earlier Lyrics UI build: **0.12.2-lyrics-ui (code 22)** — Lyrics screen, grouped settings and confirmation dialogs. [APK and manual checks](Stages/Lyrics_UI_Results/README.md). [Implementation report](docs/ui/lyrics/REPORT.md). Phone acceptance pending.

Latest UI build: **0.12.1-search-ui (code 21)** — Search layout, category icons, clear action, recent searches and shared heart/plus result rows. [APK and manual checks](Stages/Search_UI_Results/README.md). [Search report](docs/ui/search/REPORT.md). Phone acceptance pending.

Latest build: **0.12.0-updates (code 20)** — default-on background update checks/downloads with an off switch and Android installation confirmation. [APK and manual checks](Stages/Updater_Results/README.md). [Version numbering and publishing guide](docs/updates/VERSIONING_AND_RELEASES.md). Phone acceptance pending; no GitHub release published by this task.

Latest UI build: **0.11.2-library-ui (code 19)** — Library, playlist/song rows, collection styling and Sessions access. [APK and manual checks](Stages/Library_UI_Results/README.md). Player UI code 18 accepted; Library phone acceptance pending. [Library implementation report](docs/ui/library/REPORT.md).

Latest UI build: **0.11.1-player-ui (code 18)** — Now Playing, queue previews, More sheet and Queue rows. [APK and manual checks](Stages/Player_UI_Results/README.md). Same app ID/signing identity; phone acceptance pending. [Implementation report](docs/ui/player/REPORT.md).

**Restoring after an OS change or continuing in a new session? Start with [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md)** for progress, backup requirements, environment setup and the exact next steps.

A native Kotlin / Jetpack Compose Android music app, developed in functional milestones from the Sung Linux reference. Stages 4–6 were accepted on the user's POCO on 18 October 2026. Stage 6 adds playback convenience.

The owner confirmed the online-only **0.8.1-online**, code 13, and **0.9.0-stage8**, code 14, builds work on the POCO. Stage 9 is implemented as **0.10.0-stage9**, code 15, with smart playlists, review-based cleanup, local playlist versions and listening statistics; the owner reported it working on the POCO. Stage 10 is skipped. The baseline stabilization build is **0.10.1-stabilization**, code 16. The owner then authorized page-by-page UI work; the current Home UI build is **0.11.0-home-ui**, code 17, with manual visual/scroll acceptance pending. See the [Home UI report](docs/ui/home/REPORT.md). See [the stabilization report](docs/STABILIZATION_REPORT.md). See the [Stage 9 report](docs/STAGE9_REPORT.md). App-data backup/import/export and new playlist-link exchange were excluded at the owner's request. Stage 7 source remains in [deferred/stage7](deferred/stage7/README.md) outside build source sets.

See the [completion roadmap](docs/FUNCTIONAL_COMPLETION_PLAN.md), [code guide](docs/PLAYBACK_GUIDE.md), [Stage 3 report](docs/STAGE3_REPORT.md), and [accepted phone checklist](Stages/Stage3_Results/PHONE_TEST_CHECKLIST.md).

## Structure

| Module | Responsibility |
| --- | --- |
| `core:model` | Provider-neutral catalog and queue contracts, entry identities, ordering rules and transient stream model; pure Kotlin. |
| `core:youtube` | NewPipeExtractor adapter, bounded catalog responses, cancellable HTTP, paging and source error mapping; pure Kotlin. |
| `core:playback` | Testable queue/playback coordinator, persistence scheduling, Media3 engine and session-player adapter. |
| `core:data` | Room queue, local library and history repositories; explicit database migration; DataStore settings. |
| `core:designsystem` | M3 Expressive theme, typography, spacing, icons, artwork loading/masks and shared components. |
| `app` | Application dependencies, media service/controller connection, catalog ViewModel, navigation and feature screens. |

Extractor objects never enter UI or player APIs. Signed audio URLs are resolved on demand and are neither persisted nor intentionally logged. Search responses are protected from stale requests. HTTP 403/410 during playback triggers at most one fresh resolution; other failures require an explicit retry.

## Build

Use Android Studio's bundled JBR 25, Android SDK platform 37 and Build Tools 36.0.0. The Gradle wrapper is included; no system Gradle installation is needed. Set `sdk.dir` in the ignored `local.properties`, or set `ANDROID_HOME` to your SDK directory.

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew :core:model:test :core:youtube:test :core:playback:testDebugUnitTest :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; the current Home UI delivery is under `Stages/Home_UI_Results/`. Its application ID is `com.karthi.sungmobile.stage0`, retained so later debug stages upgrade the existing debug installation; it is separate from the production app. Use the **new debug key** created after OS migration for code 7 and later. Production signing uses a dedicated local key described in the [release guide](docs/updates/VERSIONING_AND_RELEASES.md). Minimum API is 26; the tested physical device is Android 15 / API 35. Older supported APIs still need device coverage.

### Signed release APK and `update.json`

Open **Terminal** in Android Studio. For a complete local release build, run:

```bash
cd /home/karthi/Projects/SungMobile
python3 scripts/production_build.py
```

The script runs the project tests and release lint, builds the signed APK, verifies its production certificate and ZIP alignment, then creates a versioned APK, `update.json`, and `SHA256SUMS` under `release-assets/v<versionName>/`. It reads the APK's actual version and uses `v<versionName>` as the default GitHub tag. With the current `0.14.2` / code `36`, upload `release-assets/v0.14.2/SungMobile-0.14.2-code36-release.apk` and `release-assets/v0.14.2/update.json` to the matching stable GitHub release. It does not publish the release. For later updates, increase `versionCode` and set `versionName` in `app/build.gradle.kts` first; the script will then name the assets automatically. It refuses to overwrite a different APK under the same version and tag unless you pass `--replace` before publication. The release build uses the production key configured in `~/.android/sungmobile-release/signing.properties`; keep using that same key for updates.

If Android Studio has already built the signed APK, `python3 scripts/production_build.py --apk app/build/outputs/apk/release/app-release.apk` verifies and packages that APK without rerunning Gradle. The manual equivalent for the current version is:

```bash
cd /home/karthi/Projects/SungMobile
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:lintRelease :app:assembleRelease
mkdir -p release-assets
cp app/build/outputs/apk/release/app-release.apk release-assets/SungMobile-0.14.2-code36-release.apk
PATH=/opt/android-studio/jbr/bin:$PATH /home/karthi/Android/Sdk/build-tools/36.0.0/apksigner verify --print-certs release-assets/SungMobile-0.14.2-code36-release.apk
python3 scripts/make_update_manifest.py \
  --apk release-assets/SungMobile-0.14.2-code36-release.apk \
  --tag v0.14.2 \
  --package-name com.karthi.sungmobile \
  --aapt /home/karthi/Android/Sdk/build-tools/36.0.0/aapt \
  --output release-assets/update.json
```

For the manual steps, upload `release-assets/SungMobile-0.14.2-code36-release.apk` and `release-assets/update.json` under the **same tag** (`v0.14.2`). For later updates, change the manual filename and tag to match the new version. Generate `update.json` only after the final APK filename and bytes are fixed; regenerate it if you rebuild or rename the APK. See the [full release guide](docs/updates/VERSIONING_AND_RELEASES.md).

Live network probes are opt-in:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :core:youtube:test -Plive=true --tests '*LiveProviderTest'
```

Connect an authorized Android device to run instrumentation. The live test plays a few seconds of audio:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.live=true
```

Without `live=true`, device tests cover deterministic UI states and skip live playback. Host tests similarly skip network probes without `-Plive=true`. On the tested HyperOS phone, the runner's background activity launch was blocked, so device verification used the user's manual checks; automated device tests are not claimed as passing. See [the Stage 0 report](docs/STAGE0_REPORT.md) for results and limitations.

## Scope and license

This build supports search, collection browsing, service-owned play/pause/seek, automatic queue progression, shuffle/repeat, queue editing/Undo and paused queue restoration. The user reported successful completion of this milestone's physical-device checklist, covering background playback, media controls and interruptions. The UI does not pause playback when it leaves the foreground.

It also provides persistent likes, local playlists, pins, bulk library actions, listening/search history and privacy controls. Stage 4 adds debounced search, validated shared/pasted links, paged collection actions with explicit partial-load choices, and privacy-aware local mixes. Stage 5 adds YouTube lyrics where available, optional matched LRCLIB fallback and essential lyric settings. Stage 6 adds speed/pitch control, sleep timer, long-recording resume points and saved sessions. The current source adds on-device songs and minimal downloads without restoring the deferred Stage 7 database or settings. Account sign-in is outside the current local-library scope.

SungMobile is licensed under **GPL-3.0-or-later**, matching the selected NewPipeExtractor dependency. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md). This development APK is not a public release. The release stage must package the corresponding source and complete dependency/asset notices before public distribution.


Stage 3 local library behavior, upgrade notes, and POCO acceptance steps are in [docs/STAGE3_REPORT.md](docs/STAGE3_REPORT.md).
