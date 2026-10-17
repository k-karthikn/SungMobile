# Stage 3 POCO M6 Pro 5G check

Accepted on 13 October 2026 based on the user reporting every checklist item working without problems. This records manual user verification.

Install `Sung-0.4.0-stage3-debug.apk` over Stage 2 **without clearing Sung's storage**. This checks migration and persistence. The Stage 3 debug package updates the same `com.karthi.sungmobile.stage0` application ID.

- [x] Existing Stage 2 queue, current song, repeat/shuffle mode, and approximate position restore after update.
- [x] Like a YouTube result; it appears in Library → Liked songs. Unlike it and verify it leaves Likes only.
- [x] Create, rename, pin and delete a playlist.
- [x] Add a song twice normally (second add is skipped); explicitly allow a duplicate and confirm each entry can be reordered or removed independently.
- [x] Select multiple songs and try bulk Like/Unlike, Add to playlist, Play next, Add to queue, and playlist removal.
- [x] Play and shuffle all songs in Likes and a playlist. Tap a song in the middle and confirm playback continues through the saved collection.
- [x] Save the queue as a playlist. Confirm it includes already-played songs and duplicate entries in the displayed order without changing playback.
- [x] Leave a song playing with the screen locked past its qualification time. Check Library → Recently played after returning.
- [x] Pause, seek, or let playback buffer during another test song. Those intervals should not add listening time.
- [x] Submit two searches. Confirm recent queries can be recalled, removed, and cleared.
- [x] Turn listening/search history off, clear each history, and turn Private mode on. Restart the app and confirm those settings persist.
- [x] While Private mode is on, submit searches and listen to music; no new history should appear. Likes and playlists should still save.
- [x] Confirm Private mode help states queue and position restoration still persist.
