# Baseline stabilization before UI improvements

19 October 2026 — `0.10.1-stabilization`, code 16. The owner accepted Stage 9 code 15 on the POCO, skipped Stage 10, and authorized this first stabilization pass. Manual acceptance of code 16 is pending. UI improvements and final post-UI regression remain later work.

## Confirmed problems fixed

1. **Private-mode Recent view:** ordinary Library Recent queries and playback snapshots did not check the privacy policy, although Home and Stage 9 views did. They now read policy and history in the same Room transaction, and the Library observes policy changes. Likes and deliberate playlist edits remain available.
2. **Availability progress after edits:** scanning used a row offset, so deleting an earlier occurrence could skip a later unchecked song. Progress now follows occurrence IDs; each batch remains bounded to 100 probes. Retry unknown probes only unknown results and keeps prior confirmed results. Canceled or obsolete provider work cannot publish into another scan.
3. **Duplicate review:** the first occurrence was retained automatically but could not be selected instead of a later copy. All members of a duplicate group are now reviewable; only later occurrences are selected by default. The removal confirmation freezes the selected IDs and count, and the action retains its original playlist target through navigation.
4. **Late library work:** pending version previews and statistics pages could update a changed screen or append data from an older history snapshot. Navigation and load generations now reject late results, duplicate mutations are guarded, previews can be canceled, and failed reads have an actual Retry button.
5. **Saved-session read failures:** malformed session files were treated as empty, so another save could overwrite unreadable data. Read/validation failures now propagate to the existing error UI and prevent mutation. AtomicFile recovery is attempted before deciding a file is absent. Resume-point clearing also handles storage errors.
6. **Lyrics request lifetime:** leaving Lyrics did not cancel an outstanding request. Screen exit now cancels it; cancellation is checked before fallback requests, cache writes and final UI publication. Cache operations are serialized so a clear cannot race an in-progress write. No provider matching or lyrics capability change is included.

## Audit and evidence

The pass inspected the single service-owned player, queue persistence, network retry/expiry logic, focus/unplug behavior, timer lifetime, library privacy, saved sessions, lyrics cache limits, manifests and database migrations. Existing regression tests cover pause/stop during resolution, stale provider responses, network failure with position retention, one-time expiry refresh, duplicate queues, repeat/shuffle, timer precedence, controller disconnect, paused restoration and storage failures. New tests exercise the fixes above and reopening a database containing queue, smart rules, versions and listening history together.

Migration fixtures cover schema 1, schema 2 (accepted pre-Stage 7 builds), schema 3 (optional device-audio prototype) and schema 4, all opening through the supported migration chain. The active schema remains 5; no additional migration is needed for code 16. Settings and saved-session persistence retain their existing formats. No destructive fallback was added.

The full offline test suite, lint and debug APK build passed: 163 tests passed, 6 opt-in live-provider probes were skipped, and there were no test failures. Lint reported 0 errors and 10 warnings. The host result counts, lint status, APK metadata and checksum are saved in `Stages/Stabilization_Results/`. The app ID remains `com.karthi.sungmobile.stage0`; the verified certificate matches code 15: `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`.

## Feature inventory and remaining gates

| Area | Current evidence / remaining gate |
| --- | --- |
| Search, collections, YouTube playback and queue | Previously phone-accepted; offline provider/controller/player tests rerun. Current live provider availability is not inferred from mocks. |
| Likes, playlists, smart playlists, versions and statistics | Stage 9 phone-accepted; privacy, scan, mutation, pagination and reopen regressions covered on host. Code 16 needs the focused phone check. |
| Lyrics, settings, timers and saved sessions | Previously phone-accepted; lyrics cancellation and saved-file failure coverage added. |
| Migration and restart recovery | Host fixtures and repository reopen tests; actual upgrade over code 15 and process restart still belong to the owner's test. |
| Performance, battery and OEM behavior | No measured POCO long-run, memory, battery or frame-time results were produced in this pass. A normal listening session with lock/focus/network changes is included in the manual checklist. |
| Stage 7 and Stage 10 | Device audio/downloads remain outside build source sets. Stage 10 is skipped. Neither is a completion gate. |
| Release packaging | Existing license texts remain bundled. A complete resolved release-license inventory, optimized production artifact and source distribution remain final-release work. This is a signed development update, not a public release. |

The assistant did not install the APK, clear phone data, connect USB or run device automation. After the owner accepts this baseline, UI improvements can proceed against it. Final integration checks should follow the UI work before declaring the full stabilization/release stage complete.
