# Stage 5 — POCO manual checklist

Status: **owner accepted Stage 5 on 18 October 2026** after reporting everything working fine. Individual checklist outcomes were not separately recorded. `Sung-0.6.1-stage5-debug.apk` (code 9) uses the same signing key and app ID as code 8. This checklist remains a regression reference; no USB connection is required.

1. Confirm existing Likes, playlists, settings and queue remain. Playback should stay paused after a cold restart until you press Play.
2. Play a song, open **Now playing → Lyrics**, and check a song with plain lyrics, one with timed lyrics if available, and one with no lyrics. Source attribution and unavailable/retry states should be clear. Plain lyrics should never highlight or seek by line.
3. On a timed song, check current-line highlighting, tap a line to seek, adjust timing by ±0.25 s and confirm the timing persists after restart. Manually scroll, then tap **Return to current line**.
4. Switch songs rapidly while Lyrics is open. No previous-song lyrics should flash or replace the new song. Rotate, leave/reopen the app, lock the phone and confirm playback remains stable.
5. In Settings, turn on LRCLIB fallback. It sends title, artist and duration when YouTube has no lyrics or only plain lyrics. Check whether a plain YouTube song gains timed lyrics when LRCLIB has a strict match; the source should switch to LRCLIB. If LRCLIB has no timed match, keep the plain YouTube lyrics. Check a missing-lyrics song; an uncertain match should remain unavailable. Turn on Private mode and confirm LRCLIB requests are skipped.
6. Change lyric text size and restart to confirm persistence. Turn lyric caching off, then on; clear the cache. Likes, playlists, queue and per-song timing offsets must remain. Check that System/Light/Dark and Blue/Wallpaper appearance choices still work.
7. Open **App license** and **Open-source notices** in Settings. Recheck the Stage 4 mini-player on album/playlist pages and the Stage 3 background controls.

Report a failure with the step, song URL, expected and actual behavior. For lyric mismatches, include the title/artist/duration shown by Sung and the displayed lyric source.
