# Local songs and downloads

The 0.12.11 (code 31) production APK (files removed 30 October 2026) includes local-song lyrics. Its download behavior carries forward the Android 15 fix verified with code 30, which saved an `.m4a` song successfully on the connected phone.

In **Library → Local songs**, SungMobile scans `Music/SungMobile/` and its subfolders. A compact refresh icon beside the filter picks up files copied in by another app; recent scan results stay visible when you switch tabs. You can grant music access for those files, and add other folders through **Settings → Local songs**. Folder selection uses Android's system picker; **Remove access** only removes that folder from SungMobile, leaving files untouched. Local tracks play and queue in the existing player. Likes, playlists, and listening history remain online-song features.

The **Download** action in online song menus starts a fresh transfer. It selects an audio format compatible with Android's Music collection; online playback can still use WebM/Opus. This distinction fixes a phone where MediaStore rejected `audio/webm`. A notification shows progress or an unknown-size status and has a Cancel action. An expired signed stream is resolved once more before failing, and failure notifications give a short, safe reason. Completed files appear in `Music/SungMobile/`; delete them through the phone's file manager. A currently running request for the same song is reported, while a later intentional request can create another file. There is no download database, inventory, or destination preference. Downloads require Android 10 (API 29) or later; Local songs and folder picking remain available from the app's API 26 minimum.

On the connected Android 15 phone, a code 30 test build saved `Kutti Story (From "Master")` as a 4,865,280-byte `.m4a` file in `Music/SungMobile/`. Further manual checks remain for picked folders, cancellation, background completion, and other source songs.

## Lyrics for local songs (code 31)

Open a local song in Now Playing and tap **Lyrics**. SungMobile checks a same-name `.lrc` or `.txt` file beside the audio when it has folder access, then reads supported embedded lyrics from MP3 ID3 `USLT`, FLAC Vorbis comments, or M4A `©lyr`. Timed `.lrc` lines remain synced and support tapping to seek and timing offsets. Local lyric files work without internet, including in private mode.

On Android 10 and later, audio permission alone does not grant access to neighboring text files. For sidecar lyrics, add the containing folder in **Settings → Local songs**; the app reads through that folder's Android picker grant. On Android 8–9, sidecars under Music can be read with legacy storage permission. Embedded lyrics are read from the audio content URI in either case. Unsupported embedded tag types may still need a sidecar.

If the file has no readable lyrics, **Search LRCLIB online** on the Lyrics page enables the existing LRCLIB setting. It sends the song title, artist, and duration to LRCLIB, validates the match, and caches found text when lyric caching is on. Private mode continues to skip that request. The on-device source takes precedence when it provides synced lyrics; a validated synced LRCLIB result can upgrade plain local text.
