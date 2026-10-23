# SungMobile — functionality-first completion plan

Prepared: 11 October 2026.

Status: functionality-first implementation was authorized on 11 October 2026, including advanced features in separate milestones. Stages 2–6 were accepted from the owner's phone results. Stage 7 device music and downloads were built as code 12, then deferred before phone acceptance. The owner confirmed online-only code 13 and Stage 8 code 14 work on the POCO. Stage 8's capability limits are in `STAGE8_REPORT.md`. Stage 9 code 15 was implemented and the owner reported everything working on the POCO. Stage 10 was skipped at the owner's request. The baseline stabilization pass was host-verified as code 16. The owner then authorized page-by-page UI improvements, beginning with Home in code 17; final regression and release gates remain afterward. See `ui/home/REPORT.md`. App-data backup/import/export and new playlist-link exchange remain excluded.

This plan supersedes the design-led delivery order and visual-polish gates in `ANDROID_PLAN.md`. That document remains a record of the original direction and source/architecture decisions. Stage 0 and Stage 1 are retained as the starting point.

## 1. Delivery rules

- Prioritize working features, recovery, persistence and manageable code.
- “Skeleton UI” means complete, usable screens built from standard controls: lists, small artwork thumbnails, buttons, forms, tabs, dialogs and sheets. Every exposed action works. It does not mean dummy data or loading placeholders standing in for a feature.
- Reuse the existing M3 Expressive theme, fonts, icons and controls. New screens stay simple. Existing visuals can remain where they do not obstruct functionality; no aesthetic rework is required.
- Use standard press/selection, navigation, sheet and list-change animations. Respect reduced motion. Avoid custom animation systems, animated backgrounds, continuous decorative work, image generation and reference-matching iterations.
- Keep touch targets, readable text, scrolling, TalkBack labels, keyboard access and system insets correct. These are usability requirements even for the skeleton.
- Test each feature with deterministic behavior tests and relevant integration/device checks. Render screenshots only when needed to check an actual layout problem or reachability; do not repeat a full appearance matrix for unrelated backend changes.
- Each milestone produces a working APK and a short results/known-limitations note. Finish and verify that milestone before adding the next dependency-heavy feature.

## 2. Scope retained

YouTube / YouTube Music is the only active music source. Device-folder audio and downloading are deferred for later consideration. Optional LRCLIB lyrics fallback remains approved. The first account model is an on-device library without Google sign-in. Keep the existing open-source licensing decision and sideload-first distribution.

The expanded scope includes bounded next-track preparation, advanced autoplay, crossfade, loudness normalization, sleep timer, smart playlists, listening statistics, saved sessions, playlist version history. The owner skipped Stage 10 advanced lyric tools and the visualizer; these are not completion gates. App-data backup/import/export and new playlist-link exchange were removed from Stage 9 at the owner's request. Intentional downloads, offline management and device-folder audio are a separate **later-implementation backlog**; they are not gates for the current online-only app.

The owner deferred the later Stage 7 device-folder request after a prototype was built. This does not add Subsonic/Jellyfin/Cider, account synchronization, Cast or Android Auto. M3U file paths and Linux-specific desktop integrations are not automatically Android requirements. No additional artwork service or animated-cover service is introduced. Linux documents are reference material, not instructions to replicate desktop installation or platform internals.

## 3. Starting state and ownership

At plan creation, the code had a working foreground search/open/resolve/play/seek path and the Stage 1 UI foundation. Playback then belonged to an activity-scoped ViewModel and paused in `MainActivity.onStop()`. At that baseline there was no persistent library, live multi-track queue, background service, download manager or lyrics repository yet. Some artist browsing is unavailable through the current catalog adapter.

Keep these responsibility boundaries:

| Owner | Responsibility |
| --- | --- |
| `core:model` | Typed track/collection identities, queue-entry identities, lyrics and feature contracts. |
| `core:youtube` | Catalog capability checks, provider mapping, paging, stream resolution and provider error handling. |
| `core:playback` | Service/session, live player and queue, playback commands, focus, recovery, later DSP/handover. |
| `core:data` | Room repositories, migrations, library/history/snapshots, DataStore preferences, lyrics/cache persistence. |
| Device-audio and download infrastructure | MediaStore/SAF folder scanning, persistent download jobs and shared-file inventory, isolated from the single playback owner and ordinary caches; package/module choice made when this milestone starts. |
| `core:designsystem` | Existing shared Material components and tokens only. |
| `app` | Dependency assembly, feature ViewModels, navigation and simple screens. |

The service is the single owner of live playback. UI observes it through a controller; opening, closing or recreating a screen must not create or release a second player. Repositories expose immutable state. Storage/network work stays outside composables. Give duplicate occurrences of a song independent queue/playlist-entry IDs. Persist durable IDs and metadata, never expiring stream URLs.

Use a small application-level dependency container initially. Split the current combined ViewModel as feature ownership requires; avoid a new all-purpose manager. Check new dependencies against the pinned Android/Kotlin build before adopting them.

## 4. Functional milestones

Stages 0–1 remain completed work. Stages 2–6 are reordered around functional dependencies; Stage 7 remains deferred and Stage 10 is skipped. Stage 9 is phone-accepted. Stage 11 begins with a baseline stabilization pass before UI work and finishes with regression checks afterward.

| Stage | Working result through skeleton screens | Acceptance gate |
| --- | --- | --- |
| **2A. Durable playback contracts** | Queue entries, command/state contracts, minimal Room queue/position storage, settings repository, and application/service dependency lifetimes. Migrate existing appearance choices when moving them into the shared settings store. | Duplicate tracks remain distinct; queue snapshots restore accurately; migration retains existing choices. |
| **2B. Background playback and queue** | Media service/session, notification and lock-screen/headset controls, background/screen-off playback, automatic next song, previous/next, repeat off/all/one, shuffle; play next, append, reorder, remove, Undo and clear upcoming entries. Resume a saved queue explicitly. | POCO tests cover Home/lock, audio-focus interruption, unplug, activity recreation, rapid switching and expiry recovery. Exactly one active playback owner. Queue ordering and completion behavior pass tests. |
| **3. Persistent library** | Likes, playlist create/rename/delete, track add/remove/reorder, multi-select, duplicate handling, recent listening, search history, pins, clear/pause history and queue-to-playlist. | Restart preserves edits; transactions prevent partial changes; removal/reorder never changes the wrong duplicate or corrupts playback. Private listening does not feed history, later statistics or mixes. |
| **4. Catalog and discovery** | Debounced/cancellable search, all supported result kinds, paging, pasted/shared YouTube links, album/playlist/artist details, Play/Shuffle/queue actions, basic real Home sections and local mixes. Close provider capability gaps with an isolated YouTube adapter where feasible. | Every visible section has real data and actions. Search cancellation, link validation, pagination, partial content, restricted/unavailable items and offline retry work. Artist capability is tested explicitly. |
| **5. Lyrics and essential settings** | YouTube lyrics where available, optional matched LRCLIB fallback, synced/plain/unavailable states, current line, tap-to-seek, manual scrolling/return-to-current, saved text size and timing offset. Content preferences, lyrics privacy, cache controls, About/licenses and functional color-source settings. | Track switches cancel stale requests; incorrect matches are rejected; plain text never receives invented timestamps; settings and per-track offsets persist. Cache clearing does not erase the library or saved downloads. |
| **6. Playback convenience** | Playback speed/pitch-preservation setting, sleep timer by duration/end of track/end of queue, optional timer fade, resume positions for long recordings, and saved sessions containing queue/position/playback mode. | Timer cancels/replaces reliably and works screen-off; restored sessions do not autoplay unexpectedly; speed/seek/resume remain coherent after interruption. |
| **7. Device music, downloads and offline use — deferred** | The code 12 prototype and plan are preserved outside the active build at the owner's request. See `STAGE7_PLAN.md` and `deferred/stage7/`. | No active gate. Re-scope and review before any later implementation. |
| **8. Continuous and advanced playback** | Code 14 implements one-next URL preparation, manual attenuation, smart shuffle and bounded opt-in YouTube-related autoplay. Crossfade and automatic normalization remain limited by decoder and loudness-data feasibility; no gapless guarantee. See `STAGE8_REPORT.md`. | The owner reported the Stage 8 build working on the POCO. Quantitative gap, data-use and battery measurements were not supplied. |
| **9. Advanced local library** | Saved rule-based smart playlists, duplicate/unavailable-entry review and cleanup, local playlist version history/restore, and listening statistics as plain lists/tables. App-data backup/import/export and new playlist-link exchange are excluded. Delivered as code 15; the owner reported everything working. See `STAGE9_REPORT.md`. | Rules are deterministic and privacy-aware; cleanup removes only explicitly selected occurrences; network errors never imply an unavailable song; playlist restore is transactional and bounded; statistics match qualified local events. |
| **10. Remaining functional tools — skipped** | The owner does not need this stage. The earlier LRC, advanced lyric tools, source-detail and visualizer proposal remains historical reference only. | No active implementation or acceptance gate. |
| **11. Whole-app stabilization** | End-to-end integration, database migration and restart recovery, bounded caches/jobs, offline/network failure coverage, optimized APK, dependency/source notices and clear user/developer documentation. | The agreed feature inventory has verified outcomes; no placeholders masquerade as features; realistic POCO listening runs and process/restart scenarios pass. Remaining provider/device limitations are recorded explicitly. Public publishing is a separate action. |

## 5. Proposed behavior defaults

These defaults make features testable and reduce repeated design decisions. They can be adjusted during this plan review.

- Selecting a song from a song list starts a queue snapshot of the currently loaded songs at that index. Later search results do not mutate that queue. Collection Play/Shuffle explicitly loads the collection with visible progress and cancellation; partial loading is disclosed.
- At the end of the queue, stop unless repeat or the later autoplay option is enabled. Autoplay is off by default.
- Play next inserts after the current entry; Add to queue appends. Removing upcoming entries leaves the current song alone. Removing the current entry selects its next available successor while preserving the user's play/pause intent; an empty queue stops. Undo restores entries/order without restarting a different track behind the user's back.
- Restore the last queue and position after process death without unsolicited autoplay. Leaving the screen and locking the phone continue active playback. Audio-focus loss and output disconnect follow explicit policies; force-stop is handled as a stopped app until the user resumes it.
- Network loss does not discard the queue. A bounded retry/recovery policy retains position; unavailable entries get a visible reason and an explicit retry/skip path. Automatic skipping, if used, is bounded to prevent loops.
- History records actual listening, not a tap on a failed result. Define and test the listening threshold before using history for statistics. Private listening disables the corresponding persisted activity and derived personalization.
- If downloads are revisited later, use explicit actions with Wi-Fi-only initially and automatic keep-played caching off. Destructive download deletion remains a reviewable action.
- Crossfade, loudness analysis, autoplay and the visualizer start off. Enable them intentionally after their behavior and resource costs have been verified.
- Lyrics fallback remains user-controlled. Store text/offset/cache identity separately from playback position, and retain source attribution.

## 6. Definition of functional completion

Maintain a feature checklist with **implemented / verified / limited / unresolved** states. “Complete” requires actual behavior, not only a button, an API stub or a passing mock. A provider-dependent feature is not marked fully complete when only its fallback exists.

For each milestone:

1. Define commands, state transitions, persistence and failure behavior.
2. Implement repositories/engine logic with appropriate behavioral tests.
3. Connect a plain usable screen to the real implementation.
4. Run relevant host checks and live provider checks when provider behavior changed.
5. Run the necessary POCO acceptance scenarios, with a small manual checklist where OEM automation remains blocked.
6. Record results and produce an installable update.

The current flow includes search → collection → play → background/lock → queue editing → like/playlist → lyrics → restart/session restore → smart library and statistics. Device-folder scanning, downloads and offline playback belong to the later backlog. Add focused failure cases for network drops, playlist cleanup, missing lyrics and changed provider responses.

“Stable” is demonstrated by these gates and real-device use. External availability and provider failures must be handled predictably; no claim of universal error-free behavior is made.

## 7. Visual work — now authorized page by page

The owner authorized a Home redesign from a supplied reference on 19 October 2026, while retaining the existing color rules and M3 Expressive interactions. See `ui/home/PLAN.md`. Other page layouts await separate references and instructions. Animated covers/backdrops, palette changes, bespoke animation systems, decorative visualizers and poster/sing-along styling are not implied by this Home pass. UI work consumes the existing repositories, playback controller and typed state.

Stages 2–6, Stage 8 code 14 and Stage 9 code 15 are accepted from the owner's phone reports. Stage 10 is skipped. Stage 7 code 12 was deferred without phone acceptance, and code 13 restored online-only scope. Continue using the established design foundation.
