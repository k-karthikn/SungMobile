# Stage 9 implementation report

Built 19 October 2026 as `0.10.0-stage9`, version code 15. Host verification passed. The owner subsequently reported everything working on the POCO, accepting the delivered Stage 9 behavior. No separate performance measurements or item-by-item phone results were supplied.

## Delivered behavior

- Saved smart playlists use typed, bounded combinations of Likes, qualified play count and recent-play windows. They can be previewed, edited, played, shuffled or saved as an ordinary editable snapshot. Rules recalculate from the local library; history-derived results are hidden immediately in Private mode. Clearing listening history removes those derived matches.
- Ordinary playlist cleanup identifies duplicate occurrences by canonical YouTube video ID and preselects only later occurrences. A user may change the selection and sees before/after counts before confirming removal. On-demand YouTube availability checks run in batches of at most 100 with progress and cancellation. Only provider-confirmed unavailable occurrences become selectable; network, rate, authentication and extraction failures are unknown and never auto-remove songs.
- Before a name, entry or order edit, the app saves the current ordinary playlist as a local version. The latest 20 versions per playlist retain the name and ordered occurrence IDs/track keys. Restore previews the version, saves the current state first, then applies the selected state in one Room transaction. Playlist deletion also deletes its versions.
- Listening statistics show qualified YouTube plays, recorded listening time, top songs and paged recent periods derived from playback passes using a 30-minute gap. Private mode hides the screen's history-derived content; clearing listening history resets it.

The active app remains YouTube-only. The Stage 7 device-audio/download prototype remains outside build source sets. App-data backup/export/import and new playlist-link exchange were excluded by the owner; existing Stage 4 YouTube playlist link opening is retained. No new sign-in, audio storage, permission or design-polish pass was added.

## Data and verification

Room schema advances from 4 to 5 with an explicit migration adding `smart_playlists` and `playlist_revisions`. Existing queue, Likes, ordinary playlists, listening history and settings tables are left intact. The migration path from older supported schemas continues through schema 4. Tests exercise the code 14 schema migration, smart rule/privacy changes, qualified stats, duplicate occurrence restore, bounded version retention and safe availability classification. The full offline host suite ran **159 cases: 153 passed, six opt-in live probes skipped, zero failures or errors**. App lint passed with zero errors and ten dependency/tool-version notices; the debug APK build passed. Live YouTube probes and device tests were not run for this stage.

The APK retains application ID `com.karthi.sungmobile.stage0` and the Stage 8 debug certificate SHA-256 `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`. This allows an in-place update over code 14 without clearing phone data. The owner's manual test should use [the Stage 9 checklist](../Stages/Stage9_Results/PHONE_TEST_CHECKLIST.md). The APK and its checksum are in `Stages/Stage9_Results/`.
