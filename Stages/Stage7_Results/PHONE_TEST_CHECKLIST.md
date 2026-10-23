# Stage 7 manual POCO checklist — historical

The owner deferred this stage before phone acceptance. Use the online-only code 13 checklist in `Stages/Online_Only_Results/` for current testing. The steps below are retained only as a future reference.

Install `Sung-0.8.0-stage7-debug.apk` **over** the accepted code 11 app. Keep its data. No USB connection is needed for this checklist.

1. Open Library and confirm existing Likes, playlists and saved sessions remain. Check existing lyrics and playback settings, then restore a queue paused.
2. Open **Device music**, allow audio access, and tap **Scan now**. Check that a song already in `Music/` and one in a nested folder appear and play. Search by its title, then try queue, Like and Add to playlist. Turn on airplane mode and play a device song, seek and skip.
3. Exclude a subfolder and scan: its songs should become unavailable or disappear from the playable list without deleting files. Remove the exclusion and scan again. Add another folder with the system picker. Turn off **Include Music/ by default** if you want only chosen folders; turn it back on afterward.
4. On Wi-Fi, download one YouTube song from Now Playing or its menu. In Downloads, watch state/bytes until **Complete**. Confirm the file in `Music/SungMobile/` using a file manager or another music app. In airplane mode, play the same YouTube song through SungMobile; it should use its saved audio.
5. Choose a different download folder in Device music, then download a second song. Check that only the new file uses the changed destination. Cancel a third download and retry it; no partial file should appear as a finished song. Delete a completed download from Downloads and confirm its device file goes away while Likes and playlists stay.
6. If practical, disable Wi-Fi during a transfer, return later, and check that the job pauses/retries and finishes only with an allowed network. Check that the mini player remains visible on Device music and Downloads.

Please report the Android version, whether code 11 data survived the update, and any failed step with the song/folder used and what appeared on screen. A phone test is required before Stage 7 acceptance.
