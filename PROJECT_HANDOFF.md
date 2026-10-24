# SungMobile — project handoff and OS migration recovery

**24 October 2026 local audio production APK:** `0.12.8`, code 28, is signed and optimized for `com.karthi.sungmobile` with the same certificate as code 27. The player button morphs from rounded square to flower when play changes to pause. Active lyrics use per-word Google Sans Flex variations and the theme accent without adding a font asset. Library has Local songs: MediaStore scans `Music/SungMobile/`, Settings can add persisted SAF folders, and Media3 plays their content URIs. Online song menus offer notification-backed downloads to `Music/SungMobile/` on API 29+; API 26–28 retains local playback. Host tests, release lint, package and signer checks passed. Phone verification and publication are pending. See `Stages/Local_Audio_Results/README.md` and `docs/local/README.md`. Next production code must exceed 28.

**24 October 2026 lyric emphasis and player control APK:** `0.12.7`, code 27, removes the filled Lyrics active-line capsule and side bar, applies theme-derived two-tone text emphasis, and fixes duplicate source credit. Now Playing and the Queue peek reuse the mini-player's expressive play/pause button. No floating lyric timestamp or word-level synchronization was added. Optimized production APK, 91 host tests, debug/release lint, package and signer verification passed. See `Stages/Lyrics_Emphasis_Results/README.md`. No phone installation or GitHub publication was performed; phone acceptance remains with the owner. Next production code must exceed 27.

**24 October 2026 complete expressive UI APK:** `0.12.6`, code 26, packages all five expressive UI slices as an optimized, signed production APK. Package `com.karthi.sungmobile` and signer match code 25, allowing an in-place production update. Release lint and APK checks passed; no phone installation or GitHub publication was performed. See `Stages/Expressive_UI_Results/README.md` and its manual checklist. Next production code must exceed 26.

**24 October 2026 source-only Lyrics/settings/loading update:** The final expressive UI slice is implemented in source. Lyrics uses the existing theme font more deliberately, keeps the active line in the reading area, compacts timing controls, and retains attribution, follow, offsets, plain text and tap-to-seek. Indeterminate loading now uses the shared motion-aware Material 3 indicator; settings switches and checked menus share consistent components. The owner's reference timestamp (`0:28`) was not added. See `docs/ui/expressive-refresh/SLICE5_REPORT.md`. Host verification passed; no APK was built or phone tested. The code-25 APK still contains only slices 1–2; any new production build must use code above 25.

**24 October 2026 source-only expressive Home/Search update:** Slices 3 and 4 of `docs/ui/expressive-refresh/PLAN.md` are implemented in source, including the owner's rounded-square Search-control correction. The Home shelves use current local data; Search keeps its four catalog kinds and existing behavior. Focused host tests and lint passed. See `docs/ui/expressive-refresh/SLICE3_REPORT.md` and `SLICE4_REPORT.md`. No new APK was built; the code-25 production APK below contains only slices 1–2. Phone acceptance for these source edits is pending, and the next production APK must use a code above 25.

**24 October 2026 expressive Library APK:** `0.12.5`, code 25, is an optimized production APK with the same `com.karthi.sungmobile` package and certificate as code 24. It includes the shared mini-player/nav refresh and Library tabs/Mixes/Settings history UI. Build and verification evidence plus manual phone checks are in `Stages/Expressive_Library_Results/README.md`. No phone installation or GitHub publication was performed in this task; do not infer phone acceptance. The next production update must use a code above 25.

**24 October 2026 player and queue UI update:** `0.12.4`, code 24, follows the production code 23 package/signing identity (`com.karthi.sungmobile`). The player now uses compact seek steps, a slimmer wave, expressive transport controls and a persistent swipe-up Queue sheet. Queue occurrence actions and all playback settings remain. Host tests passed; release build/lint evidence and pending phone checks are in `Stages/Player_Queue_UI_Results/README.md`. The `.stage0` debug app remains separate. No GitHub release has been published.

**23 October 2026 production release candidate:** The owner chose a production package instead of `.stage0`. `0.12.3`, code 23, builds as optimized, signed `com.karthi.sungmobile` with a dedicated local production key. It is a separate installation from the debug app and does not migrate its Likes/playlists/settings. See `Stages/Production_Release_Results/README.md` and `docs/updates/VERSIONING_AND_RELEASES.md`. The key and signing properties are outside the repository in `~/.android/sungmobile-release/`; both require secure backup for future updates. No GitHub release or phone acceptance yet. The debug line remains available under its existing `.stage0` ID and signer.

**23 October 2026 launcher icon:** `0.12.3-icon`, code 23, uses the intact source logo with a 12 dp adaptive foreground inset for normal/round launcher masks. Invalid color-bitmap monochrome reference removed. Same app ID and retained signer; no database change. See `docs/ui/icon/REPORT.md` and `Stages/Icon_Results/README.md`. Phone visual acceptance pending. Scrolling performance investigation started but made no code changes before this icon request; it remains outstanding.

**22 October 2026 Lyrics/settings UI:** Implemented `0.12.2-lyrics-ui`, code 22, following the owner’s light Lyrics reference while retaining Sung colors. Real track artwork/source card, compact timing controls, highlighted active line, follow action and plain-lyrics reading surface; grouped settings and shared confirmations. Same app ID, signer and Room schema 5. See `docs/ui/lyrics/REPORT.md` and `Stages/Lyrics_UI_Results/README.md`. Manual phone acceptance pending; scrolling profiling remains separate.

**22 October 2026 Search UI:** Implemented `0.12.1-search-ui`, code 21, above the updater code 20. Search now uses a rounded editable field, icon category chips, a clear action, styled recent searches, an empty state and shared heart/plus song rows. Search/ViewModel functions, app ID, retained signer and Room schema 5 remain. See `docs/ui/search/REPORT.md` and `Stages/Search_UI_Results/README.md`. Manual phone acceptance pending. Lyrics/settings/dialogs UI and scrolling profiling remain later work.

**22 October 2026 updater:** Implemented `0.12.0-updates`, code 20, with default-on unmetered background checks/downloads, a Settings toggle, manual checks and user-confirmed Android installation. Uses this repository’s stable latest GitHub release plus `update.json`; rejects lower/equal codes, wrong packages, mismatched checksums/signers and unsupported Android versions. Same application ID, retained signing key and Room schema 5. See `docs/updates/REPORT.md`, `docs/updates/VERSIONING_AND_RELEASES.md` and `Stages/Updater_Results/README.md`. Manual phone acceptance pending; no release published. The next distributed build must use code 21 or higher. No visual/device tests were performed for this task, per owner request.

**21 October 2026 Library UI update:** Owner accepted player UI code 18 and requested Library before Search. Delivered `0.11.2-library-ui`, code 19: category pills including direct Sessions, filter/sort, explicit selection mode, shared heart/options song rows, playlist cards and matching collection rows. History/privacy, smart playlists, statistics, cleanup/versions and all bulk/playlist actions remain. Same app ID, signing key and Room schema 5. 72 app tests passed; lint zero errors; build/signature verification passed. Phone acceptance pending. See `docs/ui/library/REPORT.md` and `Stages/Library_UI_Results/PHONE_TEST_CHECKLIST.md`. Search redesign remains upcoming; scrolling profiling remains deferred.

**21 October 2026 Player UI update:** Implemented the approved Now Playing + More + Queue slice as `0.11.1-player-ui`, code 18. Actual neighboring queue artwork is tappable by occurrence ID; the seek bar has foreground-only wave motion with Reduced Motion support. Advanced controls and Saved sessions/Stop are in More; Queue/Lyrics/Speed/timer/countdown remain directly reachable. Existing theme, app ID, signing certificate and Room schema 5 retained. 67 host app tests, lint and build passed. Manual phone acceptance pending; see `docs/ui/player/REPORT.md` and `Stages/Player_UI_Results/PHONE_TEST_CHECKLIST.md`. Search, Library/collections and remaining surfaces are later UI slices. Owner reports code 17 scrolling did NOT improve and deferred profiling; optimized-build causation is unconfirmed.

**19 October 2026 Home UI update:** The owner authorized page-by-page UI improvements and supplied a Home reference. The current UI build is `0.11.0-home-ui`, code 17, retaining the app ID, debug certificate and Room schema 5. Home uses the existing theme colors, artwork mix cards, heart/plus song actions, and pinned/Liked sections below Recent. The mini-player/navigation remain shared components. Playback clock updates were isolated and queue remapping removed from position ticks to reduce avoidable scrolling work. See `docs/ui/home/PLAN.md`, `docs/ui/home/REPORT.md` and `Stages/Home_UI_Results/PHONE_TEST_CHECKLIST.md`. Phone visual/scroll acceptance is pending; the owner has not provided an itemized code 16 phone result. Await the owner’s next page reference before changing other page layouts.

**19 October 2026 stabilization update:** The owner reported all Stage 9 features working on the POCO and skipped Stage 10. The owner authorized a focused whole-app stabilization pass before UI improvements. The current build is `0.10.1-stabilization`, code 16, with the same app ID/debug certificate and Room schema 5. See `docs/STABILIZATION_REPORT.md` and `Stages/Stabilization_Results/PHONE_TEST_CHECKLIST.md` for fixes, host evidence and the pending manual regression check. Final post-UI regression and public-release packaging remain later gates. The historical pending Stage 9 statements below are superseded by this update.

**19 October 2026 Stage 9 delivery:** Stage 9 has now been implemented as `0.10.0-stage9`, code 15. It adds saved smart playlists, review-based playlist cleanup, local playlist versions/restore, and listening statistics. Room schema 5 migrates existing data; app ID and debug certificate are unchanged. Host tests, lint and APK build passed. The owner has **not yet tested this APK on the POCO**, so Stage 9 phone acceptance remains pending. See `docs/STAGE9_REPORT.md` and `Stages/Stage9_Results/PHONE_TEST_CHECKLIST.md`. App-data backup/export/import and new playlist-link exchange remain excluded; Stage 7 remains deferred. The historical Stage 9 planning and Stage 4 handoff statements below are superseded by this update.

**18 October 2026 Stage 9 planning:** The owner reported Stage 8 working on the POCO and requested a Stage 9 plan. `docs/STAGE9_PLAN.md` proposes saved smart playlists, playlist cleanup and version history, and listening statistics. The owner explicitly skipped app-data backup/import/export and new playlist-link exchange for this stage. Existing YouTube playlist link opening remains. **Stage 9 implementation has not started.**

**18 October 2026 Stage 8 delivery:** The owner confirmed the online-only code 13 update works on the POCO, then authorized Stage 8. `0.9.0-stage8`, code 14, kept `com.karthi.sungmobile.stage0`, the accepted debug certificate and Room schema 4. It prepares one next-song signed URL near the end of playback, adds optional smart shuffle, per-song attenuation and provider-supplied related autoplay. No audio prebuffer or download is added. Crossfade, automatic normalization and a gapless guarantee did not pass the feasibility gates; see `docs/STAGE8_REPORT.md` and `Stages/Stage8_Results/PHONE_TEST_CHECKLIST.md`. The owner subsequently reported the delivered Stage 8 functions working on the POCO. Stage 7 remains deferred outside build source sets.

**18 October 2026 online-only decision:** After Stage 7 was built, the owner decided device-storage music and downloading add too much complexity and asked to skip them for now. The online-only `0.8.1-online` build, code 13, disconnected Stage 7 code, permissions and WorkManager; the source is preserved outside build source sets in `deferred/stage7/`. The code 12 APK is historical and **Stage 7 was not phone-accepted**. Code 13 kept the same `com.karthi.sungmobile.stage0` app ID and debug certificate. Room schema 4 supports updates from accepted code 11 and optional code 12 without deleting Stage 7 tables. YouTube remains the only music source in the active app. The owner subsequently confirmed code 13 worked on the POCO.

**18 October 2026 Stage 6 acceptance:** The owner confirmed the `0.7.1-stage6` countdown fix works on the POCO and said all Stage 6 functions are done. Stage 6, code 11, is accepted. See `docs/STAGE6_REPORT.md` and the functional completion plan. The application ID remains `com.karthi.sungmobile.stage0` with the retained debug signing certificate. Historical statements below about earlier stages not starting are superseded by the Stage 7 update above.

**Historical Stage 6 planning snapshot, earlier on 18 October 2026:** The owner reported the updated Stage 5 app working fine on the POCO and accepted Stage 5. At that point Stage 6 had not started; its proposed playback-convenience scope and defaults were in `docs/STAGE6_PLAN.md` for review. The latest accepted APK was `Stages/Stage5_Results/Sung-0.6.1-stage5-debug.apk`, code 9, with application ID `com.karthi.sungmobile.stage0` and the retained debug signing certificate. The implementation update above is the current Stage 6 status.

**18 October 2026 Stage 5 update:** The owner accepted Stage 4 after the mini-player fix. Stage 5 lyrics and essential settings are implemented as `0.6.0-stage5`, code 8, with `com.karthi.sungmobile.stage0` and the new debug signing key. Host checks and a direct YouTube lyrics probe passed; phone acceptance is pending. See `docs/STAGE5_REPORT.md` and `Stages/Stage5_Results/PHONE_TEST_CHECKLIST.md`. Do not read the historical “Stage 5 not started” statements below as the current status.

**18 October 2026 OS addendum:** Android Studio/JBR 25 and SDK API 37/Build Tools 36.0.0 were restored. The old debug key was lost; a new `~/.android/debug.keystore` was generated during a successful build. Its certificate SHA-256 is `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`. The owner agreed that a fresh installation and loss of old local data are acceptable. A reported Stage 4 bug hid the mini-player on album/playlist detail pages; it was fixed in code 7. Preserve the new key for all future updates. Historical code 6 details below remain a record of the original delivery.

**Updated: 17 October 2026.** This is a durable handoff for the owner or a new coding assistant. It records the project files inspected and the conversation decisions available on this date. It is not a claim that a backup has already been made.

## 1. Read this first: exactly where we stopped

**Stage 4 has been implemented and verified on the development computer. The next outstanding step is the owner's manual Stage 4 test on the POCO phone. No Stage 4 phone acceptance has been reported in the available conversation. Stage 5 implementation has not started.**

- Latest delivered build: **0.5.0-stage4**, version code **6**.
- Update APK: [Stages/Stage4_Results/Sung-0.5.0-stage4-debug.apk](Stages/Stage4_Results/Sung-0.5.0-stage4-debug.apk).
- Phone checklist: [Stages/Stage4_Results/PHONE_TEST_CHECKLIST.md](Stages/Stage4_Results/PHONE_TEST_CHECKLIST.md).
- Latest results: **124 host tests passed**; five live tests skipped in that host suite and **all five passed in a separate live YouTube run**. Lint: **zero errors, 13 dependency-version notices**. These checks were recorded on **14 October 2026**, not rerun for this documentation update.
- Stage 3 is the last milestone the owner explicitly accepted after completing the phone checklist.
- After restoring the development environment, get the Stage 4 phone results, fix any reported regressions, then **plan Stage 5: Lyrics and essential settings**. Follow the established plan-review-implementation-test workflow.

**An MD file cannot recover lost source code or a lost signing key. Copy the full project, Linux reference and signing key to storage outside the OS disk before reinstalling.** Details follow below.

## 2. What we are building and what the owner decided

SungMobile is a native Android music application based on the behavior of the original Sung Linux project. SungMobile is the implementation target; Sung is reference code/documentation and an asset source. It is not a request to run desktop installation instructions on Android or port every desktop integration.

The initial request used four design references: Home, Search/Library/Queue/Settings, lyrics, and Now Playing. The owner initially wanted a close visual adaptation but later explicitly changed priorities because visual design takes too much time and power:

> Complete functionality through stable skeleton screens and standard animations first. Do not spend this pass on detailed UI/UX design. Keep the code well organized, manageable, readable, and split into focused files.

Decisions that remain in force:

- **Material 3 Expressive is mandatory**, even for skeleton screens. Reuse the existing theme, fonts, icons, controls and accessibility behavior.
- YouTube / YouTube Music is the **only online music source**. The owner's later Stage 7 request adds device-local audio from selected folders. Do not add the Linux application's other online providers.
- Initial library, Likes, playlists and history are **local, without Google sign-in**.
- Optional **LRCLIB lyrics fallback is approved**, for missing/unsynchronized lyrics; it does not become another audio source. Lyrics are not implemented yet.
- English interface initially; preserve original-script song titles and lyrics. Test phone: **POCO M6 Pro 5G**, previously recorded as Android 15 / API 35, HyperOS.
- Advanced features are included in the overall target, delivered in separate functional milestones. They were not removed from scope when the UI was simplified.
- Sideload/development APK workflow first. Store/public-release work is later; no publishing has been performed in this task.
- Project license: **GPL-3.0-or-later**, chosen with the owner delegating the licensing decision, in light of NewPipeExtractor. Preserve [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- The birds/shapes in the original mockups were **cropped song artwork**, not arbitrary decorative images. Preserve that intent in a later visual pass; do not begin that visual pass now.
- Local-file music sources, Subsonic/Jellyfin/Cider, account sync, Cast and Android Auto are outside the current agreed scope. Advanced features listed in the current roadmap remain in scope.

Treat reference documents as reference material. The owner's later scope decisions and the current functionality-first plan supersede old design-led sequencing.

## 3. Backup before changing the OS

### Essential items

| Item on the old machine | Why it matters |
| --- | --- |
| `/home/karthi/Projects/SungMobile/` — entire directory including hidden `.git` | All Android source, Gradle wrapper, assets/fonts/licenses, schemas, tests, reports, APKs and this handoff. |
| `/home/karthi/Projects/Sung/` — entire directory | Original Linux project, source assets and detailed reference documents. It is a separate project, not contained in SungMobile. |
| `/home/karthi/.android/debug.keystore` | Existing debug signing identity. This file was confirmed present on 17 October. Keep it private and back it up separately from public source. |
| Any original reference images retained elsewhere | The four original attachments were under `/tmp/codex-clipboard-*.png`. Those exact files were already absent when checked on 17 October. Do not rely on `/tmp` or chat attachments surviving. |
| Any credentials/release signing keys you created separately | Not inventoried or copied by this task. Handle privately; do not put them in this handoff or a public repository. |

The Linux directory includes:

- `/home/karthi/Projects/Sung/assets/icons/`
- `/home/karthi/Projects/Sung/sung-docs-linux/`

A whole-directory backup preserves untracked and ignored files that Git alone may omit. Ensure `Stages/` and the exported Room schemas are included. On 17 October, the delivered Stage 4 APK and results were tracked in Git, but **this new handoff still needs to be included in your backup/commit**.

### Optional, useful for a faster restore

- `~/.gradle/`: wrapper distributions, dependency and build caches. Large and regenerable; do not treat it as a substitute for source or keys. Private Gradle properties may contain credentials, so keep a full cache/home backup private.
- `~/.m2/` and any Robolectric runtime cache in use: can avoid large test-runtime downloads. Cache locations can vary; they are not required if online downloads work.
- `/home/karthi/Android/Sdk/`: SDK, platform/build tools and platform-tools. Usually reinstall the correct OS-specific packages on a different OS.
- Android Studio configuration, editor settings and a chat export if desired. This handoff is intended to work without the old chat or Codex installation.
- Existing Stage 0/1/2 screenshots and test reports are under `Stages/`. Generated build directories are reproducible and need not be kept for recovery.

### Practical backup checklist

1. Copy **both project directories** and the **signing key** onto an external drive or other storage that will survive the OS replacement. A copy on the same partition being formatted is not sufficient.
2. Include hidden files, especially `.git`. Keep the key in a private backup, outside any public Git repository.
3. Open the copied `PROJECT_HANDOFF.md`, inspect the copied `app/` and `core/` source folders, and verify the copied APK checksum using `Stages/Stage4_Results/SHA256SUMS`.
4. Confirm that the private key file is in the backup. Merely having the certificate fingerprint below cannot recreate the private key.
5. If relying on a Git remote, verify that the necessary commits/files are actually present there. The local tracking reference alone does not prove that a remote backup is accessible.
6. Keep the installed phone app and its data. A PC OS reinstall does not itself erase phone data; uninstalling Sung or clearing its storage does. The owner later removed in-app backup/export from Stage 9, so no such recovery workflow is currently planned.

No backup archive, key copy, Git commit or remote push was performed by this documentation task.

## 4. Snapshot of the repository and package identity

Repository inspected on 17 October 2026:

- Branch: `main`.
- HEAD: `1df777e4f65238cebafd021ad9ec7e5f3d5d8216` — `feat:playlists and more.`
- Previous commits shown: `e5e65d9` (`feat: playllist`) and `9f1eeed` (`The open-source music player for an Android.`).
- Working tree was clean before writing this handoff. Local `main` matched its cached `origin/main` tracking reference. Remote name: `origin`; no fetch/push or remote availability verification was performed for this document.
- If future code differs, inspect Git and update this snapshot rather than resetting work to this old commit.

| Package property | Value |
| --- | --- |
| Namespace / base application ID | `com.karthi.sungmobile` |
| Debug application ID | `com.karthi.sungmobile.stage0` |
| Debug suffix | `.stage0`, intentionally retained so each milestone updates the same installed app |
| Delivered version | `0.5.0-stage4`, code `6` |
| APK size | 22,607,459 bytes |
| APK SHA-256 | `d2d3e5da9240fb7b4947903d9c77eef8e9cd633f98e8331fb1e1a6f3fa84831c` |
| Signing certificate SHA-256 | `e5bbe3b453f20d0aa935347441a89674facc69fbdc059d2aa580ebe98c1d9fdc` |

The Stage 4 signing certificate was verified to match Stage 3. Restore the original debug key before building an update on a new machine. A newly generated key will not match the installed package; do not solve a signing mismatch by casually uninstalling the app and losing its local library.

## 5. Milestones completed so far

| Stage | Delivered work | Acceptance/status |
| --- | --- | --- |
| 0 — Core proof | YouTube search, extraction, resolve/play/pause/seek/stop; early provider and phone experiments. | Owner confirmed the core worked. Early background/automatic-next limitations were later handled by Stage 2. |
| 1 — Design foundation | M3 Expressive theme, typography, icons, artwork components, appearance options and accessible layouts. | Implemented; retained as the shared foundation. Detailed design work was subsequently postponed by the owner. |
| 2 — Durable/background playback | Single playback service/session, persistent queue, background/screen-off playback, automatic progression, repeat/shuffle, queue editing and Undo. | Owner reported every checklist item working on 12 October. |
| 3 — Persistent local library | Likes, playlists, pins, bulk/snapshot actions, listening/search history, recording switches and Private mode; Room v1→v2 migration. | Owner reported every checklist item working on 13 October. |
| 4 — Catalog and discovery | Debounced search, link intake, dedicated collections, bounded whole-collection actions, real local Home sections/mixes. | Implemented; host/live-provider verified on 14 October. **Manual POCO acceptance pending.** |

See the stage reports under `docs/` and preserved packages/checklists under `Stages/`. Historical Stage 3 count correction: 93 total tests meant **89 passed + four skipped**, not 93 passed.

## 6. Current functionality and important behavior

### Playback and queue

- `SungPlaybackService` owns the only live audio engine and session. Activities use a controller; leaving or recreating the UI does not stop playback.
- Media3/ExoPlayer handles actual audio, audio focus and session integration. UI mutations go through `PlaybackControl`.
- Queue entries have their own IDs; duplicate videos are valid distinct occurrences. Reorder/remove/jump target the entry ID, not the video ID.
- Restore queue/order/current item/position/modes after restart **paused** until the user resumes. At queue end, stop unless repeat applies; advanced autoplay is later work.
- Search/song-row playback snapshots the currently loaded list. Whole-collection actions fetch pages before applying a snapshot.
- Playback resolution has its own worker capacity, separate from catalog requests. Signed stream URLs remain transient and must never be persisted or intentionally logged.
- HTTP 403/410 recovery is bounded to one fresh resolution for the current selection/retry; other failures retain the queue and expose recovery rather than an endless skip/retry loop.

### Local library and history

- Room database: `sung.db`, **version 2**. `DatabaseProvider` explicitly registers `LibraryMigration`; preserve v1 and v2 exported schemas. Do not add destructive migration fallbacks.
- Likes use canonical YouTube video keys; playlist occurrences have independent IDs. Bulk writes are transactional. Snapshot playback is independent of later edits/deletion.
- Appearance preferences use DataStore. Room holds queue/library/history data; do not move storage into composables.
- Service-owned listening accumulation uses monotonic playing time. Seeking, pauses and buffering do not earn listening time. Qualification is 30 seconds or half a known track duration, whichever is smaller; unknown duration uses 30 seconds.
- Search history stores submitted/opened-result queries, deduplicates by category/query and retains up to 50. Typing previews alone do not create history.
- Private mode and history-clear changes increment policy epochs; stale writes cannot repopulate cleared/suppressed activity. Private mode still allows queue recovery and deliberate Likes/pins.

### Stage 4 search, links and collections

- Debounce: **350 ms**, at least two characters. Explicit submit works immediately for any nonblank query.
- Query/category changes immediately cancel/invalidate old requests. Hidden Search does not continue a pending preview. Back preserves query/category/results/scroll; process recreation re-fetches from durable identity, not saved provider tokens.
- Paste, `ACTION_SEND` text and supported `ACTION_VIEW` URLs use the same validator. Reject deceptive hosts/credentials/unsupported forms. Multiple links or song+playlist context offer choices.
- Links open previews, without autoplay or queue mutation. A valid timestamp is bounded to 24 hours and applied only to an explicit single-song Play action, also clamped to known track duration.
- Dedicated collection pages preserve repeated song occurrences. Show actual metadata, loaded versus reported totals, partial-content notices and exposed restrictions.
- Play all / Shuffle all / Play next / Add to queue / Add to local playlist load first and commit once. Bounds per attempt: **500 playable entries, 50 continuation requests, 90 seconds**. Cancel or a changed queue invalidates stale queue work. Partial loads require an explicit subset choice.
- Continuation callbacks live in a **128-entry process-local store**. Expired tokens require refresh; repeated/cyclic/empty-no-progress continuations stop safely.

### Home and presentation

- Actual local sections only: pinned playlists, Likes, recently played and full-list shortcuts. Previews contain up to 12 entries.
- Liked shuffle and Recent mix use up to 50 distinct candidates. Selection is deterministic for candidates/seed; initial seed derives from UTC day, refresh increments it, and history epoch affects Recent mix.
- Private mode/clear invalidates history-derived Home content. Playback and open mix lists use snapshots; obsolete previews close on source changes.
- Reuse `SungTheme`, Material roles, `SungSpacing`, bundled Google Sans Flex and Material Symbols Rounded. Preserve font/icon licenses. Respect reduced motion, screen/keyboard insets, scrolling, text scaling and touch targets.
- Do not invent trending, mood or personalized sections. Do not add a new visual design pass to the next functional milestone.

## 7. Known limits — do not mark these as completed

- Artist search works, but the sampled Topic channels did not expose a browseable music tab. They use a labelled song-search fallback. Where available, channel videos are labelled **channel videos**, not artist discography.
- Direct YouTube Music `MPREb_…` album browse IDs are recognized but unsupported by the current adapter. Album search can return usable playlist URLs; use those or ordinary playlist links. `UC…` browse forms route through channel handling.
- Anonymous online Home recommendations have not been implemented or verified. Home is local data, not a YouTube account feed.
- YouTube can omit private/deleted entries without providing their identity. Preserve exposed information and report partial content; do not fabricate hidden metadata.
- Android may keep ordinary YouTube links assigned to YouTube. Sung does not claim domain ownership; Share → Sung and paste are explicit entry points (`autoVerify=false`).
- Streaming/remote catalog retrieval needs network access. Offline metadata is available; intentional audio downloads are not implemented.
- Full lyrics, essential settings expansion, sleep timer, crossfade, normalization, smart rules/statistics, backup/export and visualizer remain future milestones.
- Earlier phone automation was unreliable on HyperOS (USB installation restriction and test runs ended by `SwipeUpClean`). The owner explicitly chose to test APKs manually. Do not require USB merely to continue development.
- Host tests do not prove physical phone behavior. Stage 4 upgrade/share/rotation/background checks still need the owner's report. Provider tests are dated samples and can change with YouTube.

## 8. Code organization and where to make changes

Main app package root: `app/src/main/java/com/karthi/sungmobile/`.

| Area | Ownership / starting point |
| --- | --- |
| `app/.../SungApplication.kt`, `app/AppContainer.kt` | Application dependency ownership, shared provider/repositories/scope. |
| `app/.../MainActivity.kt` | Lifecycle-owned ViewModels, saved-state handles, incoming intents. |
| `app/.../app/SungRoot.kt` | Compose feature assembly and state observation. |
| `app/.../app/SungApp.kt` | Navigation, overlays, saved screen state and shared shell. |
| `app/.../app/SungViewModel.kt` + `feature/search/` | Search only: query/category/debounce/pagination/history commits. |
| `app/.../navigation/` | Validated incoming-link state and choice dialog. |
| `app/.../feature/collection/` | `CollectionViewModel`, `CollectionActionsViewModel`, detail screen and operation dialog. |
| `app/.../feature/home/` | `HomeViewModel`, `DiscoveryHomeScreen`, local mixes. The older HomeScreen remains for foundation/previews/tests. |
| `app/.../feature/library/` | Library browser/actions, playlist dialogs and shared song actions via `LibraryHost`. |
| `app/.../feature/history/` | Privacy/history controls and recent-search UI. |
| `app/.../playback/` | Service, session callback/state mapping and `PlaybackConnection`. |
| `core/model/src/main/kotlin/com/karthi/sungmobile/core/model/` | Pure Kotlin catalog/queue/library contracts; `catalog/` has link/collection loader models, `discovery/` has Home/mix contracts. |
| `core/youtube/src/main/kotlin/com/karthi/sungmobile/core/youtube/` | `YouTubeSource`, downloader/execution/cancellation, `catalog/` mappings/details/continuations, separate `StreamExtractor`. |
| `core/playback/src/main/kotlin/com/karthi/sungmobile/core/playback/` | Coordinator/engine, persistence, session player and listening accumulator/recorder. |
| `core/data/src/main/java/com/karthi/sungmobile/core/data/` | Room entities/DAOs/migrations, library/history/queue/Home repositories and DataStore settings. |
| `core/designsystem/` | Shared theme/type/shape/motion, vectors, artwork masks, components and asset licenses. |
| `core/data/schemas/` | Exported Room v1/v2 schemas; retain for migrations and tests. |

Typical flow: screen → feature ViewModel → catalog/repository or `PlaybackControl` → service/coordinator → engine. Network and storage do not belong in composables. Provider extractor objects must not escape `core:youtube` into the app's contracts.

Use focused files, immutable state and explicit cancellation/generation checks. Keep provider experiments isolated; do not couple a catalog change to audio resolution. Use the existing Room migration and history-policy barriers for later features.

## 9. Restore the build environment on the new OS

This is the **recorded working configuration**, not a request to upgrade everything to the newest versions. Dependency versions are in [gradle/libs.versions.toml](gradle/libs.versions.toml); module settings are in their `build.gradle.kts` files.

| Tool/dependency | Recorded version/settings |
| --- | --- |
| Gradle wrapper | 9.6.0; wrapper JAR, scripts, checksum and properties are in the repository |
| Gradle daemon JVM | Java 25; `gradle/gradle-daemon-jvm.properties` sets toolchain version 25 |
| Previously tested Java | Android Studio bundled JetBrains Runtime 25, `/opt/android-studio/jbr` on Linux |
| Android Gradle Plugin | 9.4.1; Android modules use AGP's built-in Kotlin support |
| Kotlin JVM / Compose plugin | 2.2.10 |
| Android SDK | compile/target API 37; minimum API 26; Build Tools 36.0.0 used for recorded verification |
| Compose BOM / Material 3 | 2026.09.00 / 1.5.0-beta01 |
| Media3 | 1.11.1 |
| NewPipeExtractor | v0.26.5, JitPack |
| Coroutines / OkHttp / Coil | 1.10.2 / 4.12.0 / 3.3.0 |
| Room / DataStore | 2.8.4 / 1.2.0; Java annotation processor for Room, no KSP setup |
| Robolectric / Roborazzi | 4.17 / 1.76.0 |
| JVM bytecode target | Java 11; this is distinct from the Java 25 runtime used to run Gradle |

Restore steps:

1. Restore `SungMobile` and `Sung` to any suitable location. They do not have to retain `/home/karthi/Projects`; update machine-specific paths accordingly.
2. Restore the original debug keystore to the Android debug signing location on that OS before building the update (`~/.android/debug.keystore` on the old Linux host). Verify the built APK certificate matches the fingerprint in section 4.
3. Install Android Studio and a compatible Java 25 runtime. Install SDK platform 37, Build Tools 36.0.0 and platform-tools. Use the repository's Gradle wrapper, not a separately installed system Gradle. The old machine's default Java 27 was not the recorded working build runtime.
4. Open the **SungMobile root**, where `settings.gradle.kts` is located. Let Studio synchronize. Gradle daemon criteria may choose/download Java 25; inspect the actual JVM if there is a mismatch rather than assuming `JAVA_HOME` overrides daemon criteria.
5. Regenerate ignored `local.properties` with your new SDK path. Old value: `sdk.dir=/home/karthi/Android/Sdk`. Let Studio generate the correct Windows path escaping if moving to Windows.
6. Build, then run host tests. Compare against the saved baseline and investigate real differences. An internet connection is needed for missing Gradle/Maven/Google/JitPack artifacts and Robolectric Android runtimes.
7. Install/update the APK manually on the POCO, retaining app data and the application ID.

Linux example, run from the restored project root; change the JBR path if necessary:

```bash
chmod +x gradlew
JAVA_HOME=/opt/android-studio/jbr ./gradlew --version
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew :core:model:test :core:youtube:test :core:data:testDebugUnitTest :core:playback:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `gradlew.bat` from PowerShell or Command Prompt, set the appropriate Java/SDK paths or use Android Studio's Gradle JVM settings, and run the same task names. Do not copy Linux SDK executables and expect them to run on another OS.

Fresh build output is normally `app/build/outputs/apk/debug/app-debug.apk`; confirm `app/build/outputs/apk/debug/output-metadata.json` if it differs. The preserved deliverable under `Stages/Stage4_Results/` is the authoritative previously tested APK.

Live provider tests are separate, opt-in and contact YouTube:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :core:youtube:test -Plive=true --tests '*LiveProviderTest'
```

For a slow dependency server, the successful resumed build used bounded network timeouts:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew -Dorg.gradle.internal.http.connectionTimeout=10000 -Dorg.gradle.internal.http.socketTimeout=10000 :app:assembleDebug --console=plain
```

Do not assume a long first build is an app bug. On 14 October missing dependency metadata and a large Robolectric Android runtime download delayed verification; the completed build and tests passed. Offline mode failed when required metadata was missing. Inspect logs/download progress before discarding source or caches.

## 10. Verification evidence to preserve

[Stages/Stage4_Results/README.md](Stages/Stage4_Results/README.md) and [build-summary.json](Stages/Stage4_Results/build-summary.json) contain the final recorded results.

| Module | Passed host tests |
| --- | ---: |
| core:model | 27 |
| core:youtube | 7 |
| core:data | 15 |
| core:playback | 30 |
| app | 45 |
| **Total** | **124** |

Host total: 129 cases = 124 passed + five opt-in network probes skipped. Separate live run: five passed, zero skipped/failed. These figures are not 129 host tests plus five additional distinct tests.

Live sample on 14 October, English/India provider configuration:

- Song search continuation and two real audio byte reads passed (WebM, reported 160 kbps).
- Album/playlist search and details passed.
- One playlist returned 40 entries against 45 reported, with partial-content information.
- A long playlist returned 101 entries on the first page and 15 on the next, against a reported total of 120. Counts describe provider results; they are not invented completeness guarantees.
- Artist fallback searches passed; a full discography was not verified.

Preserve `build.log`, `provider-unit-tests.log`, `live-provider.log`, `live-provider-results.xml`, `host-test-counts.json`, `lint-results.xml`, `apk-signature.txt` and `SHA256SUMS` in the results folder. Manual phone outcomes must be recorded separately from these host checks.

## 11. Exact next actions and remaining roadmap

### Resume here

1. Read this handoff and the current reports; inspect the restored working tree before changing code.
2. Restore the environment and signing key. Rebuild/test once to establish that the new OS can reproduce the project.
3. Ask for any Stage 4 phone results already obtained. Otherwise use the supplied Stage 4 checklist: upgrade/data retention, search, link preview/share, timestamps, collection paging/actions, Back/rotation, Home/privacy, and background playback regression.
4. Fix reported Stage 4 problems before declaring phone acceptance. Preserve separate source, network and device limitations.
5. Once accepted, update the Stage 4 report/checklist status, then prepare a detailed **Stage 5 plan** for review. Do not silently jump to downloads, crossfade or a UI redesign.

### Stage 5 planning starting points

Stage 5 covers YouTube lyrics where available, an optional matched LRCLIB fallback, plain/synced/unavailable states, current-line highlighting, tap-to-seek, manual scroll/return-to-current, persisted text size and timing offsets, essential preferences, lyrics privacy/cache controls, About/licenses and functional color-source settings.

Before implementation, inspect the Linux lyrics/reference behavior, probe actual provider capability, define matching/rejection and stale-request cancellation, and specify cache/offset ownership and any migration. No lyric matching thresholds or complete Stage 5 architecture have yet been approved. Suggested ownership follows the existing module boundaries: pure lyric models/contracts in `core:model`, provider work isolated from audio resolution, persistence in `core:data`, and a focused `app/feature/lyrics` UI/ViewModel. Treat this as a planning starting point, not completed code.

### Later authorized milestones

| Stage | Remaining scope |
| --- | --- |
| 6 — Playback convenience | Speed/pitch preservation, sleep timer (duration/end of track/end of queue), optional fade, long-recording resume, saved sessions. |
| 7 — Downloads/offline | Explicit download/cancel/retry/remove, durable jobs, offline inventory/playback, storage budget and Wi-Fi policy; separate cache from saved audio. |
| 8 — Advanced playback | Next-track preparation, verified supported-format gapless behavior, optional crossfade, normalization/trim, smart shuffle and opt-in related autoplay. |
| 9 — Advanced local library | Saved smart playlists, cleanup, playlist history/restore and listening statistics. App-data backup/import/export and new playlist-link exchange are excluded by the owner's later decision. |
| 10 — Remaining tools | Lyrics search/jump, LRC import/timing tools, source details and a minimal on-demand visualizer where supported. |
| 11 — Stabilization | Full integration, migration/backup recovery, offline/network/storage failure cases, performance/device runs, release packaging and source/license notices. |

The complete acceptance gates and defaults are in [docs/FUNCTIONAL_COMPLETION_PLAN.md](docs/FUNCTIONAL_COMPLETION_PLAN.md). Public publishing is a separate action, not implied by implementation approval.

## 12. Reading order and reference map

1. This file — current status, recovery and continuation.
2. [README.md](README.md) — build/project overview.
3. [docs/FUNCTIONAL_COMPLETION_PLAN.md](docs/FUNCTIONAL_COMPLETION_PLAN.md) — current authoritative milestone order and delivery rules.
4. [docs/STAGE4_REPORT.md](docs/STAGE4_REPORT.md), [docs/STAGE4_PLAN.md](docs/STAGE4_PLAN.md), and [Stage 4 checklist](Stages/Stage4_Results/PHONE_TEST_CHECKLIST.md).
5. [docs/PLAYBACK_GUIDE.md](docs/PLAYBACK_GUIDE.md) — practical code ownership/use, including Stage 3/4 additions.
6. [docs/STAGE3_REPORT.md](docs/STAGE3_REPORT.md), [docs/STAGE3_PLAN.md](docs/STAGE3_PLAN.md), [docs/STAGE2_REPORT.md](docs/STAGE2_REPORT.md).
7. [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md), [docs/STAGE1_REPORT.md](docs/STAGE1_REPORT.md), [docs/STAGE0_REPORT.md](docs/STAGE0_REPORT.md).
8. [docs/ANDROID_PLAN.md](docs/ANDROID_PLAN.md) — original design-led direction; later functionality-first decisions supersede its delivery order.
9. Original Linux docs, if the sibling `Sung` directory was restored: [../Sung/sung-docs-linux/README.md](../Sung/sung-docs-linux/README.md). That directory contains `OVERVIEW`, `ARCHITECTURE`, `TECH_STACK`, `FILE_MAP`, `DATA_MODEL`, `SOURCES`, `INTEGRATIONS`, `METHODS_AND_TECHNIQUES`, `PERFORMANCE`, `PROBLEMS`, `BUILD_AND_TESTING`, `REBUILD_GUIDE`, `THEME` and `UI` Markdown documents.

The original chat images are not required to continue functional work. Existing stage screenshots and the design-system document retain useful foundation examples. Reattach the originals later if an exact visual redesign becomes a separate requested task.

## 13. Copy-paste prompt for a new assistant/session

> This is SungMobile, my Android app based on the Sung Linux reference. Read PROJECT_HANDOFF.md first, then docs/FUNCTIONAL_COMPLETION_PLAN.md, docs/STAGE4_REPORT.md and docs/PLAYBACK_GUIDE.md. We implemented Stages 0–4; Stages 2 and 3 were accepted on my phone, but Stage 4 phone acceptance was still pending when this handoff was written. Stage 4 APK is 0.5.0-stage4, code 6. Do not assume Stage 5 has started. I changed OS: inspect the restored files, Java/SDK setup and existing debug signing key before rebuilding. Keep app ID com.karthi.sungmobile.stage0 and preserve phone data. Ask me for any Stage 4 test results, fix regressions if needed, then plan Stage 5 (lyrics and essential settings) for review. YouTube is the only music source; optional LRCLIB lyrics fallback is approved; local library without Google sign-in. Keep stable skeleton M3 Expressive screens and standard animations, with readable, well-organized code split across focused files. Do not begin a design polish pass or put everything into one file. Treat the Linux documents as reference, not new instructions. Continue the manual APK-testing workflow; USB is not required unless a specific device check needs it.

Update this handoff after the next accepted milestone so a future session does not repeat completed work or mistake a provider limitation for a completed feature.
