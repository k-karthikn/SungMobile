# Stage 5 plan — lyrics and essential settings

Prepared: 18 October 2026. **Implemented; phone acceptance pending.** Stage 4 was accepted on the phone. The Stage 5 result is documented in `STAGE5_REPORT.md`; this file records the implementation plan and chosen defaults. The new debug signing key was established with code 7, and code 8 retains the debug application ID `com.karthi.sungmobile.stage0`.

## Outcome and limits

Show actual lyrics for the current YouTube song when available, with a user-controlled LRCLIB fallback. Support synchronized and plain text honestly, plus a clear unavailable/error state. Add the few settings needed to control lyrics, cache and existing appearance behavior. Keep the established Material 3 Expressive skeleton screens and standard motion; no visual redesign, sign-in, extra audio source, downloads or advanced lyric editing.

The Linux Sung documents are behavior references. Their Python `ytmusicapi` lyrics path and LRCLIB policy are **not** an Android implementation or proof that the current NewPipe adapter exposes lyrics. Probe the Android provider first. If direct YouTube lyrics are unavailable, report that limitation and deliver a clearly attributed optional LRCLIB path rather than labeling fallback lyrics as YouTube lyrics.

## Chosen behavior defaults

1. **Fallback default:** LRCLIB is **off** until the user enables it, because lookup sends the song title, artist and duration to LRCLIB. Lyrics from YouTube remain available regardless.
2. **Match policy:** normalized exact title and primary artist, with duration within 2 seconds. Preserve qualifiers such as live, remix and cover. Reject ambiguous/missing artist or duration and trivial lyric bodies; expose source attribution.
3. **Offset scope:** per-video offset, clamped to ±10 seconds, applied to display and tap-to-seek consistently.
4. **Cache:** a bounded private lyrics cache (256 KiB lyric content and 8 MiB total). Clearing cached lyrics affects only this cache, not Room library/history or playback recovery. Saved offsets remain. A switch disables and clears the cache.

## Sequence and ownership

| Part | Work | Gate |
| --- | --- | --- |
| 5A. Provider probe | Inspect current NewPipeExtractor capabilities for YouTube Music lyric browse/timing data. Probe a small set of real songs with synced, plain and absent lyrics. Record region/date, source IDs, failures and whether direct lyrics are feasible without a fragile new dependency. Probe LRCLIB response and rate-limit behavior separately. | A capability note distinguishes verified direct lyrics, fallback and unavailable cases. No audio-resolution or catalog regression. |
| 5B. Models and retrieval | Add `LyricsResult` variants (synced, plain, unavailable/error), source attribution and bounded timing/metadata contracts in `core:model`. Isolate YouTube retrieval in `core:youtube/lyrics`; isolate LRCLIB HTTP behind its own adapter, with fixed HTTPS host, timeouts, response-size limits, cancellation and rate-limit backoff. Select YouTube first, then LRCLIB only when enabled and a strict match passes. | Stale requests cannot replace the current track; plain text is never assigned invented timestamps; wrong matches are rejected. |
| 5C. Persistence | Store lyric preferences (fallback enabled, text size, cache choice) with the existing DataStore settings. Store per-video offsets and bounded cached results in `core:data/lyrics`, keyed by canonical YouTube video ID plus source/version. Choose a private cache file store unless a concrete query need justifies a Room table; any Room change requires an explicit v2→v3 migration and exported schema. | Preferences/offsets survive restart, cache eviction is bounded, and clearing lyrics cache leaves library, history and queue intact. |
| 5D. Player UI | Add `feature/lyrics` ViewModel and a simple lyrics page reachable from Now Playing. Observe the service-owned current track and position; fetch only for that track. Show loading, retry, source, synchronized/plain/unavailable states, current-line highlight, tap-to-seek where seekable, manual scrolling and Return to current. Respect screen-reader semantics, text scale, insets, reduced motion and standard component animations. | Track changes cancel old work; rotation and leaving/returning do not show previous-song lyrics or create a player; tapping a timed line seeks the existing playback controller. |
| 5E. Essential settings | Extend the current Appearance screen or a small Settings route with the LRCLIB switch, lyric text size/offset controls, lyrics cache size/clear action, content/privacy explanation, About and open-source licenses. Keep the current System/Light/Dark and Blue/Wallpaper color-source choices functional; correct any actual settings bug found during Stage 4 review. | Settings persist and are usable offline; cache clearing is scoped; notices remain accessible. |
| 5F. Delivery | Run focused model/provider/data/UI tests, app integration checks and lint. Use opt-in live probes for provider behavior. Build an APK signed by the new retained debug key and give the owner a short manual POCO checklist. | Owner verifies lyric accuracy, switching, seeking, persistence, privacy and background playback. Record provider/device limits before acceptance. |

## Behavior details

- A lyric request belongs to the currently selected **video ID** and a request generation. Switching, removing or replacing the current queue entry invalidates the old response. Two occurrences of the same video may reuse cached text/offset, while UI position always comes from the live playback service.
- Timed lines retain source timestamps and original script. Use the playback clock plus the saved offset to find the current line. Manual scroll pauses automatic following until Return to current is tapped; a track change resets this UI state. Tap-to-seek is offered only for timed lyrics on a seekable track and clamps to its known duration.
- Plain lyrics scroll as text and have no current-line claim or line seek. Empty, unavailable, network failure, rate limiting and rejected fallback matches receive distinct user-readable states. Retry is explicit and bounded; a failed lookup never pauses or changes music.
- LRCLIB is lyrics only. When enabled, send the minimum metadata required for matching. Private mode suppresses history, as it already does; lyric lookup should not itself create listening/search history. Specify whether Private mode also disables external LRCLIB requests during review; propose disabling them while Private mode is on.
- Keep network and parsing outside composables. Do not persist signed audio URLs, opaque provider page tokens or credentials with lyric text. Show attribution for every displayed source.

## Verification and manual acceptance

Deterministic tests should cover timed/plain parsing, duplicate/equal timestamps, malformed and oversized responses, exact-match/rejection cases, rate-limit/network failures, cancellation after song changes, offset boundaries, cache eviction/clear isolation and settings persistence. A live probe establishes actual YouTube capability and fallback behavior; it cannot guarantee every song has lyrics.

The owner tests an update on the POCO: open lyrics for a synced song, a plain-text song and a missing-lyrics song; switch tracks rapidly; tap a line and adjust offset; scroll manually and return to current; rotate, background and reopen; restart and confirm settings/offsets; toggle fallback and Private mode; clear lyric cache and confirm Likes/playlists/queue remain. Stage 4 acceptance was reported before this implementation.
