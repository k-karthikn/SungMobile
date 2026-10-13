# Sung Stage 2 — POCO manual test

Build: **0.3.0-stage2**, version code **4**. Install `Sung-0.3.0-stage2-debug.apk` as an update to your existing Sung app. Do not uninstall first: this check includes preserving Stage 1 appearance settings.

Completed according to the user on 12 October 2026: all checklist checks worked without problems. Retained for future regression checks. The automated host tests use fake audio/network for player transitions; they do not replace device checks.

## Main checks

1. **Update and settings:** launch Sung. Your theme, color choice and reduced-motion setting should remain. Search should still work.
2. **Queue and playback:** search for an artist, tap a song, and open the player. Queue should contain the songs currently loaded in those results. Check pause, resume and ±10 seconds.
3. **Background:** while audio plays, press Home and use another app for about one minute. Audio should continue. Return to Sung: the same song/position should be shown, without a second audio stream.
4. **Screen off:** play, lock the phone and leave it for two minutes. Audio should continue. Try pause/resume and Next from the lock screen and media notification.
5. **Automatic next:** choose a song with another entry after it. Seek close to its end, then leave it playing. The next song should begin automatically, including while the screen is locked. Repeat one should replay the current song; Repeat all should wrap from the last to the first. Repeat off should stop at the end of the queue.
6. **Queue edits:** from Search use the + menu for Play next/Add to queue. Add the same song twice. In Queue use Options to move/remove one occurrence. Removing an upcoming song must not restart the current song. Check Undo and Clear upcoming. Removing the current song should advance; Undo should restore its entry without jumping back to it.
7. **Shuffle:** toggle on, inspect the upcoming order, then toggle off. The current song should not restart. Disabling shuffle should restore the baseline order; manual reordering creates a new baseline.
8. **Restore:** pause at a recognizable position, wait five seconds, then force-stop Sung in Android settings and reopen it. The queue and position should return **paused**. Tap Play to resume. Force-stop itself is expected to stop playback.
9. **Failure recovery:** while streaming a song, disconnect the network long enough to exhaust buffered audio. The queue should remain. Restore the network, tap Retry/Play, and check recovery. Try Next if a particular song is unavailable. There should be no rapid endless song skipping.
10. **Interruptions:** test another app taking audio focus, then returning to Sung. If using headphones, disconnect them while playing: Sung should pause. Reconnecting alone should not unexpectedly start audio.
11. **Rapid actions:** tap Next a few times during audio preparation, then Pause. Only the final selection should remain; an older request must not start playing later.

## What to send back

For a failure, report the numbered check, song/search used, whether the phone was locked, what you tapped, and what happened. A screenshot of the player or notification is useful. Put screenshots in `Stages/Stage2_Results/` if convenient.

Please test the default phone settings first. If HyperOS stops playback, report that result before changing battery/background settings so we can distinguish app behavior from an OEM restriction.

Likes/playlists, lyrics and downloads are later milestones and are not part of this APK.
