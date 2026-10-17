# SungMobile

**Restoring after an OS change or continuing in a new session? Start with [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md)** for progress, backup requirements, environment setup and the exact next steps.

A native Kotlin / Jetpack Compose Android music app, developed in functional milestones from the Sung Linux reference. **Stage 4 adds responsive search, shared YouTube links, collection actions and real local Home sections.** The user reported every Stage 3 POCO checklist item working on 13 October 2026.

The current build is **0.5.0-stage4**. It uses the M3 Expressive foundation with simple functional controls. Stage 4 host verification is complete; manual phone acceptance is pending. See the [Stage 4 report](docs/STAGE4_REPORT.md) and [phone checklist](Stages/Stage4_Results/PHONE_TEST_CHECKLIST.md).

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

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Its application ID is `com.karthi.sungmobile.stage0`, retained so later stages upgrade your existing installation; it is separate from the future production app. Minimum API is 26; the tested physical device is Android 15 / API 35. Older supported APIs still need device coverage.

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

It also provides persistent likes, local playlists, pins, bulk library actions, listening/search history and privacy controls. Stage 4 adds debounced search, validated shared/pasted links, paged collection actions with explicit partial-load choices, and privacy-aware local mixes. Downloads, lyrics and complete artist/discovery support follow in the functional plan. Account sign-in is outside the current local-library scope.

SungMobile is licensed under **GPL-3.0-or-later**, matching the selected NewPipeExtractor dependency. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md). This development APK is not a public release. The release stage must package the corresponding source and complete dependency/asset notices before public distribution.


Stage 3 local library behavior, upgrade notes, and POCO acceptance steps are in [docs/STAGE3_REPORT.md](docs/STAGE3_REPORT.md).
