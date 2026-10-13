# Stage 3 — Persistent local library

Prepared: **12 October 2026**.

Status: implemented and host-verified on 13 October 2026. The Stage 2 phone-checklist checks were reported successful by the user. POCO Stage 3 upgrade and feature acceptance remains pending; see [STAGE3_REPORT.md](STAGE3_REPORT.md).

## Outcome

Replace the Library placeholder with working local likes, playlists, pins and listening history. Add search history and connect library actions to Search, Now Playing and Queue. Keep the existing M3 foundation, simple lists/forms/dialogs, normal component animations and reduced-motion behavior. No custom design pass.

Everything stays on the phone without sign-in. Saved metadata can be browsed without a connection; playing audio still requires YouTube access until Stage 7 downloads. Stage 3 does not create or modify playlists in the user's YouTube account.

## Delivery order

| Part | Deliverable | Gate |
| --- | --- | --- |
| **3A. Library storage** | Stable YouTube track identity, track/like/playlist/playlist-entry/history records, repository interfaces, Room migration 1→2, history preferences. | Upgrade preserves the Stage 2 queue/position and appearance choices; transactions and identifiers are tested. |
| **3B. Likes and playlists** | Like/unlike from song menus and Now Playing; Liked songs; playlist create/rename/delete; add/remove/reorder entries; Play/Shuffle/Play next/Add to queue; queue-to-playlist. | Changes persist across restart; duplicate occurrences are independent; library edits leave an already-playing queue unchanged. |
| **3C. Library management** | Multi-select, bulk add/remove, pin/unpin local playlists, library text filtering and simple sorting. | Correct selected entries are changed in one transaction, including duplicates; cancelled dialogs do nothing; large lists are loaded incrementally. |
| **3D. Listening and search history** | Service-owned listening recorder, Recently played, submitted-search history, pause/clear controls and private listening. | Background plays count; failed playback, seeking, buffering and paused time do not inflate listening; privacy and clearing cannot be undone by pending writes. |
| **3E. Integration and acceptance** | Cross-screen state updates, complete error/empty/loading states, focused regression checks, update APK and a phone checklist. | Host checks pass and the user verifies upgrade/data persistence and library behavior on the POCO. |

These are manageable implementation parts of one Stage 3 milestone, rather than five unrelated app releases.

## Feature behavior proposed for this stage

### Likes and identity

- One like per YouTube video ID, even when the same video is reached through a short link, a music.youtube.com link or a normal watch link. Different uploads remain different songs; do not merge songs by title/artist.
- Reuse the existing validated YouTube-link parser at the library boundary. Give local records a provider/video key, while keeping queue-entry IDs and playlist-entry IDs independent.
- Like changes update Now Playing, search/queue menus and the library from repository flows.
- Unliking removes the like only. It does not remove playlist entries, history or the playing queue.
- Retain usable metadata when refreshes return missing fields. Do not automatically delete unavailable songs from the library. Playback shows the existing retry/skip state.

### Playlists and duplicates

- Local playlists have UUIDs. Names are trimmed, nonblank and limited to 120 characters. Names are labels, not identifiers; renaming never changes identity.
- Each occurrence has its own playlist-entry ID and order. Reordering/removing one duplicate cannot target another occurrence accidentally.
- Normal Add to playlist skips songs already present by default, reports the skipped count and offers an explicit add-duplicates choice. This also handles duplicate songs within a bulk selection.
- Save queue as playlist copies the entire displayed queue, including earlier entries, preserving its order and duplicates. Its label/help text must make the scope clear. It neither changes nor stops the queue.
- Playing a local playlist takes a snapshot. Subsequent playlist edits or deletion do not change active playback. Explicit Play replaces the queue; Play next/Add to queue modifies it only when requested.
- Shuffle play randomizes the collection once and establishes a consistent baseline/current entry; it must not accidentally reshuffle twice in UI and service layers.
- Delete playlist and bulk destructive actions show their consequences before confirmation. Cancelling makes no database change. Removing a playlist does not delete likes, listening records or shared track metadata still referenced elsewhere.

### Library screens and actions

- Library: Liked songs, local playlists with pinned ones first, and Recently played.
- Playlist detail: list, Play/Shuffle, add/rename/delete options and per-entry remove/move actions.
- Multi-select in local song lists: select all within the explicitly loaded/visible selection scope, deselect, add to playlist, queue actions and remove where appropriate. If a paged list offers select-all across the full collection, use a repository query and show the full selected count; do not imply unloaded rows are selected accidentally.
- Text filtering covers saved titles/artists and playlist names. Simple ordering uses saved playlist order, recently liked/played, and alphabetical choices where meaningful.
- Add to playlist uses a small dialog/sheet with existing playlists and Create playlist.
- All actions use shared repositories and the existing playback controller. No screen constructs an audio engine or invokes the resolver directly.

### Listening history and future statistics

Proposed qualification rule: add a song to listening history after **30 seconds of actual playback, or half its known duration, whichever is shorter**. For unknown duration, use 30 seconds.

- Measure elapsed time while audio is actually playing using a monotonic clock. Pause, buffering and focus suppression contribute no time. A seek changes media position, not accumulated listening time.
- The recorder belongs to the service lifecycle, so locked-screen/background listening follows the same rule as foreground listening.
- Model a playback pass explicitly. Track transitions and repeat/replay passes are distinguishable even when they use the same queue-entry ID. Stream refresh/retry and activity recreation do not create duplicate listening events.
- Give persisted events stable IDs and use idempotent updates. Retain listened milliseconds and timestamps, so Stage 9 can calculate statistics from actual activity instead of inferring counts from the Recently played list. Do not persist every progress tick as another row.
- Recently played shows unique tracks ordered by their latest qualifying listen. Repeat events remain distinct underneath for later statistics.
- Persist recording checkpoints at bounded intervals; document any small time-loss window after an abrupt process kill. Restoration must not double-count already-written activity.

### Privacy and clearing

- Listening history and search history each have a persistent recording toggle and separate clear actions. Turning recording off preserves earlier history; clearing deletes it.
- Private listening is a visible, persistent mode that suppresses both forms of history and their future personalization/statistics inputs. It survives an app restart until turned off. It does not disable deliberate likes or playlist edits.
- Entering private mode closes the public listening segment at that boundary and excludes all subsequent private time. Leaving it starts fresh accumulation; do not backfill private listening or search queries.
- Serialize privacy changes, clearing and history writes with a generation/barrier. Pending work from an earlier generation cannot recreate cleared rows or save private activity.
- Clearing listening history also removes its saved listening totals used by future statistics, but preserves likes, playlists and the active queue. Continued public listening can create a new event only after fresh qualification.
- Search history records submitted nonempty queries and result category, not every keystroke. Retrying an existing query updates its recency rather than adding duplicate rows. Keep the 50 most recent distinct query/category pairs; allow individual deletion and clear-all.
- Privacy controls concern library/search activity. Playback recovery still retains the queue and position under the Stage 2 behavior; artwork/network caches are not an incognito guarantee. State that distinction in the control's help text.

## Code organization

Use the existing modules; avoid a new global library manager.

| Location | Responsibility |
| --- | --- |
| `core:model/library/` | Track keys, library/playlist/history models and repository contracts. |
| `core:data/db/` | Separate entity/DAO files and explicit migration 1→2; retain the version-1 schema and export version 2. |
| `core:data/library/` | Track/likes and playlist repositories, bulk transactions, snapshot queries. |
| `core:data/history/` | Listening/search persistence, privacy barriers, retention/query rules. |
| `core:data/settings/` | Persistent history/private-listening preferences. |
| `core:playback/history/` | Small recorder/state machine consuming playback-pass events; no UI or Room dependency. |
| `app/playback/` | Service wiring between playback events, recorder and repositories. |
| `app/feature/library/` | Library ViewModel/state and list screen. |
| `app/feature/playlist/` | Playlist ViewModel/state, detail screen, selection and add/create dialogs. |
| `app/feature/history/` | Recent-listening and history/privacy controls. |
| Existing search/player/queue packages | Thin library actions and observed like state. |

The existing catalog ViewModel should continue to handle catalog requests. Do not grow it into the owner of library, playlists, privacy and playback. Shared track records store canonical metadata, not expiring stream URLs or provider objects. New dependencies are added only if necessary; the existing Room/DataStore/Media3 stack should cover the work.

## Verification and finish criteria

- Test an actual populated version-1 database upgrading to version 2: queue entries, duplicate IDs, order, repeat/shuffle, current position and appearance choices remain intact. No destructive fallback migration.
- Test like idempotence and canonical video identity across supported URL forms.
- Test playlist ordering, duplicate protection/explicit duplicates, bulk changes, rollback on failure and deletion/reference integrity.
- Test queue snapshots remaining independent after source-playlist edits or deletion.
- Test recorder thresholds with fake clocks: pause/buffer/focus loss, seeks, retries, repeat one, track switching, recreation and restored checkpoints.
- Test private-mode boundaries, paused history, clear operations with writes pending, search-history deduplication and retention.
- Test cross-screen state updates, action reachability, selection of the correct duplicate and meaningful error states. Use screenshots only where a changed layout needs inspection.
- Preserve Stage 2 playback regressions. Build/lint the APK and run a short manual POCO checklist for upgrade, likes/playlists/history, restart and background listening capture.

Deliver an update APK, test summary, migration notes and code-guide additions. Stage 4 remains catalog/discovery; advanced smart playlists/statistics/backup remain Stage 9. No new USB connection, account or tool installation is needed to review this plan.
