# Stage 4 — Catalog and discovery

Prepared: **13 October 2026**.

Status: implementation authorized on 13 October 2026 and delivered on 14 October 2026; host verification and provider limitations are recorded in `STAGE4_REPORT.md`. POCO acceptance is pending. The user reported every Stage 3 phone-checklist item working on 13 October 2026, so Stage 3 is accepted on that basis.

## Outcome

Make finding and opening music complete: responsive search, received YouTube links, dedicated collection pages, reliable collection-to-queue actions, and useful Home sections backed by real data. Retain the existing M3 Expressive foundation, ordinary component/list animations and reduced-motion support. Use simple lists, thumbnails, controls and dialogs.

YouTube remains the only music provider. No Google sign-in is required. Lyrics/settings remain Stage 5; downloads remain Stage 7; autoplay remains Stage 8; advanced smart-playlist rules and statistics remain Stage 9.

## Current implementation and gaps

- Search supports Songs, Albums, Artists and Playlists, manual submission, stale-response protection, and Load more. Editing text does not yet start a debounced request or immediately invalidate an older request.
- Collections currently reuse the Search screen. Navigation retains pages but does not fully restore query/category/list position. There is no complete paged collection Play/Shuffle operation.
- YouTubeSource currently deduplicates every page by song ID, and the ViewModel deduplicates across pages. This is useful for search results but can erase intentional duplicate playlist occurrences.
- Artist opening attempts the channel videos tab and otherwise offers song search. A channel's uploads and search-by-name results are not a verified artist discography.
- The parser accepts supported HTTPS watch, short, embed, playlist, channel and handle links. YouTube Music browse IDs and watch links with both video and playlist context need explicit treatment.
- Android currently registers only the launcher activity, with no shared-text or URL-opening handlers.
- Home displays the active song and recent catalog results; it does not yet observe local playlists, Likes or qualified listening history.

These findings come from the local code and Linux reference documents. Current external provider capabilities will be measured during implementation before adopting an additional adapter.

## Delivery sequence

| Part | Work | Finish gate |
| --- | --- | --- |
| 4A. Provider and catalog contracts | Probe result kinds, collection paging, artist/channel support and link shapes. Split mapping, continuation storage and collection extraction out of YouTubeSource. Add typed collection and occurrence identities. | Capability matrix distinguishes verified support from fallback or unavailable behavior; existing stream resolution remains covered. |
| 4B. Responsive search | Debounce, immediate stale-request cancellation, filters, paging, submitted-query history and navigation restoration. | Slow old requests cannot replace newer results; typing does not fill history; retry retains usable results. |
| 4C. Link intake and detail screens | Paste/open, Android shared text and supported web intents, album/playlist/artist detail routes and per-song library actions. | Cold/warm activity delivery opens the intended content once, without unsolicited audio; invalid links are rejected. |
| 4D. Collection actions | Whole-collection loading with progress/cancel, Play, Shuffle, Play next, Add to queue and Add to local playlist. | Queue changes are atomic and deliberate, duplicate occurrences/order survive, and incomplete loads are clearly identified. |
| 4E. Real Home and local mixes | Pinned playlists, recent songs, Likes shortcuts and small reproducible mixes; provider discovery only where verified. | Every visible row has actual data and working actions; private mode and history clearing invalidate derived content. |
| 4F. Integration | Deterministic tests, live provider checks, APK and short POCO checklist. | Stage 2–3 regressions pass; provider limitations are documented; user checks upgrade and new flows. |

## Proposed behavior

### Search

- Wait **350 ms** after the last edit before searching queries of at least two characters. Search/keyboard submit works immediately for any nonblank valid query. Clearing the field cancels current work and returns to recent searches.
- Cancel/invalidate the current request as soon as query/category changes, before waiting for the new debounce. Cancel pending typing requests when leaving Search or opening a detail route. Guard results by query/category/request identity even if a provider ignores cancellation.
- Keep Songs, Albums, Artists and Playlists separate. Preserve loaded rows when an additional page fails; offer retry on that page. Disable duplicate Load more requests and stop repeated/cyclic continuations. Expired in-memory page tokens offer refresh instead of silently restarting or duplicating the list.
- Debounced previews do not create history. Explicit submission/category search or opening a preview result commits the displayed query/category once; a retry can update recency. Respect the existing recording/private-mode barriers.
- Back from a detail screen restores the original query, category, loaded results and list position. Preserve navigation across rotation. After process recreation, re-fetch from durable route/query identity; never persist opaque provider page tokens.
- Selecting a search song keeps the existing loaded-results queue-snapshot behavior. Collection-wide actions use the complete collection loader below.

### Links and Android integration

- Centralize normalization and validation for pasted links, ACTION_SEND text and supported ACTION_VIEW URLs. Handle cold launch and subsequent intents; activity recreation must not re-open an already handled intent or create another audio engine.
- Extract a supported URL from shared text containing a title. If multiple supported URLs exist, let the user choose one. Accept only validated YouTube/YouTube Music hosts and supported path/ID shapes; reject deceptive hosts, embedded credentials, unsupported schemes and arbitrary redirect fetching.
- Preserve relevant context: a watch URL with a playlist offers song or playlist opening; a valid timestamp can be offered as the starting point after an explicit Play action. Bound malformed or excessive timestamps.
- Probe and support provider-returned YouTube Music album/artist browse forms when the adapter can resolve them. If a form is unsupported, say so without misclassifying it as a search term.
- A received link opens a preview/detail page. Playback begins only on a user Play/song action. Sharing a URL never imports a playlist or alters the current queue automatically.
- Android's chooser or default-link settings may keep regular YouTube links opening in YouTube. Share → Sung and paste remain explicit entry points; do not promise ownership of YouTube's verified domains.

### Album, playlist and artist details

- Separate collection details from search state. Display real title, artwork, creator/artist and counts only when the provider supplies them. Identify loaded counts versus total counts.
- Keep source order and occurrence identities for playlist entries, including repeated videos. Deduplicate search results by canonical identity; deduplicate collection page overlap only with evidence of the same source occurrence. Do not apply blanket video-ID deduplication to collections.
- Provide Like, Add to local playlist, Play next and Add to queue for playable songs. Opening an online playlist does not create a local playlist until an explicit save/add action.
- Retain unavailable/private/deleted entry information when the provider exposes it; otherwise show an honest partial-content notice. Do not invent hidden song metadata or discard an entire collection because one row is unavailable.
- Artist capability comes first: expose real artist songs/albums only if verified. Label channel uploads as channel videos and search fallbacks as search results. An isolated YouTube Music catalog adapter may fill demonstrated gaps; it must remain inside core:youtube, with bounded requests, mapping tests and explicit failures. Do not broaden the provider list or couple a catalog experiment to audio resolution.

### Collection actions and large lists

- Browsing loads one page at a time. Explicit Play/Shuffle/Play next/Add to queue/Add to local playlist gathers pages with visible progress and Cancel; keep existing playback running while it loads.
- Proposed safety bounds per operation: **500 entries, 50 page requests or 90 seconds**, whichever is reached first. Stop on cyclic/empty-no-progress continuations. Reassess these constants against measured provider/device behavior, not by silently removing bounds.
- If loading fails or a bound is reached, retain the loaded result and offer Retry where possible, Cancel, or a clearly labelled action using the loaded subset. Never label a partial subset as the full collection.
- Validate the completed batch and apply it once. Cancelling makes no queue/library mutation. A newer Play/collection intent invalidates the old operation so its late result cannot replace the newer queue. If the target queue is replaced during loading, cancel the stale queue operation and require an explicit fresh action.
- Shuffle once at the service boundary using the existing collection command. Each duplicate receives its own queue-entry ID. Playing a collection creates a snapshot independent of future catalog or local edits.
- Saving/addition to a local playlist uses Stage 3's duplicate policy and transaction behavior, with the loaded scope stated explicitly.

### Home and basic local mixes

- Show real, bounded sections: current playback shortcut, pinned local playlists, recently played songs, and Likes. Each section links to its full local list; use small previews (up to 12 entries) rather than loading the whole library into Home.
- Provide a **Liked shuffle** and **Recent mix**, each up to 50 distinct songs and only when enough candidates exist. Use a documented deterministic selection/seed for a given refresh; freeze the list when opened/played. Refresh explicitly or when its source data/privacy policy changes, not on every playback progress tick.
- Recent mix uses qualified public listening history. While Private mode is active, suppress history-derived mixes and personalized online requests; deliberate Likes and pinned-playlist shortcuts remain available. Clearing history immediately removes its derived mix candidates, including cached results.
- Mixes are local generated lists, not claimed mood classification or YouTube account recommendations. They can be played/shuffled or saved through existing local-playlist actions. Advanced rule editing and statistics remain Stage 9.
- Anonymous online Home/discovery sections are included only if 4A verifies a usable YouTube endpoint. Label their actual source and content accurately; search results cannot be labelled charts or personalized recommendations. Report unsupported provider sections as limited rather than claiming complete discovery support.
- A fresh library offers useful search/open-link entry points and any verified anonymous discovery data. Offline Home keeps local metadata and clear network retry states. No cloud fallback source or automatic sign-in.

## Organization and resource limits

| Location | Ownership |
| --- | --- |
| core:model/catalog and link models | Collection metadata, result/occurrence identity, capabilities, validated open requests and action progress. |
| core:youtube/catalog | Search and detail adapters, metadata mapping, bounded continuation store and provider errors. Keep stream resolution separately owned. |
| core:data/discovery | Local Home/mix queries and any bounded public metadata cache. Private/history-derived responses must not survive a clear/privacy barrier. |
| app/feature/search | Query/category/debounce/paging state and explicit submission events. |
| app/feature/collection | Detail route/state, occurrence lists and collection-load operations. |
| app/feature/home | Home ViewModel, section state and local mix actions. |
| app/navigation and link intake | Android intents and route restoration; MainActivity forwards events. |
| Existing library/playback components | Shared Like/add actions and the single existing service/controller. |

No new global manager. Keep composables free of storage/network calls. Playback resolution must have capacity even when search/discovery is busy; bound/cancel catalog concurrency, avoid prefetching all pages, and preserve the extractor's cancellation of network calls. Cache durable metadata only; signed audio URLs remain transient. Start with bounded in-memory public catalog caching; add Room tables/migration only if a concrete persistent requirement needs them.

## Verification and acceptance

- Fake-clock search tests cover rapid edits, immediate invalidation, submit during debounce, category changes, leaving the screen, clear, failures and late uncancellable results. Confirm preview typing does not populate persisted history.
- Link tests cover supported forms, malformed IDs, deceptive hosts, shared text, multiple URLs, timestamps and playlist context. Test cold/warm intents, rotation and Back with one playback service.
- Provider mapping/paging tests include intentional duplicates, overlap, missing fields, unavailable entries, expired/repeated continuations and partial pages.
- Collection operation tests cover cancel/error/limits, explicit partial selection, atomic queue mutation, concurrent new playback intents and playlist snapshot independence.
- Home/mix tests cover empty libraries, stable selections, bounds, offline metadata, likes changes, history clearing and private-mode invalidation.
- Run live searches/details/paging/artist capability and stream regression checks. Record region/time and provider limitations; passing mocks or fallback searches do not prove artist discography support.
- Run affected module tests, app integration checks and lint, then deliver an update APK and manual POCO checklist: upgrade, rapid search, Share → Sung, collection actions, Back/rotation, Home/private mode and background playback.

No new account, phone connection or tool installation was required. Continue the user's manual APK-testing workflow. See `STAGE4_REPORT.md` for the delivered scope and capability limits.
