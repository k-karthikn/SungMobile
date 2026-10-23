# Baseline stabilization — manual POCO check

Install `Sung-0.10.1-stabilization-debug.apk`, code 16, over the accepted code 15. Keep app data; no uninstall or USB connection is needed.

1. **Upgrade and restart:** confirm Likes, ordinary/smart playlists, local versions, saved sessions, lyrics settings and queue remain. Close/reopen the app; the restored queue should stay paused until you play it. If you force-stop the app, reopen it and explicitly resume.
2. **One normal listening session:** play a multi-song queue, lock the phone, use notification controls, and test a headset disconnect or another app taking audio focus. Try repeat/shuffle and a one-minute timer. Look for wrong-song transitions, unwanted restarts or an incorrect countdown.
3. **Network loss:** turn off connectivity during playback and try search/lyrics. The queue should remain; an error should allow retry. Restore connectivity and resume, checking that the song retains an appropriate position.
4. **Privacy:** enable Private mode and check Library Recent, Home recent mixes, history-based smart lists and statistics. History-derived content should be hidden while deliberate Likes remain. Turn Private mode off and confirm saved history returns; clearing history should remove derived results.
5. **Cleanup:** create duplicates, keep a later copy by selecting the first occurrence instead, and confirm only the reviewed occurrences are removed. Run an availability scan, cancel it, and return later. Network errors must stay unknown; Retry unknown must not remove songs. Restore a playlist version and verify duplicate order.
6. **Navigation and lyrics:** quickly leave a version preview, Statistics or Lyrics while it is loading. Old content should not reappear on another screen. Reopen Lyrics and verify normal loading, sync and cache controls still work.

Report any crash, unexpected playback, data loss, wrong removal, privacy inconsistency or unusual sustained battery/data use. A short “all passed” is enough if these checks work. Manual acceptance of this build is pending.
