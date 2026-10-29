# Production readiness review and action plan

Reviewed 27 October 2026. This is a planning document for the current source checkout (`0.14.6`, code 40). No app code, assets, or release artifacts were changed during this review. The intended distribution path is the existing direct APK and GitHub release updater.

## Baseline and scope

- Reviewed module layout, Gradle and manifest configuration, release scripts, app wiring, backup and database code, downloads, updates, local audio, playback, Home, library statistics, and representative tests.
- `./gradlew test :app:lintDebug --offline --console=plain` completed successfully. The test reports contain 275 tests, 0 failures, and 6 skipped live-provider tests. Most Gradle tasks were already up to date. Debug lint reports **0 errors and 51 warnings**.
- No device test, release lint/build, profiler capture, or live provider test was run for this review. Passing host checks does not establish phone or release behavior.
- Existing strengths to preserve: separate model/data/playback/provider/design modules, Room migration tests, bounded backup JSON, signed APK verification, checksum and signer validation before update installation, and service-owned playback.

## Priority 0 — protect user data before refactoring

### 1. Make backup restore recoverable across Room and DataStore

**Finding:** `core/data/.../backup/BackupRepository.kt:32-67` commits all Room changes, then imports DataStore settings. A settings write failure leaves a partially applied backup even though the UI reports import failure. Room and DataStore do not share one transaction.

**Action:** Decide and document restore semantics, then add a small persisted restore journal or another recoverable two-stage flow. On restart, finish or clearly report an interrupted import. Keep import idempotent. Add a test with a failing `PortableSettingsStore`, plus a restart/retry test. Keep the existing user's data when validation fails.

**Done when:** A settings failure or process interruption cannot silently produce a half-restored library/settings state, and tests prove retry/recovery behavior.

### 2. Define collision behavior and validate backup identities

**Finding:** `BackupJson.kt:71-109` checks duplicate *track* keys but does not check duplicate playlist IDs, entry IDs, like keys, smart playlist IDs, or listening event IDs. `BackupRepository.kt:39-48` skips creation of a playlist with an existing ID but still imports its entries; DAO inserts use `IGNORE` for several records (`PlaylistDao.java:19`, `LibraryDao.java:11-12`, `HistoryDao.java:13`). This can merge into an existing playlist or silently discard conflicting imported rows. The current backup test restores into an empty target and repeats the same backup; it does not cover divergent existing playlists.

**Action:** Reject duplicate identities inside a backup. Specify merge rules for an existing playlist with the same ID but different name/entries; preserve both sets of user data and show an accurate import summary. Add focused tests for divergent playlist IDs, conflicting entry IDs across playlists, duplicate records, and re-import.

**Done when:** Imports have deterministic, documented results and never silently drop or misattribute playlist entries.

## Priority 1 — reliability and observable performance

### 3. Recover interrupted audio downloads

**Finding to verify on device:** `feature/download/AudioTransfer.kt:79-125` creates a pending MediaStore row and removes it in `finally`, but a killed process cannot execute that cleanup. `DownloadRequests.kt:35-41` keeps request state only in memory. The Android 10–13 service and Android 14+ job both use this path.

**Action:** Persist only the minimal ownership information needed to identify the app's pending rows. On startup or before a new download, reconcile stale app-owned pending rows and notifications without touching completed audio. Exercise cancellation, network loss, force-stop/process death, and app restart on both download paths.

**Done when:** Interrupted transfers leave no indefinitely hidden app-owned files, and completed downloads remain intact and visible in Local songs.

### 4. Harden updater state after interruption

**Finding:** `updates/AppUpdates.kt:20-25` initializes `ready` from the existence of two files, while `start()` only checks whether the stored manifest is already installed (`:34-42`). A corrupt or mismatched APK/manifest pair can be shown as ready until the user taps Install; download replacement writes the APK and manifest in separate steps (`:78-90`). Installation revalidates the APK, so the current identity check is a strong safety gate; the issue is stale state and recovery.

**Action:** Validate the stored pair before advertising it as ready, clear or quarantine invalid state, and stage replacement so a crash cannot leave a misleading pair. Add tests for an interrupted replacement, corrupt manifest, truncated APK, and an already-installed update. Keep package, version, checksum, signer, and minimum-SDK checks.

**Done when:** The ready banner always corresponds to a validated installable file and interruption leads to a clear retry state.

### 5. Profile and then fix scrolling costs

**Finding:** Lint flags `feature/home/HomeFeedShelf.kt:42` for reading a frequently changing carousel/list index directly in composition. `feature/library/ListeningStatisticsScreen.kt:356-391` puts every expanded recent row inside one `LazyColumn` item, so loaded pages are composed eagerly. `SungRoot.kt:34-58` collects many feature flows at the root; that may widen recomposition, but its cost needs measurement. Historical handoff notes say scrolling still felt slow after an earlier attempt.

**Action:** Capture frame time and recomposition traces on a representative phone in a release-like build. Move fast-changing caption state to a smaller composable; render expanded statistics rows as keyed lazy items; then address root subscriptions only if traces show them to be costly. Keep large-font and reduced-motion behavior.

**Done when:** Before/after traces show fewer missed frames on Home and statistics, with no navigation or accessibility regression.

### 6. Add device coverage for the direct APK path

**Finding:** Only two current files exist under `app/src/androidTest`; the host suite does not exercise real MediaStore, SAF, foreground services/jobs, notification permission, MediaSession background playback, or package installation. The release script runs host tests, release lint, assembly, signature and alignment checks, but phone acceptance remains manual.

**Action:** Keep a concise release checklist and automate a few stable device scenarios where practical: upgrade over an installed production build with library data retained; backup export/import; local folder grant and playback; download/cancel/restart; screen-off playback; update check/download/install. Cover API 26–28 legacy storage, API 29–33 foreground-service downloads, and API 34+ user-initiated jobs on representative devices/emulators.

**Done when:** Every release records the tested version, device/API, outcomes, and remaining exceptions before publishing its GitHub assets.

## Priority 2 — maintainability and repeatable releases

### 7. Move reusable local data code into `core:data`

**Finding:** `feature/local/LocalAudioRepository.kt`, `LocalArtworkRepository.kt`, and `feature/lyrics/LocalLyricsReader.kt` contain scanning, persistence, cache, and file parsing work in the app feature layer. This conflicts with `AGENTS.md`'s placement rule for reusable data formats and repositories. `LocalAudioRepository.kt` also combines folder preferences, scanning, deduplication, metadata, and artwork refresh in one 271-line class.

**Action:** Move reusable repository/parsing/cache pieces into small, descriptive `core:data` files, leaving permission prompts and Compose screens in their feature packages. Preserve URI and folder permission behavior. Add migration/restoration tests only where storage behavior changes.

**Done when:** UI packages own UI and Android entry points, while reusable data logic has focused tests and a stable interface.

### 8. Remove duplicated download orchestration

**Finding:** `AudioDownloadService.kt:55-87` and `AudioDownloadJob.kt:32-55` repeat transfer setup, progress, success/failure notification, cache invalidation, and finish handling. They have distinct Android lifecycle duties, so wholesale merging would obscure those differences.

**Action:** Extract one small feature-level transfer runner for the shared work. Keep service foreground management and job callbacks separate. Also centralize repeated local image extension lists in `LocalAudioRepository.kt`.

**Done when:** One change to transfer success/failure behavior applies to both Android paths and lifecycle tests still cover each path.

### 9. Reduce app wiring and screen complexity incrementally

**Finding:** `SungRoot.kt` wires many feature states/callbacks into `SungApp.kt`, which has a long parameter list and owns destination/overlay decisions. `ListeningStatisticsScreen.kt` is 424 lines and includes chart, ranking, recent-list, and formatting code.

**Action:** Introduce small feature route hosts and explicit action/state models where they simplify call sites. Split statistics sections and pure formatting into focused files. Avoid a broad navigation rewrite; preserve saved overlay state and back behavior.

**Done when:** Each changed file has a clear responsibility and current UI/navigation tests still pass.

### 10. Put checks in CI and triage lint deliberately

**Finding:** There is no checked-in `.github/workflows` pipeline. The current debug lint report has 51 warnings: one performance warning, a context-lifetime warning in `AppUpdates.kt:133`, unused resources/strings, API annotation advice, icon warnings, and dependency-version notices. These have different risk levels.

**Action:** Add CI for host tests and debug lint on each change; use a release gate for release lint and build verification without storing signing secrets in CI. Triage warnings by actual impact. Check runtime/XML references before removing resources. Review dependency upgrades one family at a time with the same tests and a device smoke check; do not upgrade solely because lint lists a newer version.

**Done when:** Mainline changes have repeatable green checks, high-impact lint items are resolved or documented, and release packaging remains reproducible locally.

### 11. Check accessibility, text, and backup policy

**Finding:** Feature UI contains many inline user-facing strings while lint also reports unused string resources. This makes consistent wording and future localization harder to manage. The manifest sets `allowBackup="false"`, matching the app's explicit JSON export, but `backup_rules.xml` and `data_extraction_rules.xml` still contain starter-template comments; the policy should be clear to maintainers.

**Action:** Audit the main flows with TalkBack, large text, reduced motion, narrow screens, and light/dark themes. Move reusable user-facing text into resources as screens are touched; remove confirmed unused strings. Document the intended Android backup/transfer policy and remove or replace template XML only after verifying manifest behavior. Preserve the privacy-first diagnostic log and its opt-in attachment flow.

**Done when:** Core controls have clear spoken labels and focus order, text remains usable at large font sizes, and backup behavior is explicit in code and docs.

### 12. Clean historical artifacts after verification

**Finding:** `Stages/` holds 317 tracked files, including 32 APKs totaling about 570 MB. `deferred/stage7/source_snapshot/` duplicates older source. Lint lists potentially unused strings and launcher resources. `release-assets/` is ignored and contains locally prepared releases; it is not part of the tracked cleanup count. `PROJECT_HANDOFF.md` starts with code 31 history while current source is code 40, so it is easy to mistake past status for current status.

**Action:** First inventory which APKs, reports, schema fixtures, screenshots, and snapshots are needed for audit, migration tests, or release recovery. Keep current release evidence and historical notes accessible, then remove redundant tracked binaries/snapshots from the working tree in a separate cleanup change. Update the handoff/index to point to current source version and archived history. Do not rewrite Git history or delete local release assets as part of routine cleanup.

**Done when:** The repository is smaller and easier to navigate, with no loss of reproducibility, migration fixtures, or release provenance.

## Proposed execution order

1. Add failing backup restore/collision tests; implement and verify the data-safe restore behavior.
2. Exercise and fix interrupted download and updater recovery on device/emulator.
3. Record performance baseline, fix measured scroll hotspots, and compare traces.
4. Extract reusable data and shared download work in small, testable changes.
5. Add CI and direct-APK release gates, then triage lint and dependency updates.
6. Inventory and remove confirmed redundant artifacts/resources last.

For each implementation slice: keep the existing production application ID and signer, retain Room migration paths and user data, run focused tests plus the full host/lint gate, and record phone results for behavior the host cannot model. Raise `versionCode` above code 40 before distributing a new production APK; confirm against every APK actually distributed.
