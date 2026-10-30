# Production readiness review and action plan

Initial review: 27 October 2026, source version `0.14.6` (code 40). That review changed no app code, assets, or release artifacts. The intended distribution path is the existing direct APK and GitHub release updater.

## Implementation progress (30 October 2026)

- **Completed:** backup restore recovery journal, retry recovery, identity validation, and deterministic playlist collision merging (items 1–2).
- **Completed:** updater startup validation and staged replacement; invalid, incomplete, already-installed, and abandoned staged update files are cleared before any update is shown as ready (item 4).
- **Reclassified:** interrupted pending audio rows already have an Android MediaStore expiration policy; this is not an indefinite-storage bug. Device checks remain on the release checklist (item 3).
- **Verified:** `./gradlew test :app:lintDebug :app:lintRelease --offline --console=plain` succeeds: 281 host tests, 0 failures, 6 skipped live-provider checks, and 0 lint errors. After the lint fixes below, app lint reports 39 debug warnings and 45 release warnings. This checks release lint only; no APK was assembled or signed.
- **Added:** GitHub Actions workflow runs host tests and debug lint on pull requests and pushes to `main` (item 10). It does not access signing material or publish APKs.
- **Organized:** local audio scanning/artwork and local lyric file parsing now live in `core:data`; the scanner, audio models, lyric reader, and lyric parser have separate responsibilities and focused tests (item 7). Shared download outcome handling is extracted (item 8), and listening-statistics formatting has its own file (item 9).
- **Current source (30 October):** `0.14.8`, code 42. This is source state only; it does not claim a signed build, production install, or publication. The installed production app remains code 41. The 27 October review baseline below remains a historical snapshot.
- **Release build (31 October):** `0.14.9`, code 43. The production build script completed tests, release lint, assembly, signing and alignment verification, then generated local release assets. It has not been installed or published.
- **Device identified:** POCO M6 Pro 5G (`23076PC4BI`), Android 15 / API 35. The installed production app remains `0.14.7`, code 41; its signer matches the existing local release APK. No production install or app-data change was made.
- **Device test blocked:** connected instrumentation could not install the separate `.stage0` debug app (`INSTALL_FAILED_USER_RESTRICTED`). Its report contains zero tests, although Gradle returned success. Enable/approve USB app installation on the phone before retrying. `adb` is available at `/home/karthi/Android/Sdk/platform-tools/adb` but is not on `PATH`.

## Baseline and scope

- Reviewed module layout, Gradle and manifest configuration, release scripts, app wiring, backup and database code, downloads, updates, local audio, playback, Home, library statistics, and representative tests.
- At the 27 October baseline, `./gradlew test :app:lintDebug --offline --console=plain` completed successfully. The reports contained 275 tests, 0 failures, and 6 skipped live-provider tests. Most Gradle tasks were already up to date. Debug lint reported **0 errors and 51 warnings**.
- At that baseline, no device test, release lint/build, profiler capture, or live provider test was run. Passing host checks does not establish phone or release behavior.
- Existing strengths to preserve: separate model/data/playback/provider/design modules, Room migration tests, bounded backup JSON, signed APK verification, checksum and signer validation before update installation, and service-owned playback.

## Priority 0 — protect user data before refactoring

### 1. Make backup restore recoverable across Room and DataStore — complete

**Finding (resolved):** The original restore wrote Room before DataStore without a recovery record, so a settings write failure could leave a partial restore.

**Action — completed 30 October:** Added a Room restore journal that commits with imported library rows, then replays portable settings on startup or before the next export/import. Replaying settings is safe, and validation still completes before import writes.

**Done when:** A settings failure or process interruption cannot silently produce a half-restored library/settings state. Covered by failing-store recovery, retry, and migration tests.

### 2. Define collision behavior and validate backup identities — complete

**Finding (resolved):** Backup validation now rejects duplicate imported identities. Same-ID playlists previously could silently merge entries into existing playlists without a clear collision rule.

**Action — completed 30 October:** Defined merge behavior: keep the existing playlist name and entries, append missing imported entries in backup order, and deterministically remap conflicting entry IDs. Imports remain idempotent. Backup summaries describe the imported file contents and state that existing items were kept.

**Done when:** Imports have deterministic results and never silently drop or misattribute playlist entries. Covered by duplicate validation, divergent playlist merge, ID collision, and re-import tests.

## Priority 1 — reliability and observable performance

### 3. Verify interrupted audio download cleanup

**Finding:** `feature/download/AudioTransfer.kt:79-125` creates a pending MediaStore row and removes it in `finally`; a killed process cannot execute that cleanup. Android MediaStore expires pending rows using `DATE_EXPIRES` (the documented default is typically seven days), so these rows do not remain hidden indefinitely. `DownloadRequests.kt:35-41` keeps request state only in memory, and the Android 10–13 service and Android 14+ job both use this transfer path. See [MediaStore.MediaColumns](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns).

**Action:** Keep the current `finally` cleanup and verify MediaStore expiry on supported Android versions. Exercise cancellation, network loss, force-stop/process death, and app restart on both download paths. Add app-owned reconciliation only if device testing shows rows persist beyond the platform expiry; never touch completed audio.

**Done when:** Device checks confirm interrupted pending rows expire or are removed and completed downloads remain intact and visible in Local songs.

### 4. Harden updater state after interruption

**Finding:** `updates/AppUpdates.kt` previously initialized `ready` from two files without validating them. A corrupt or mismatched APK/manifest pair could be shown as ready until the user tapped Install; replacement also wrote the APK and manifest in separate steps. Installation already revalidated package, version, checksum, signer, and minimum SDK.

**Action — completed 30 October:** Validate saved files on startup under the updater mutex before advertising readiness. Delete incomplete or invalid files and leftover staging files; treat an already-installed version as stale. Download and verify to a staging file, sync the staged manifest, then atomically replace the APK and commit manifest. Installation still revalidates the pair.

**Done when:** The ready banner always corresponds to a validated installable file and interruption leads to a clear retry state. Focused updater tests cover malformed metadata, truncated APKs, abandoned staging files, and already-installed updates.

### 5. Profile and then fix scrolling costs

**Finding:** Lint flags `feature/home/HomeFeedShelf.kt:42` for reading a frequently changing carousel/list index directly in composition. `feature/library/ListeningStatisticsScreen.kt:356-391` puts every expanded recent row inside one `LazyColumn` item, so loaded pages are composed eagerly. `SungRoot.kt:34-58` collects many feature flows at the root; that may widen recomposition, but its cost needs measurement. Historical handoff notes say scrolling still felt slow after an earlier attempt.

**Action:** The frequent Home carousel index is now observed by a small caption composable instead of the whole shelf; the empty-shelf path is also safe. Capture frame time and recomposition traces on a representative phone in a release-like build. Render expanded statistics rows as keyed lazy items only after checking the current cost and preserving large-font behavior; address root subscriptions only if traces show them to be costly. Keep reduced-motion behavior.

**Done when:** Before/after traces show fewer missed frames on Home and statistics, with no navigation or accessibility regression.

### 6. Add device coverage for the direct APK path

**Finding:** Only two current files exist under `app/src/androidTest`; the host suite does not exercise real MediaStore, SAF, foreground services/jobs, notification permission, MediaSession background playback, or package installation. The release script runs host tests, release lint, assembly, signature and alignment checks, but phone acceptance remains manual.

**Device status (30 October):** A POCO M6 Pro 5G on Android 15 / API 35 is connected. The instrumentation runner's separate debug app was blocked by `INSTALL_FAILED_USER_RESTRICTED`; zero app instrumentation tests ran. The installed production app remains at code 41 and its signer matches the existing local release APK.

**Action:** Keep a concise release checklist and automate a few stable device scenarios where practical: upgrade over an installed production build with library data retained; backup export/import; local folder grant and playback; download/cancel/restart; screen-off playback; update check/download/install. First enable/approve USB app installation and rerun the debug UI tests. Then check the listed production flows on API 35; cover API 26–28 legacy storage and API 29–33 foreground-service downloads on additional representative devices/emulators.

**Done when:** Every release records the tested version, device/API, outcomes, and remaining exceptions before publishing its GitHub assets.

## Priority 2 — maintainability and repeatable releases

### 7. Move reusable local data code into `core:data`

**Finding:** `LocalAudioRepository.kt`, `LocalArtworkRepository.kt`, and `LocalLyricsReader.kt` previously placed scanning, cache, and local file parsing in the app feature layer. The large repository also combined scanning and repository state.

**Action — completed 30 October:** Moved local audio repositories and models under `core:data/local`, split MediaStore/SAF scanning into `LocalAudioScanner`, and separated local lyric reading from format parsing under `core:data/lyrics`. Compose screens and permission prompts remain under `app/feature`. Folder grants and content URIs retain their existing behavior; focused artwork and lyric parser tests now run with the data module.

**Done when:** UI packages own UI and Android entry points, while reusable data logic has focused tests and a stable interface.

### 8. Remove duplicated download orchestration

**Finding:** `AudioDownloadService.kt:55-87` and `AudioDownloadJob.kt:32-55` repeat transfer setup, progress, success/failure notification, cache invalidation, and finish handling. They have distinct Android lifecycle duties, so wholesale merging would obscure those differences.

**Action — completed 30 October:** Extracted `AudioDownloadRunner` for shared progress, success/failure notifications, logging, and local-library invalidation. The service keeps foreground notification management and the job keeps its system callbacks. Centralized image extensions in local audio models.

**Done when:** One change to transfer success/failure behavior applies to both Android paths. Android lifecycle cancellation and process-death coverage remains in item 6's device checklist.

### 9. Reduce app wiring and screen complexity incrementally

**Finding:** `SungRoot.kt` wires many feature states/callbacks into `SungApp.kt`, which has a long parameter list and owns destination/overlay decisions. `ListeningStatisticsScreen.kt` is 424 lines and includes chart, ranking, recent-list, and formatting code.

**Action:** Pure statistics formatting now lives in `ListeningStatisticsFormatting.kt`. Introduce small feature route hosts and explicit action/state models where they simplify call sites; split the remaining statistics sections incrementally. Avoid a broad navigation rewrite; preserve saved overlay state and back behavior.

**Done when:** Each changed file has a clear responsibility and current UI/navigation tests still pass.

### 10. Put checks in CI and triage lint deliberately

**Finding:** A checked-in `.github/workflows/android-checks.yml` now runs host tests and debug lint. Debug lint reports 39 warnings, mostly unused resources/strings, dependency-version notices, KTX suggestions, and icon guidance. These have different risk levels.

**Action:** CI for host tests and debug lint is in place. Debug and release lint both complete locally with zero errors; the app reports 39 debug warnings and 45 release warnings. The updater context warning and guarded MediaStore API warning were resolved, along with the Home carousel recomposition warning. Keep the separate local signed APK verification gate without storing signing secrets in CI. Check runtime/XML references before removing resources. Review dependency upgrades one family at a time with the same tests and a device smoke check; do not upgrade solely because lint lists a newer version. The workflow has not yet run on GitHub.

**Done when:** Mainline changes have repeatable green checks, high-impact lint items are resolved or documented, and release packaging remains reproducible locally.

### 11. Check accessibility, text, and backup policy

**Finding:** Feature UI contains many inline user-facing strings while lint also reports unused string resources. This makes consistent wording and future localization harder to manage. The backup policy was implicit in `allowBackup="false"` and obscured by unused starter-template XML; the rules now explicitly exclude app data from cloud backup and device transfer.

**Action:** `android:allowBackup="false"` opts out of Android Auto Backup. Because some Android 12+ manufacturers may still allow device-to-device transfer, the XML rules also exclude all app data from both cloud backup and device transfer. Users can move selected portable data through the validated in-app JSON export/restore flow. Replaced the starter-template XML rules on 30 October. See [Android's backup behavior documentation](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore). Audit the main flows with TalkBack, large text, reduced motion, narrow screens, and light/dark themes. Move reusable user-facing text into resources as screens are touched; remove confirmed unused strings. Preserve the privacy-first diagnostic log and its opt-in attachment flow.

**Done when:** Backup behavior is explicit in code and docs. Core controls have clear spoken labels and focus order, and text remains usable at large font sizes. The manual accessibility audit remains open.

### 12. Clean historical artifacts after verification

**Finding:** The owner removed the historical `Stages/` archive and deferred Stage 7 source snapshot on 30 October 2026. Before removal, they contained 317 tracked files, including 32 APKs totaling about 570 MB, and 40 older source files. Their former artifact references remain in dated reports as historical records. Lint lists potentially unused strings and launcher resources. `release-assets/` is ignored and was not removed. The handoff now identifies the current source version before its historical entries.

**Action:** Removed `Stages/` and `deferred/` at the owner's direction. The old binary, screenshot, test-report, and prototype-source artifacts are no longer in the working tree; their contents remain recoverable only from Git history if present in a commit. No Git history rewrite or local release-asset deletion was performed. Added a current-state handoff at the top of `PROJECT_HANDOFF.md`.

**Done when:** The unneeded folders are removed and the current handoff distinguishes active source status from historical notes. Historical evidence is no longer kept in the working tree by owner choice.

## Proposed execution order

1. ~~Add failing backup restore/collision tests; implement and verify the data-safe restore behavior.~~
2. ~~Harden saved updater recovery and verify focused startup cases.~~ Exercise interrupted download cleanup on device/emulator.
3. Record a performance baseline, fix measured scroll hotspots, and compare traces.
4. ~~Move local repositories/parsers and extract shared download work.~~ Split remaining screen sections as needed.
5. ~~Add CI for host tests and debug lint.~~ Keep the direct-APK release gate; triage lint and dependency updates.
6. Inventory and remove confirmed redundant artifacts/resources last.

For each implementation slice: keep the existing production application ID and signer, retain Room migration paths and user data, run focused tests plus the full host/lint gate, and record phone results for behavior the host cannot model. Raise `versionCode` above code 41 before distributing a new production APK; confirm against every APK actually distributed.
