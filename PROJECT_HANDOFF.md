# SungMobile project handoff

This file is the short, current entry point for continuing work. See [`docs/README.md`](docs/README.md) for the documentation map and [`docs/PRODUCTION_READINESS_REVIEW.md`](docs/PRODUCTION_READINESS_REVIEW.md) for open reliability and release work.

## Current source snapshot — 31 October 2026

- Source version: **0.14.9**, version code **43** (`app/build.gradle.kts`).
- Installed production version: **0.14.7**, version code **41**. A signed and verified APK for 0.14.9 is prepared locally; it has not been installed or published.
- Production application ID: `com.karthi.sungmobile`.
- Debug application ID: `com.karthi.sungmobile.stage0`. Debug and production installs have separate app data.
- License: GPL-3.0-or-later. See [`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

The working tree contains in-progress changes. Inspect `git status` before editing so existing work is not overwritten. The `Stages/` evidence archive and deferred Stage 7 prototype were removed from the working tree on 30 October 2026 at the owner's direction. Old milestone reports remain in `docs/`; their deleted artifact references are historical.

## Product and architecture

SungMobile is an Android music player with YouTube discovery and playback, local audio, downloads, playlists, listening history, lyrics, and settings. The Android app is the maintained project. The Linux desktop documentation is retained as reference material under [`docs/reference/sung-linux/`](docs/reference/sung-linux/), not as an Android implementation specification.

The modules separate provider-neutral models, YouTube access, persistence, playback, shared Compose design, and app features. The app is the composition root; the playback service owns the live player. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) before moving code between modules.


## Set up and build

Follow [`README.md`](README.md) for SDK setup and build commands, and [`CONTRIBUTE.md`](CONTRIBUTE.md) for coding and review expectations. Android SDK platform 37 and Build Tools 36.0.0 are used by the current CI workflow. Keep `local.properties` and signing files outside version control.

The production signing configuration is expected at `~/.android/sungmobile-release/signing.properties`. Keep the properties file and private key backed up securely and out of the repository. Production releases must preserve the production app ID and signing key, and use a version code greater than every production APK already distributed. See [`docs/updates/VERSIONING_AND_RELEASES.md`](docs/updates/VERSIONING_AND_RELEASES.md).

## Open work

The current review tracks device checks for interrupted downloads and release flows, performance profiling, an accessibility audit, lint-warning cleanup, and a gradual split of complex app wiring. The previous instrumentation install was blocked with `INSTALL_FAILED_USER_RESTRICTED`; no phone acceptance should be inferred from host tests or lint. Use the review document for the latest detailed status rather than treating old stage plans as current.

## Suggested reading order

1. [`README.md`](README.md) — app, setup, and modules.
2. [`CONTRIBUTE.md`](CONTRIBUTE.md) — local workflow and change expectations.
3. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — dependency direction and runtime ownership.
4. [`docs/PRODUCTION_READINESS_REVIEW.md`](docs/PRODUCTION_READINESS_REVIEW.md) — current open work and known limits.
5. [`docs/PLAYBACK_GUIDE.md`](docs/PLAYBACK_GUIDE.md) — playback ownership and behavior.
