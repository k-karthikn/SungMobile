# Stage 6 — POCO manual checklist

Status: **accepted on the POCO on 18 October 2026**. The owner confirmed the code 11 countdown fix and reported all Stage 6 functions done. `Sung-0.7.1-stage6-debug.apk` (code 11) remains the accepted APK. The app ID and signing key are unchanged; no USB connection was required.

Countdown retest procedure: Set a custom one-minute sleep timer in Now Playing. “Pauses in 1:00” should count down through `0:59`, `0:58` and so on while the screen is open, then playback should pause at the end. Lock and unlock the phone during another timer and confirm the label resumes at the correct remaining time. Confirm Likes, playlists and settings remain after updating. The owner reported the corrected build working; individual subchecks were not reported separately.

The original Stage 6 checklist follows for any regression checks you want to repeat:

1. Confirm Likes, playlists, lyrics settings/offsets and the current queue remain after updating. A cold app restart should restore the queue paused.
2. In Now Playing, set 0.5×, 1.2× and 2.0×; toggle Preserve pitch and reset to 1.0×. Switch songs, seek, open synced lyrics and restart the app. Check the displayed setting and actual sound.
3. Start a 1-minute timer, lock the phone and check that playback pauses. Replace and cancel a timer. Try the optional final-10-second fade, then cancel it and confirm normal volume returns. Test end-of-track before the next song, manual Next cancellation, and end-of-queue with repeat off/on.
4. Listen to a recording over 20 minutes long for at least 30 seconds. Reopen it and use the saved Resume position, then try Start over. Confirm Private mode creates no new resume point. Check that normal queue recovery is still paused after restart.
5. Save a session containing duplicate songs, queue edits, repeat/shuffle, a nonzero position and nondefault speed. Preview, rename and restore it after switching queues; it must start paused at the correct occurrence/position. Delete the saved session and confirm the live queue, Likes and playlists remain. Also open Saved sessions from Library with no track playing.

Report any failure with the step, what you did, and what happened. Stage 6 remains pending until your phone results are recorded.
