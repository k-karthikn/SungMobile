# Phone check — 0.12.9 (code 29)

Install over production `com.karthi.sungmobile` without uninstalling. Confirm Likes, playlists, history, sessions, and settings remain.

- Open Library → Local songs. Confirm the list starts immediately below the compact filter/refresh row, with no duplicate heading or extra-folder controls.
- Switch to another Library tab and back. The existing songs should remain visible without a full loading state. Copy a new file into `Music/SungMobile/` and tap the refresh icon; confirm it appears.
- Manage a picked folder in Settings → Local songs, then return to the Local songs tab and confirm the list updates.
- On Android 15, download an online song. Confirm progress, a completed notification, and the file in `Music/SungMobile/`. If it fails, note the new notification message and song title; the host could not reproduce this without a connected phone.
- Cancel a second download and confirm no partial song appears. Leave the app during a download and confirm it continues.

Phone layout, storage, and background-transfer acceptance remain pending.
