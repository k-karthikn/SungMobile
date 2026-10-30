# Stage 4 — Catalog and discovery

**18 October 2026 phone follow-up:** The owner reported that playback works from album/playlist details, but the bottom mini-player disappears there and returns on Home. `SungApp` now shows the mini-player on collection detail pages. Build `0.5.1-stage4` (code 7) was delivered and the owner subsequently reported **Stage 4 working fine**, accepting it before Stage 5 began. The original Stage 4 host/live/lint figures below belong to the code 6 build; code 7 passed `:app:assembleDebug`. The old signing key was lost during OS migration, so code 7 uses a new debug key and required a fresh installation over any old-key build. The owner agreed to that data reset. Later builds retain the new key.

Implemented on 13–14 October 2026. Phone acceptance is pending the user's manual POCO checklist. Stage 3 remains the accepted device baseline.

## Working behavior

- Search previews wait 350 ms after an edit of at least two characters. Submit runs immediately for any nonblank query. Edits and leaving Search invalidate old work immediately. Preview typing does not populate search history; submitting or opening a result does. Search results, category and scroll survive a visit to collection details. Durable query and route identity survive state recreation; continuation tokens are never saved to disk.
- Pasted links, Android Share text and supported VIEW intents use one YouTube validator. A song with playlist context offers both destinations. Multiple links offer choices. Valid timestamps are bounded to 24 hours and applied only after an explicit Play action; actual playback also clamps to known track duration. Receiving a link creates a preview, never a queue change or unsolicited playback.
- Dedicated collection pages show actual metadata and distinguish loaded counts from provider-reported totals. Playlist occurrences retain separate identities and source order, including repeated songs. Membership/paid/upcoming rows retain provider-exposed titles/reasons as unavailable information. Other extractor omissions/errors produce a partial-content notice.
- Collection Play all, Shuffle all, Play next, Add to queue and Add to local playlist gather pages before committing once. Existing playback continues during loading. Cancel or a changed target queue rejects stale work. Bounds are 500 playable entries, 50 continuation requests or 90 seconds per loading attempt. A failed/limited/partial load requires an explicit choice to use the loaded subset. Retry resumes a failed continuation; expired tokens require refreshing the collection. Library saves use the existing transactional duplicate policy.
- Home observes real local data: up to 12 pinned playlists, 12 recent songs and 12 liked-song previews, plus full-list shortcuts. Liked shuffle and Recent mix use up to 50 distinct candidates from the latest 50 liked/recent songs. Candidates are sorted by canonical identity and shuffled with a stored seed (initial UTC day; explicit refresh increments it). The recent mix also incorporates the listening-history epoch. No mood classification, charts or account recommendations are invented.
- Private mode suppresses history-derived Home content; Likes and pins remain deliberate local data. Clearing history invalidates its candidates. An open mix captures its list; source changes close an obsolete preview. Queue playback remains a snapshot.

The app retains the M3 Expressive foundation and uses simple lists, controls, dialogs and standard component/list motion with reduced-motion support. There is no new design pass, account flow, music source, database migration or audio engine.

## Organization

| Location | Ownership |
| --- | --- |
| `core/model/.../catalog` | Collection metadata, occurrences, validated link choices and bounded batch loader. |
| `core/model/.../discovery` | Home contract and deterministic local mix selection. |
| `core/youtube/.../catalog` | Provider mapping, search/details and bounded continuation storage. |
| `core/youtube/StreamExtractor` | Existing on-demand audio resolution, separately scheduled from catalog work. |
| `core/data/.../discovery` | Transactional Home reads from existing Room tables and privacy policy. |
| `app/feature/collection` | Detail route, page state, collection actions and dialogs. |
| `app/feature/home` | Local Home observation, mix state and skeleton screen. |
| `app/navigation` | Shared/pasted link intake and choice dialog. |
| `app/app/SungViewModel` | Search state only; callbacks route collections/links to their owners. |
| `app/app/SungRoot` | Feature composition; MainActivity supplies lifecycle-owned ViewModels and intents. |

Catalog requests have two worker slots and stream resolution has its own worker slot. Provider continuation callbacks stay in a bounded 128-entry process-local store. Stable fingerprints expose repeated continuations; search deduplicates song identity while collection paging preserves occurrences. No signed stream URLs enter Room or saved state.

## Provider limitations

Live checks on 14 October 2026 (English localization, India content region) passed all five probes: song search/paging, two audio byte reads, album/playlist details, a long-playlist continuation and artist fallback search. The long playlist returned 101 entries on its first page and 15 on the next, against a provider-reported total of 120. Another playlist exposed partial content (40 returned, 45 reported), confirming why subset choices remain necessary. See Stage 4 results (files removed 30 October 2026).

- Album/playlist search can return playable playlist URLs. Direct YouTube Music `MPREb_…` album browse links are recognized but explicitly unsupported by this adapter; use the album search or a playlist URL instead.
- Artist search works. Where available, artist opening exposes **YouTube channel videos**, not a verified music discography. Missing music tabs provide a clearly labelled song-search fallback. `UC…` music browse IDs route through channel handling.
- Anonymous online Home recommendations were not implemented or verified. Home consists of actual local data; a fresh installation offers Search/open-link entry points. No fallback search is presented as a recommendation feed.
- The provider may omit deleted/private rows without exposing their identity. The app cannot invent them. Exposed restrictions and extraction errors are retained/reported.
- Android may keep ordinary YouTube links assigned to YouTube. Sung does not claim domain ownership (`autoVerify=false`). Share → Sung and paste are the reliable explicit entry points.
- Local metadata remains browsable offline. Streaming and remote collection retrieval still require a working YouTube connection. Downloads and lyrics remain later milestones.

## Verification and upgrade

Verification: **124 host tests passed**, **five live provider tests passed**, lint **zero errors / 13 dependency-version notices**, and APK signature verified against Stage 3. Exact counts, APK identity and signature are recorded in build-summary.json (files removed 30 October 2026). The deterministic checks cover search debounce/cancellation/history, link parsing/intents, duplicate occurrences, batch limits/timeouts/retry/cancellation, atomic collection actions, Room Home bounds/privacy, seeded mixes, and search/detail state restoration. Existing playback, Media3 controller, queue, migration and library regressions are included.

Host tests do not certify HyperOS share delivery, lock-screen playback or physical rotation behavior. Install the update over Stage 3 without uninstalling or clearing data, then follow the POCO checklist (files removed 30 October 2026). The application ID, signing key and existing Room version are retained.
