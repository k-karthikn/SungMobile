# Stage 9 plan — advanced local library

Prepared 18 October 2026 for owner review; approved and implemented 19 October 2026. The owner reported the Stage 8 code 14 build working on the POCO. The implementation and phone acceptance are documented in [STAGE9_REPORT.md](STAGE9_REPORT.md). Keep YouTube as the only music source, the local library without Google sign-in, the existing M3 Expressive skeleton screens and standard animations, and the manual APK-testing workflow. The next APK must retain `com.karthi.sungmobile.stage0`, the current debug signing certificate and existing phone data. Stage 7 device music and downloads remain outside the build.

## Scope decision

Stage 9 covers **saved smart playlists, playlist cleanup, local playlist version history, and listening statistics**. The owner explicitly removed two proposals from this stage:

- **No app-data backup, export, import or restore file workflow.** Playlist version restore below is an in-app undo/history feature for a single local playlist, not an app-data backup system.
- **No new playlist-link exchange feature.** The existing Stage 4 ability to open, play or save supported YouTube playlist links remains in place; do not expand it into account sync, sharing/export or a new interchange format.

Do not add cloud storage, Google sign-in, local audio scanning, downloads, a new online music provider or a design-polish pass. The owner can reconsider the two omitted features in a later plan; they are not Stage 9 delivery gates.

## Proposed behavior

### 9A. Saved smart playlists

Add a distinct saved **smart playlist** type alongside ordinary editable playlists. Users can name one, choose from a small set of local rules, preview its current songs and save it. Initial rules should use data the app already records: liked songs, qualified play count, and played within a chosen recent window. Keep combinations and sorting bounded and understandable: for example, liked + played at least twice, ordered by most recent or most played, limited to 25/50/100 songs. Do not infer genre, mood or artist identity from unreliable YouTube titles. A smart playlist is a live view; its members update after a Like, qualified listening event or history clear. **Play/Shuffle takes a snapshot** so later library changes do not alter the active queue. Editing a smart playlist changes its rules; manually adding, removing or reordering songs belongs to an ordinary playlist. Offer an explicit “Save current songs as ordinary playlist” action if useful, with a preview.

Rules, tie-breakers and limits must be deterministic and persisted as typed data, not stored SQL or an unbounded query string. Evaluate history-dependent rules against the current history policy in one database transaction. In Private mode, hide history-derived smart results; disabling history recording stops new input without deleting old records; clearing history immediately removes derived results. Pure Likes-based rules may remain visible in Private mode because deliberate Likes are still retained there. Never store a separate cache of private listening data.

### 9B. Playlist cleanup

For an ordinary playlist, show a review screen with duplicate *occurrences* grouped by canonical YouTube video ID. Preserve the first occurrence by default and let the user choose exactly which entry IDs to remove. Show the before/after count and require a final confirmation; nothing is deleted automatically. Smart playlists are computed views and do not receive this cleanup action.

“Unavailable” must be checked on demand against YouTube, with visible progress, cancellation and a bounded number of probes per run. Only a provider-confirmed unavailable item is offered for removal. Network errors, timeouts, rate limits and authentication challenges are **unknown**, not “unavailable”; leave those entries untouched and show a retry option. Do not persist signed stream URLs or turn the check into audio downloading. A confirmed unavailable entry keeps its local metadata until the user explicitly removes that occurrence.

### 9C. Local playlist versions

Before a confirmed edit to an ordinary playlist's name, entries or order, save a bounded local version containing the playlist name and ordered entry IDs/track keys. Keep a small cap (proposed: latest 20 versions per playlist) and show date, song count and a preview of differences. A restore first saves the current version, then applies the selected version in one Room transaction, preserving duplicate occurrences and order. This is **per-playlist local history**, not a device-transfer backup. Version restore applies to existing playlists; deletion remains final after the current confirmation and should say so clearly. Versions must not contain signed URLs, credentials or audio bytes.

### 9D. Listening statistics

Add a plain, readable Statistics screen from the existing qualified listening events. Start with total qualified plays, total recorded listening time, top songs and recent listening periods; paginate detail lists rather than loading the entire history into Compose. Define periods deterministically from event times (proposed: a new period after a 30-minute gap) and label them as derived, since the current schema stores playback passes rather than a real session ID. Do not count a tap on a song that failed to play. Private mode hides history-derived statistics immediately and creates no new events; Clear listening history resets them. Statistics use local data only and do not contact YouTube.

## Data and screen boundaries

The current Room schema is version 4. Implementation will likely need a version 5 migration for smart-playlist definitions and playlist revisions, plus bounded indexes or queries for listening aggregates. Preserve the existing v1–v4 migration path and test upgrades from code 11, optional code 12, code 13 and code 14 without clearing Likes, ordinary playlists, history, lyrics preferences, saved sessions or the queue. Keep the Stage 7 tables untouched and unused. Use focused models/repositories in `core:model` and `core:data`; keep rules as pure testable functions where possible. Add small Library and Statistics screens/ViewModels rather than enlarging the existing player or putting all logic in one file.

The app remains online-only. Smart playlists and statistics work from local metadata while offline. Availability checks require a network connection, but a failed check must never modify a playlist. Existing playlist link intake is left unchanged.

## Implementation order and gates

| Part | Work | Acceptance gate |
| --- | --- | --- |
| 9A. Data foundation | Specify typed rule/version models; add explicit Room migration, indexes and repository contracts. | Upgrade preserves code 14 data; invalid rules and duplicate entry IDs are rejected without a partial write. |
| 9B. Smart playlists | Implement deterministic rule evaluation, saved definitions and Library UI. | Likes/history changes update results; Private mode and Clear history remove history-derived results; playback uses a stable snapshot. |
| 9C. Cleanup | Add duplicate review and bounded availability scan with explicit selection/confirmation. | Only selected occurrence IDs are removed; temporary provider/network failures never become deletion candidates. |
| 9D. Version history | Snapshot ordinary playlist edits and implement preview/transactional restore. | Name, duplicates and order round-trip; a failed restore leaves the current playlist intact; retention remains bounded. |
| 9E. Statistics | Add bounded aggregate queries and simple screen. | Qualified plays/time match source events; Private mode and history clearing are reflected immediately. |
| 9F. Delivery | Run migration, repository, privacy, UI and relevant provider tests; lint and build an in-place signed APK. | App ID/signing identity unchanged; owner tests the APK manually on the POCO before acceptance. |

## Proposed POCO check

Install the Stage 9 APK over code 14 without uninstalling. Confirm existing Likes, playlists, lyrics settings, saved sessions and queue survive. Create a smart playlist, Like and play songs, and confirm its preview updates while the active playback queue stays fixed. Turn Private mode on/off and clear listening history to check smart results and statistics. Review duplicates in an ordinary playlist and remove only selected occurrences. Run availability review once with network and once without; no entries should disappear on a network failure. Edit, reorder and restore a playlist version, checking duplicate order. Finally, restart the app and confirm smart rules, revisions and statistics persist. No USB test is required unless a specific device issue needs it.
