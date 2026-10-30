# Stage 2 — Persistent queue and background playback

Implemented on **11 October 2026**. **Code, host verification and user-reported POCO acceptance are complete.** On 12 October 2026 the user reported completing the supplied checklist with everything working and no problems. The device result is user-reported, not an automated run; no phone installation was performed by the agent.

## Delivered

- One service-owned ExoPlayer engine, exposed through a Media3 session. The screen holds a MediaController; leaving the activity no longer pauses or releases audio.
- Standard media notification, lock-screen/headset command integration, audio focus/noisy handling and network playback wake locks.
- Queue snapshots from currently loaded search/collection songs; automatic next, previous/next, repeat off/all/one and shuffle.
- Play next, append, move up/down, remove, clear upcoming and Undo. Duplicate songs use distinct occurrence IDs. Removing the current song preserves play/pause intent; Undo does not restart an older song.
- Room queue/position persistence with atomic transactions and an exported version-1 schema. Startup restores paused. Position checkpoints occur roughly every five seconds, as well as queue/intent changes and orderly service shutdown. A failed initial read is not overwritten with an empty queue.
- DataStore appearance settings with migration from Stage 1's preferences; visible storage errors.
- Plain Material queue screens and controls, with standard list motion respecting reduced motion. Existing Stage 1 artwork/theme remains in place; no new custom design work.
- Source resolution remains cancellable. Signed URLs are not persisted. Expired streams receive one refresh attempt; other errors retain the queue for explicit retry/skip.
- Separate model, data, playback, session, service/controller, catalog and feature files. See [the playback code guide](PLAYBACK_GUIDE.md).

## Host verification

| Area | Result |
| --- | --- |
| Model: links and pure queue rules | 13 passed |
| Provider HTTP adapter | 3 passed |
| Playback: coordinator, queue transitions, persistence scheduling and Media3 player adapter | 26 passed |
| Data: real Room reopen/rollback and preference migration | 5 passed |
| App: catalog/search, appearance, queue controls, navigation and real Media3 controller/session protocol | 18 passed |
| **Total** | **65 passed, 0 failures** |
| Live provider probes | 4 opt-in tests skipped; provider implementation unchanged |
| Debug APK and instrumentation APK | Built successfully |
| APK signature | Verified, v2 scheme |
| Android lint | 0 errors; 13 dependency/tool-version advisories |
| POCO supplied checklist | User reported all checks working without problems, 12 October 2026 |

The session/controller test uses the real Media3 protocol with fake network resolution and audio decoding. It verifies initial Play after setting an empty queue, duplicate removal, Undo status delivery, storage-error delivery, pause/seek/resume and audio continuing after the controller disconnects. It also verifies rejection of an untrusted controller. It does not claim real-device audio or foreground-service execution.

Two relevant screen captures were inspected: queue controls at 200% font size and the full player's added controls. The historical full visual matrix was not rerun. Three navigation/player-action regressions from the design suite were included.

The exported service follows Media3's connection model. Its callback accepts the app's UID and trusted media controllers; playlist mutation and custom commands are restricted to the app UID. The generic exported-service lint warning is narrowly suppressed with that policy documented in the manifest and covered by a host test.

## Artifact

- APK: `Stages/Stage2_Results/Sung-0.3.0-stage2-debug.apk` (files removed 30 October 2026)
- Version: `0.3.0-stage2`, code `4`
- Application ID: `com.karthi.sungmobile.stage0` (retained for in-place updates)
- Size: `22,266,355` bytes
- SHA-256: `1cae675ec6b06cc94f82fc40062f62abe3cbca6b10c880b4afd77cf4afca9be5`
- Results, XML test reports, relevant screenshots and build summary: `Stages/Stage2_Results/` (files removed 30 October 2026)

`final-core-checks.log` records the passing core tests and an initial lint failure for renamed Media3 constants. Those constants were corrected; `final-app-build.log` records the final successful app checks, APK builds and lint. Nothing remains waived from that failure.

## Device acceptance and limits

The user reported successful completion of the phone checklist (files removed 30 October 2026) on 12 October 2026. The checklist remains available for later regression tests.

The supplied checklist covers actual YouTube playback, leaving/locking the POCO, automatic next with the screen off, system media controls, audio focus/headphone disconnects, rapid changes, process restoration and network failure recovery. The user reported the checklist working without problems; no per-case logs or independent automated device results were supplied. The agent made no OEM battery-management changes. An abrupt process kill may lose up to approximately one checkpoint interval of position; force-stop is expected to stop audio and requires an explicit resume.

This milestone does not add likes/playlists, lyrics, downloads, crossfade or the rest of the expanded plan. They remain scheduled in separate stages. Artist catalog limitations from Stage 0 are unchanged. Stage 2 is accepted for progression to Stage 3; this is not a claim of coverage across every device or provider condition.
