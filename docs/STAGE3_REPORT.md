# Stage 3 — Persistent local library

Status: implementation complete; host checks passed on 13 October 2026. All 93 runnable test cases passed; four live YouTube network tests were skipped. Lint reported zero errors and 13 dependency version notices. The APK is ready for the POCO upgrade and acceptance check, which is still pending.

## What is included

- Version 2 Room database with an explicit additive migration from the shipped version 1 queue schema. Queue order, duplicate entries, current item, repeat/shuffle mode, and position remain intact. Appearance continues to use its existing DataStore settings and is not moved into the library database.
- Canonical YouTube video keys for Likes, metadata updates that preserve fields missing from partial results, local playlist creation/rename/delete, pins, duplicate-aware adds, entry-specific removal and reorder, filtering, sorting, and incremental loading.
- Library screens for Liked songs, local playlists, and Recently played; bulk selection applies to the items currently loaded. Song actions are available in Search, Now Playing, Queue, and library rows.
- Play/Shuffle, Play next/Add to queue, and Save queue as playlist. Playlist playback snapshots the full saved list; saving the queue copies its current displayed order, including played entries and duplicates.
- Service-owned listening checkpoints. Count only playing time on a monotonic clock; pause, suppression, buffering, and seeking do not add time. A pass is stored at most once and can continue after restoration. Recent songs are unique, while listening events remain separate for later statistics.
- Search history stores submitted queries and category, refreshes duplicate recency, keeps 50 distinct entries, and supports individual removal and clear.
- Separate listening/search recording switches, Private mode, clear confirmations, and epoch checks that reject writes captured before a privacy or clear action. Help text explains that Private mode does not suppress queue/position restoration.

## Code map

- `core/model/.../library/`: IDs, models and repository contracts.
- `core/data/.../db/`: Room entities, DAOs, schema and migration; `library/` and `history/` contain the repositories.
- `core/playback/.../history/`: clock-based recorder, without a database or UI dependency.
- `app/feature/library/`: browser/actions ViewModels, list, playlist rows, dialogs and shared cross-screen actions.
- `app/feature/history/`: privacy/history controls and recent searches.
- `app/playback/SungPlaybackService.kt`: owns the recorder alongside playback; the service still owns the only audio engine.

For implementation conventions and build commands, see [PLAYBACK_GUIDE.md](PLAYBACK_GUIDE.md).

## Verification

- Room repository tests cover canonical likes, metadata retention, independent duplicate entries, ordering, invalid transaction rollback, paging/filtering, private/clear barriers and the 50-query cap.
- The migration test creates a database from the exported Stage 2 schema, opens it with the version 2 Room builder, and checks persisted queue data after migration.
- Playback tests cover collection shuffle/bulk queue insertion and listening qualification, pause gaps, and checkpoint restoration.
- The app regression suite exercises Media3 session/controller behavior, Search, queue, appearance and the existing ViewModel flows.
- Final test/lint/build output: see `Stages/Stage3_Results/build.log`; exact counts and APK SHA-256 are in `Stages/Stage3_Results/build-summary.json`.

## POCO acceptance checklist

Install the update over Stage 2 without clearing app data. Then check:

1. Confirm the previous queue and position restore.
2. Like the same YouTube song from Search and Now Playing; confirm it appears once under Liked songs and remains after restart.
3. Create a playlist, add a duplicate explicitly, reorder/remove one occurrence, and confirm the other occurrence remains.
4. Play and shuffle a playlist; edit or delete it while playing and confirm the active queue remains a snapshot.
5. Save a queue to a playlist and confirm earlier entries, duplicates, and order are included.
6. Listen past the qualifying time with the screen off; check Recently played. Pause or seek during another song and confirm those intervals do not count.
7. Submit searches, remove/clear history, test the recording switches and Private mode, then restart to confirm settings persist.

Stage 7 audio downloads are not part of this milestone: saved metadata can be browsed offline, but streaming still requires YouTube access.
