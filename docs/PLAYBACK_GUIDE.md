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
- Repeat one applies to natural completion. Manual Next still advances. Repeat all wraps at the end. Without repeat, queue completion stops unless Stage 8 related autoplay is enabled and returns usable provider items.
- Stop stops audio and resets the current position, retaining the queue for explicit Play.
- Leaving the activity or releasing its controller does not stop the service. Reopening the app connects to the existing player.
- Process restoration keeps the queue/position paused. Checkpoints are taken on queue/intent changes and roughly every five seconds during playback, so an abrupt process kill can lose a few seconds. Force-stop is not treated as permission to restart audio.
- Stream expiry receives one refresh attempt per selection/retry. Other failures retain the queue and expose Retry or manual Next. Unavailable items do not trigger an unlimited automatic skip loop.

## Tests

Pure queue/coordinator tests cover order, duplicates, transitions and cancellation. Room tests use a real on-disk database and verify reopen/transaction rollback. Media3 tests exercise both the player adapter and an actual session/controller connection with only network/decoding replaced. Compose tests check action reachability and duplicate-entry targeting.

These checks do not establish physical-device audio focus, OEM background behavior, lock-screen controls or real YouTube playback. Use the Stage 2 phone checklist for those checks.


## Stage 3 local library

The Room database is now version 2. `DatabaseProvider.open` always registers `LibraryMigration`; never use a destructive fallback for user queue or library data. Keep the version 1 schema under `core/data/schemas` as the migration source. The migration adds `playbackPassId` to the queue header and creates library/history tables. Appearance remains in DataStore.

Use `CatalogItem.libraryKey()` only for validated YouTube songs. A liked video is unique by video ID, while `PlaylistEntryEntity.id` gives every playlist occurrence its own stable identity. Read list pages through `LibraryRepository.songs` and `playlists`; use `snapshot` for an explicit Play action. A playback snapshot is independent of later playlist edits. Bulk writes belong in one repository transaction.

The service owns `HistoryRecording`; UI and activities must not measure listening time. It uses monotonic elapsed time and persists bounded checkpoints. Repositories compare the captured history epoch inside each write transaction, so an old recorder or search submission cannot restore data cleared or suppressed by a newer privacy setting. Private mode affects listening and search history; queue recovery still persists.

Library browser/actions and history each have separate ViewModels. `LibraryHost` supplies shared song actions to Search, Now Playing and Queue through a CompositionLocal. Keep catalog requests in `SungViewModel`, playback mutations behind `PlaybackControl`, and UI state out of Room DAOs.

See [STAGE3_REPORT.md](STAGE3_REPORT.md) for feature behavior and the phone acceptance checklist.

## Stage 4 catalog and Home

`SungViewModel` now owns Search only. Use `CollectionViewModel.open(CollectionRoute(...))` for online details and `LinkIntakeViewModel.receive` for pasted/shared links. Neither starts audio. The activity supplies `SavedStateHandle` through its ViewModel factory; do not create feature ViewModels in composables.

For a complete online collection action, call `CollectionActionsViewModel.begin(details, action)`. It loads through the pure `CollectionBatchLoader`, then commits a validated snapshot through the existing `PlaybackControl`. Handle partial batches through its dialog; do not directly play a first page while labelling it as the whole playlist. `CatalogEntry.id` identifies a loaded occurrence and must be used for collection row keys. `CatalogItem.id` still identifies the song and may repeat.

`MusicLinks.extract(text)` returns validated destinations, including separate song/playlist choices and a bounded starting position. Show the choices before calling `CollectionViewModel.open`. Use `PlaybackControl.playAt` only after a user Play action for a timestamped single-song preview.

Home reads `AppContainer.home` (a `RoomHomeRepository`) through `HomeViewModel`. This query shares the existing database and privacy policy. Build future recommendation/statistics features on those privacy barriers; do not maintain a separate unbounded listening cache in the UI. The local mix function is pure and deterministic for a candidate set and seed.

Provider mappings, details and continuation callbacks are isolated under `core:youtube/catalog`; `StreamExtractor` retains audio resolution. A provider-specific improvement belongs there, behind the catalog contracts, rather than in a screen or playback service. Check [STAGE4_REPORT.md](STAGE4_REPORT.md) for current capability limits.

## Stage 6 playback convenience

`PlaybackCoordinator` still owns the single live queue/player state. `ExoPlayerEngine` applies service-owned speed and per-player fade volume. `SleepTimerController` uses a monotonic deadline and intercepts natural completion before automatic advance; it is canceled on process death while the existing queue restores paused. App-only speed/timer commands and state cross the Media3 session through `PlaybackSessionCallback`, `SessionStatus`, `SessionStateMapper` and `PlaybackConnection`. Screens never construct a timer or an audio engine.

`LongResumeRecorder` counts actual playing time for recordings of at least 20 minutes, checks Private mode before writing, and stores points through `ConvenienceRepository`. That repository uses bounded atomic private files for per-video resume points and named saved sessions, separate from the Room active queue/library and DataStore speed preference. A saved session retains duplicate queue-entry IDs and the canonical unshuffled order. Its Restore action runs in the service, replaces the active queue paused and reports completion to the UI. Library also opens Saved sessions when no track is playing. See [STAGE6_REPORT.md](STAGE6_REPORT.md) for behavior, limits and POCO checks.

## Stage 8 online playback

`PlaybackCoordinator` prepares at most one next occurrence's signed URL near natural completion. Preparation is transient, tied to the occurrence ID and queue state, and canceled by pause, seek, manual selection or a relevant queue/mode change. `ExoPlayerEngine` remains a single player and does not prebuffer audio; a prepared URL only avoids a late extractor request. Do not describe this as guaranteed gapless playback.

`QueueState.smartShuffle` is a pure best-effort reorder of upcoming entries; it retains duplicate occurrence IDs and the saved original order. DataStore stores the off-by-default smart-shuffle and related-autoplay switches plus bounded per-video volume reductions. The service applies a song's saved gain before loading its stream, multiplies it with the sleep-timer fade, and never changes system volume. The YouTube adapter obtains related items from the provider's `StreamInfo.relatedItems`; the coordinator requests once at natural queue end, filters existing songs, and suppresses it in Private mode or when the timer wins. The UI stays in focused `PlayerAdvancedControls` within Now Playing. See [STAGE8_REPORT.md](STAGE8_REPORT.md) for limits and manual checks.
