# Player and Queue phone check — 0.12.4 (code 24)

1. Install over production code 23 (`com.karthi.sungmobile`) without uninstalling. Confirm your production Likes, playlists and settings remain. If you only have the older `.stage0` debug app, this production package installs separately with a fresh library.
2. Play a song. Confirm the previous/current/next artwork is correct and tapping a neighbor switches to that queue entry. Test the slim seek wave and −10/+10 steps; the steps should be small visually but easy to tap.
3. Test Play/Pause, Previous, Next, Repeat off/all/one and Shuffle. The pressed/selected state should be clear, and the controls should fit in portrait, large text and both theme colors.
4. Swipe up from the Queue handle; confirm the sheet opens and the compact current-song header and queue appear. Tap a queue song, scroll the queue, use Move/Remove, Undo, Clear upcoming and Save queue as playlist. Swipe down, tap the handle and press Back to close the sheet. The player should remain open.
5. Confirm Lyrics, speed, sleep timer/countdown, song Like/playlist actions and More options remain reachable by scrolling the player. Pause playback, leave the app and return: the wave should not keep animating while paused or backgrounded. Check Reduced Motion if enabled.

Report any clipping, gesture conflict, incorrect queue order, lost data or playback regression. Device verification is left to the owner.
