# Stage 9 manual POCO check

Install [Sung-0.10.0-stage9-debug.apk](Sung-0.10.0-stage9-debug.apk) **over code 14**. Do not uninstall or clear app data. USB is not required.

1. Confirm existing Likes, ordinary playlists, lyric settings, saved sessions and paused queue survive. Play a YouTube song and confirm mini-player and background controls still work.
2. In Library, create a smart playlist. Preview a rule combining Likes and qualified plays, then save it. Like and listen to songs; the list should update. Play/Shuffle should use a fixed queue snapshot even if Likes later change. Save the current smart songs as an ordinary playlist and edit that copy.
3. Open History & privacy. Private mode should hide history-based smart results and statistics while a Likes-only smart list remains. Turn Private mode off, then clear listening history; history-based results and statistics should reset.
4. In an ordinary playlist with duplicate YouTube songs, open **Cleanup & versions**. Check that only later occurrences start selected. Change selection, inspect before/after counts and remove one occurrence; other copies should remain.
5. Run the availability check with internet and cancel once. Try again without internet; songs should remain, errors should be counted as unknown and Retry unknown should be offered. Only a confirmed unavailable song should appear as a removal candidate, and removal still requires selection and confirmation.
6. Rename and reorder the playlist, inspect a local version and restore it. Check the old name, duplicate occurrences and exact order return. Restart the app; smart rules, versions and statistics should remain.

Report any data loss, wrong selection, temporary network failure treated as unavailable, version mismatch, crash or playback regression. The owner subsequently reported everything working; this checklist remains for regression checks.
