# Stage 2 playback code guide

The UI can be redesigned without moving playback logic. There is one live engine, owned by `SungPlaybackService`, and screens use `PlaybackControl` through `PlaybackConnection`.

## Where to work

| Location | Responsibility |
| --- | --- |
| `core/model/.../Queue.kt` | Queue entries, repeat mode, pure ordering/editing rules and the persistence contract. |
| `core/playback/.../PlaybackCoordinator.kt` | Track resolution, cancellation, user playback intent, automatic advance and queue commands. No Android UI dependencies. |
| `core/playback/.../PlayerEngine.kt` | Small audio-engine contract and immutable UI state. |
| `core/playback/.../ExoPlayerEngine.kt` | Actual Media3 audio, focus/noisy handling, network wake locks and stream error mapping. |
| `core/playback/.../PlaybackPersistence.kt` | Paused restoration and ordered, conflated queue/position checkpoints. |
| `core/playback/.../session/SessionPlayer.kt` | Adapts the coordinator to Media3's standard `Player` protocol. |
| `core/playback/.../session/QueueMediaItems.kt` | Durable catalog metadata across the session boundary; no signed URLs. |
| `core/playback/.../session/SessionStatus.kt` | App-only status sent as session extras, including Undo and storage errors. |
| `core/data/.../db/` | Room entities, DAO and database. Java annotation processing keeps this small schema independent of a new Kotlin compiler plugin. |
| `core/data/.../queue/RoomQueueStore.kt` | Maps database rows to model snapshots on the IO dispatcher. |
| `core/data/.../settings/SettingsRepository.kt` | DataStore preferences and migration from Stage 1's SharedPreferences. |
| `app/.../playback/SungPlaybackService.kt` | Creates/releases the engine, session and persistence observer. |
| `app/.../playback/PlaybackSessionCallback.kt` | Controller permissions, queue input and custom queue commands. |
| `app/.../playback/PlaybackConnection.kt` | Screen-side MediaController lifecycle and command forwarding. |
| `app/.../playback/SessionStateMapper.kt` | Converts controller state into typed screen state. |
| `app/.../app/AppContainer.kt` | Shared provider, repositories and application-lifetime background writes. |
| `app/.../app/SungViewModel.kt` | Catalog requests and selecting a snapshot of loaded songs; does not own or release audio. |
| `app/.../feature/queue/QueueScreen.kt` | Simple queue list and actions. |
| `app/.../feature/player/QueueControls.kt` | Previous/next, repeat/shuffle and queue navigation. |

Paths above are relative to the repository. Kotlin package roots are `com/karthi/sungmobile` and `com/karthi/sungmobile/core`.

## Adding a screen

Observe `PlaybackControl.state`. Call `select(songs, selectedIndex)`, `toggle()`, `seekTo(positionMs)`, `enqueue(track, next)`, or the queue commands. The app supplies the implementation through its existing ViewModel/container wiring. A screen must not construct ExoPlayer or call the YouTube resolver directly.

Use `QueueEntry.id` for queue actions and list keys. `CatalogItem.id` identifies a song and can appear multiple times. For new persistent library features, add repositories to `core:data`; keep their models in `core:model` and their Compose screens in `app/feature/<feature>`.

Changes to the database require an explicit Room migration and an updated exported schema under `core/data/schemas`. Do not enable destructive migration as a shortcut.

## Playback behavior

- Selecting a search result queues the currently loaded songs, starting at the chosen result. Later searches do not modify that queue.
- Play next inserts after the current occurrence; Add to queue appends. Adding to an empty queue leaves it paused.
- Removing the current song chooses its next surviving successor, or the last remaining entry when none follows. It keeps the current play/pause intent. Removing all entries stops audio.
- Undo restores the last removal/clear. It preserves the current successor rather than jumping back to the removed song. A new insertion, reorder, shuffle change or queue replacement invalidates that Undo.
- Shuffle randomizes upcoming entries and retains the current entry. Disabling it restores the stored baseline order; a manual reorder defines a new baseline.
- Repeat one applies to natural completion. Manual Next still advances. Repeat all wraps at the end. Without repeat, queue completion stops.
- Stop stops audio and resets the current position, retaining the queue for explicit Play.
- Leaving the activity or releasing its controller does not stop the service. Reopening the app connects to the existing player.
- Process restoration keeps the queue/position paused. Checkpoints are taken on queue/intent changes and roughly every five seconds during playback, so an abrupt process kill can lose a few seconds. Force-stop is not treated as permission to restart audio.
- Stream expiry receives one refresh attempt per selection/retry. Other failures retain the queue and expose Retry or manual Next. Unavailable items do not trigger an unlimited automatic skip loop.

## Tests

Pure queue/coordinator tests cover order, duplicates, transitions and cancellation. Room tests use a real on-disk database and verify reopen/transaction rollback. Media3 tests exercise both the player adapter and an actual session/controller connection with only network/decoding replaced. Compose tests check action reachability and duplicate-entry targeting.

These checks do not establish physical-device audio focus, OEM background behavior, lock-screen controls or real YouTube playback. Use the Stage 2 phone checklist for those checks.
