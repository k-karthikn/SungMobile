# Stage 8 manual POCO check

Install [Sung-0.9.0-stage8-debug.apk](Sung-0.9.0-stage8-debug.apk) **over code 13**. Do not uninstall or clear app data. No USB connection is needed.

1. Confirm Likes, playlists, saved sessions, lyric settings and paused queue restoration survived. Search and play a YouTube song; confirm the mini-player and lock-screen controls still work.
2. Play a queue of at least three songs and listen to natural transitions. This build prepares the next URL but does not guarantee gapless audio. Near a song's end, try Pause, manual Next, seek, remove/reorder the next song and Stop. The visible and audible next song should agree, once only.
3. Try repeat one/all/off and shuffled duplicate songs. Enable **Smart shuffle** in Now Playing, then turn Shuffle on and inspect the queue. Turn Shuffle off and confirm original order returns. Try queue edit and Undo.
4. Lower one song to −6 dB using **This song's volume**. Confirm the next song is at its own level. Start a one-minute sleep timer with its final fade, cancel it, and confirm the chosen song's level returns without a jump. Also try speed/pitch and a headset or audio-focus interruption.
5. Enable **Related autoplay**, use a one-song queue with repeat off and let it finish naturally. If YouTube supplies usable related items, up to five should join the queue; an empty result should simply stop. Turn it off and repeat. In Private mode it should not request related songs. Try once without network; it should not loop or crash.
6. During a normal listening session, compare transition delay and mobile-data/battery use with code 13. There is no audio prebuffer, download or device-file access in this build.

The owner later reported the delivered Stage 8 functions working on the POCO. Keep this list for regression checks and report any wrong song, duplicate playback, volume jump, data loss, crash, unexpectedly high data/battery use or repeat/timer regression.
