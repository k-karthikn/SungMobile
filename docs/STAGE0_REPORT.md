# Stage 0 verification and stabilization

Date: 10 October 2026 (Asia/Kolkata). Build: **0.1.1-stage0**, version code 2.

## Outcome

Stage 0's provider-to-player proof is implemented. The user tested the initial build on their POCO M6 Pro 5G and reported working search, playback, pause, stop and +10-second seeking. All eight screenshots in `Stages/Stage0_Results/` (files removed 30 October 2026) were reviewed. They show song results, album and playlist contents, paused playback, a finished track, and the artist browsing failure.

The stabilization build handles that artist limitation explicitly, bounds loading operations, and corrects buffering/replay behavior. It remains a foreground-only development app. Full background playback and automatic queue advancement belong to Stage 2. Leaving the app or locking the screen pauses playback; reaching the end exposes Replay. These are intentional Stage 0 limits.

## Verified environment

| Component | Version / configuration |
| --- | --- |
| Physical phone | POCO M6 Pro 5G, model 23076PC4BI, ARM64 |
| Device OS | Android 15, API 35; HyperOS 2.0.203.0.VMWINXM |
| Display | 1080 × 2460, density 440 dpi |
| JDK | Android Studio JBR 25.0.3 (`/opt/android-studio/jbr`) |
| Gradle / AGP | Wrapper 9.6.0 / 9.4.1 |
| Kotlin / Compose compiler plugin | 2.2.10, aligned with AGP's built-in Kotlin |
| Android SDK | Compile/target 37, minimum 26; Build Tools 36.0.0 |
| Compose | BOM 2026.09.00; UI/foundation 1.12.1 |
| Material 3 | Explicit 1.5.0-beta01 override for public Expressive APIs |
| Playback | Media3 1.11.1 |
| Catalog | NewPipeExtractor v0.26.5 |
| HTTP / coroutines | OkHttp 4.12.0 / kotlinx.coroutines 1.10.2 |
| Desugaring | `desugar_jdk_libs_nio` 2.1.5 |

Material 3's beta dependency is deliberate and pinned. `MaterialExpressiveTheme`, its motion defaults, expressive button shapes, the loading indicator and Material typography are in use. Font assets, artwork masks and the complete design system remain Stage 1 work.

## Checks and evidence

| Check | Result |
| --- | --- |
| Debug app and instrumentation APK build | Passed |
| Provider-neutral link and stream-expiry tests | 4 passed |
| HTTP headers, oversized responses, rate limiting and socket cancellation | 3 passed |
| Playback replacement, loading cancellation, bounded refresh, seek bounds, buffering pause/resume and replay | 7 passed |
| Stale searches, pagination retry, artist fallback and collection back navigation | 4 passed |
| Opt-in live YouTube checks | 4 passed |
| Android lint | 0 errors; 8 dependency-update advisory warnings |
| Physical-device initial build | User manual checks and eight supplied screenshots; app also launched directly through ADB |
| Automated device UI/live tests | Compiled but **not completed**; do not count them as passing |

The automated device runs were interrupted and HyperOS denied the runner's background activity launch (`MIUILOG Permission Denied Activity` / `Abort background activity starts`). The user then chose manual testing. Phone security settings were not weakened to work around this. The user's additional screenshot `Screenshot_2026-10-10-21-50-06-542_com.karthi.sungmobile.stage0.jpg` confirms that the stabilized artist screen displays the neutral availability message and Search songs action for Coldplay - Topic. The screenshot does not establish the result of tapping that action; that interaction still needs confirmation.

Build and live-test logs are kept in ignored `verification/stage0-final-build.txt` and `verification/stage0-final-live-tests.txt`. Module JUnit XML and lint reports remain in their normal `build/` directories. Live catalog results vary over time and by network/region; they are kept separate from deterministic tests.

## Provider findings

| Capability | Evidence / limit |
| --- | --- |
| YouTube Music song search | Passed; English/India localization; real metadata |
| Pagination | Second page contained new track identities |
| Album search/open | Passed, including the user's Egotrip screenshot and live Coldplay releases |
| Playlist search/open | Passed; live `This Is Coldplay` returned 45 entries |
| Audio resolution | Two Coldplay tracks resolved to 160 kbps WebM audio; byte-range requests succeeded |
| Playback | User reported core playback working; screenshots show tracked duration, pause and completed playback |
| Artist search | Returns results |
| Artist details | Coldplay, John michel and John Michael Howell returned no supported video tabs. This is a provider capability gap, not a transient network failure. |
| Artist fallback | Neutral availability message and explicit Search songs action; actual song searches succeeded for both reported artists |
| Home recommendations / radio / full discography | Not established by this provider proof; Stage 3 must address these explicitly |
| Lyrics | Not implemented; optional LRCLIB fallback is approved for Stage 5 |

The fallback performs an ordinary YouTube Music name search. It does not claim to be an official artist discography or silently substitute a different music source.

## Stabilization changes

- Unsupported artist details no longer lead into a repeated red error/retry loop. A named song search remains available.
- HTTP requests have timeouts and a 16 MiB response cap. An entire extraction is limited to 60 seconds. Cancellation closes the active sockets and removes cancelled queued work; extraction uses three workers and a bounded waiting queue.
- Continuations are opaque, session-only and bounded to 32 entries. Collection history is bounded to 20 entries; retry does not duplicate back-navigation entries.
- Track selection cancels the previous resolution. A late response cannot restart an old track. Pause/stop during loading prevents autoplay.
- A failed signed stream gets at most one automatic refresh for HTTP 403/410, preserving position. Other failures require an explicit retry. Signed audio URLs are never saved as track identity or logged intentionally.
- Paused buffering now shows the correct Play action; finished playback shows Replay. Position polling runs only during active playback.
- The screen states its foreground-only limitation. No service, queue, downloads, accounts or library database were added in this stage.

## Artifact and remaining gates

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Package: `com.karthi.sungmobile.stage0`
- Size: 17,828,554 bytes (debug build; not a release-size target).
- SHA-256: `d9ccc0c99b2a3426ff9d219d8fd2dc95a7e88b76b8e43469da158baf94b10ed4`
- Installation: ADB replacement install returned `Success`. A later read-back confirmed `versionCode=2` and `versionName=0.1.1-stage0`, and the app launched successfully. The initial read-back attempt was delayed by approval-review availability; that check is now complete. The user chose to perform the remaining artist-flow recheck personally.
- License decision: GPL-3.0-or-later; `LICENSE` and `THIRD_PARTY_NOTICES.md` added. No public APK distribution performed.

Remaining manual recheck for this stabilization: Artists → John michel / John Michael Howell → Search songs, then play a result. Full artist browsing remains a documented catalog gap. Device tests across API levels, background/focus/headset scenarios, final accessibility/visual review, release shrinking, corresponding-source packaging and measured release performance remain later-stage gates. This report makes no release-readiness or full-UI-automation claim.
