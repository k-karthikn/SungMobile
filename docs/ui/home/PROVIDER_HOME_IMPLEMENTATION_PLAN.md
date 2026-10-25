# Plan: provider-backed SungMobile Home

Status: implemented 25 October 2026. See the [implementation report](PROVIDER_HOME_IMPLEMENTATION_REPORT.md) for the shipped design, validation, and remaining device checks. Input: [desktop content research](SUNG_DESKTOP_CONTENT_RESEARCH.md), [video and architecture research](PROVIDER_HOME_RESEARCH_REPORT.md), and the user's clarification that tapping a track card starts music while tapping a playlist or album card opens its page. **Do not put a play button on individual cards.** The existing local sections move off Home so discovery stays clean.

## Outcome and scope

Home should show actual titled YouTube Music shelves such as `Quick picks` when the provider returns them, using the rounded, expanding/compressing artwork carousel from the supplied video. The four mood chips open song searches. Home becomes a focused discovery surface. Existing local mixes, Recently played, liked songs and pinned playlists remain available through Library, with their current privacy/queue behavior. Home works gracefully when the online feed is empty or unavailable; no row title, song, playlist, or language is fabricated to mimic the screenshot.

This phase does not add account sign-in, audio-output switching, an extra bottom-navigation destination, a new recommendation algorithm, or Python/Chaquopy. It does not change local-song eligibility for likes/history (the existing play-and-queue-only decision remains in force).

## Proposed mobile layout

1. Keep SungMobile's shared settings button, search entry, mini-player, bottom bar and expanded-window rail. The Home title remains `Listen`. Put one accessible refresh icon beside the title for **online Home shelves**. Remove the large `Now playing` and `Refresh mixes` pills from Home: tapping the shared mini-player already opens Now playing, and the mix-refresh action belongs in Library > Mixes.
2. Place four compact, rounded-rectangle outlined chips below the header: `Feel good`, `Focus`, `Unwind`, `Energize`. Each sends Sung's corresponding query to the existing Search screen with `CatalogKind.SONG`; it does not mutate the Home shelves.
3. Render each supported provider row in returned order: heading, 1:1 artwork carousel, then a fixed-height caption for the currently snapped/prominent card (title up to two lines; artist/type one line). The fixed caption prevents labels from becoming unreadable when neighboring cards shrink. Use a small `Album`/`Playlist` badge on artwork where useful; songs need no badge. No card play overlay, autoplay-on-focus, or second-tap requirement.
4. Do not append the old `Your mixes`, Recently played, pinned playlists or liked songs sections to Home. Their Library destinations remain intact. On a compact phone, limit each online shelf to a bounded set of cards so the page remains scrollable; provide more only if the provider supplies a valid route/continuation and the UI has a defined action. During initial development, retain the existing Home until the new feed is usable; replace the old sections together with the new Home UI rather than leaving a blank page between slices.
5. On wide screens, let the carousel show more large/medium cards while retaining the same item semantics. Color, badge and placeholder roles come from `MaterialTheme.colorScheme`; do not copy the example video's navy/blue palette or embed colors from album art into text surfaces.

### Where the current Home content goes

| Current Home element | Destination after the redesign |
| --- | --- |
| `Your mixes` cards | Library > Mixes already shows Liked shuffle and Recent mix. Move the `Refresh mixes` action there as a small labeled icon/action; it must still advance the existing mix seed. |
| Recently played rows | Library > Recents already supports filtering, sorting, playback and queue actions. Private mode continues to suppress history. |
| Liked-song rows | Library > Liked songs already provides the full list and actions. |
| Pinned playlist cards | Library > Playlists already sorts pinned playlists first and marks them `Pinned`; keep pin/unpin in the playlist menu. |
| `Now playing` pill | The persistent mini-player opens the full player whenever a track exists. |

No underlying likes, listening history, pin state, local mixes or Room records are removed. Remove only their duplicated Home presentation. When the provider is offline or returns no supported rows, show a compact message with Retry and an optional `Open Library` action instead of filling Home with the old sections. Search, Library and the mini-player stay usable.

### Card interaction table

| Provider item | Primary tap | Visibility rule |
| --- | --- | --- |
| Song / playable music video | Start it using `PlaybackControl.select()`; use the playable songs in that shelf as its next/previous context. | Require a valid video ID and canonical song URL. |
| Playlist | Open the existing collection detail route. | Require a playlist ID and a route that the current catalog source can load. |
| Album | Open an album detail route with real tracks. | Show only after album browse or a verified album-to-playlist route works end to end. |
| Artist | Open an artist detail route if supported. | Otherwise omit from this version of the feed; never show a dead card. |
| Podcast, audiobook, episode, unknown renderer | No card. | Current app does not have a suitable detail/playback flow. |

The whole visible card is a minimum 48 dp accessible target, with semantics such as `Play <song> by <artist>` or `Open album <title>`. The type badge is informational, not a second button. Long-press or extra menu actions are outside this Home-card pass; existing Search/Library actions remain available elsewhere.

## Motion specification

- Use Material 3 Compose `HorizontalMultiBrowseCarousel` and `rememberCarouselState` for supported rows. Start near `preferredItemWidth` 190–220 dp on compact phones, 8–12 dp gaps, square image bounds, and a large rounded Material shape. Tune actual widths by rendering at 320, 360, 412, 600 and 840 dp. Allow the next card to peek in; the carousel's own mask/size interpolation supplies the swipe animation seen in the video. Avoid an additional scale animation on each bitmap.
- Use `CarouselState.currentItem` for the caption and animate only caption changes (short fade-through) when motion is enabled. Keep a stable caption height so horizontal swipes do not move the whole Home page vertically. Save the selected item/scroll position per stable shelf identity across navigation and refresh; clamp it when a shelf's item count changes.
- Tap acts immediately; a swipe scrolls and snaps. Do not make the first tap merely focus the card. For a collection open, use the app's current collection overlay first; a shared-artwork transition can be a later refinement if it fits existing navigation without duplicating the player or adding a second detail screen.
- Respect `LocalSungMotionEnabled`: when disabled, render a non-morphing `LazyRow` with the same card and direct-tap behavior, and change captions without fade. Support accessibility services and large font sizes; allow enough vertical space for two title lines.
- Reuse the current `SungArtwork`/Coil loader, request image sizes near actual display bounds, use a stable themed placeholder, and avoid new bitmap collage generation. Keep carousel motion confined to the card's mask/layout, not network image reloads.

The [official carousel component guide](https://developer.android.com/develop/ui/compose/components/carousel) and [API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/carousel/HorizontalMultiBrowseCarousel.composable) support the proposed sizing/masking behavior. [Compose lazy-list guidance](https://developer.android.com/develop/ui/compose/lists) supports bounded card dimensions and stable list structure.

## Content architecture

**Native Kotlin port of the required Sung behavior, not a Python runtime:**

```text
YouTube Music FEmusic_home browse + continuation responses
    -> core:youtube Home feed gateway + bounded mixed-row parser
    -> provider-neutral HomeShelf(title, items, stable key)
    -> discovery-feed ViewModel/state, separate from existing Room-backed mix state
    -> DiscoveryHomeScreen carousel/empty/error states
```

1. Add a `HomeFeedSource` capability and `HomeShelf` model in `core:model`; implement them in `core:youtube`. Keep `RoomHomeRepository` and the existing `HomeViewModel` responsible for local likes/history/pins and Library mixes. Wire the provider source once in `AppContainer` to a separate Activity-scoped discovery-feed ViewModel, rather than calling the provider from composables. The name `HomeViewModel` can be clarified in a later refactor; avoid disrupting Library's current `HomeState` dependency in this pass.
2. Reuse the existing OkHttp/Gson stack and cancellable request pattern from `YouTubeLyricsGateway`; keep a provider-specific client/parser in `core:youtube` so YouTube renderer objects never enter UI or Room tables. Send an unauthenticated YouTube Music `browse` request for `FEmusic_home`. Sung's `helper/catalog.py:402-408` and installed `ytmusicapi` parser are the behavior reference. Bound HTTP time, body size, section count (target five), items per section, string length and continuation count. Cancel when the ViewModel is cleared or a newer refresh replaces the request. Use the same network-policy/error mapping conventions as other catalog work.
3. Parse the initial section-list and its continuation(s). Support the renderer variants actually needed for this feed (`musicTwoRowItemRenderer`, playable responsive-list items, carousel headers/title or strapline). Map only validated IDs, normalized text, thumbnail URL, kind and optional duration to `CatalogItem`. Keep provider order; deduplicate identical item identities within a shelf; skip empty/unsupported shelves. Never convert an unknown kind into a song merely because it has artwork.
4. Finish routing before surfacing collection cards. Playlist IDs should form canonical playlist URLs. Artists with a supported `UC…` channel URL can use the existing route. Album browse IDs (`MPRE…`) currently fail in `CatalogExtractor.open()`; either implement native YouTube Music album browse/track mapping in `core:youtube` or use a verified audio-playlist ID from the album item. Test the exact route through `CollectionViewModel` and `CollectionScreen`. If a route cannot be supported, omit that card rather than opening a broken detail page.
5. Keep online feed state distinct from local `HomeData`: `initialLoading`, `refreshing`, `shelves`, `lastSuccess`, `error`. The discovery-feed ViewModel lives across Home/Search/Library tab changes, so return to Home does not refetch immediately. Refresh preserves the last good shelves. A modest in-memory freshness window (proposed 20 minutes) avoids a request on every tab switch; manual refresh bypasses it. An optional later cache may store **normalized** bounded feed metadata in `cacheDir` for offline restart, never raw provider JSON, signed media URLs, history IDs or downloaded artwork. Start with the in-memory behavior and evaluate whether restart caching is useful after phone testing.
6. A provider error shows one compact retry/connection message, or keeps the last successful shelves with a small error notice. An empty successful feed removes its placeholders and offers Search/Library navigation. Private mode continues to hide local history in Library; no local likes/history IDs are sent in the anonymous Home request.

`ytmusicapi` documents Home as variable titled rows with mixed item types: [browsing reference](https://ytmusicapi.readthedocs.io/en/stable/reference/browsing.html). Its internal response parser is useful as a **source reference**, but copying all of its Python code or making its internal endpoint a public contract would be brittle.

## Implementation sequence and checkpoints

| Slice | Work | Reviewable evidence before the next slice |
| --- | --- | --- |
| 1. Contract and fixtures | Define `HomeShelf`/`HomeFeedSource`; save small sanitized test responses covering song, playlist, album, mixed and unsupported items; write a bounded parser. | JVM tests for mapping, row order, empty/malformed responses, continuation cap and cancellation/oversize handling. No UI yet. |
| 2. Openable routes | Connect playlist and album cards to working collection routes, and songs to shelf-context playback. Filter unsupported item kinds. | Route tests plus a manual open/play check. No card that cannot act. |
| 3. State and refresh | Add discovery-feed state beside the existing Room-backed mix state; add memory freshness, stale-while-refresh behavior and retry. Keep Library mixes and the existing refresh seed working. | ViewModel tests for initial, success, error, repeat tab entry and manual refresh; verify Library mix refresh still changes the mix. |
| 4. UI and motion | Add chips, titled shelves, Material multi-browse cards, caption, badges and reduced-motion fallback. Remove duplicated local Home sections; move mix refresh into Library > Mixes and rely on the mini-player for Now playing. | Compose screenshots/tests at narrow, normal, expanded, dark/light and large-font widths; a swipe/tap check on a phone; verify all former Home content/actions are reachable in Library or mini-player. |
| 5. Finish | Check network resilience, screen-reader labels, image loading and release-size impact. | Appropriate Gradle tests/lint and a release-mode scroll check. APK packaging/installation only if requested at that stage. |

## Acceptance criteria

- Provider-returned row titles and item order appear without hardcoded `Quick picks`/`Hindi Hits`; five rows is an upper target, not a promise that all five exist.
- Every visible online card has one working tap action: song starts playback; playlist/album opens usable tracks. No card has an embedded play button.
- The carousel expands/compresses neighbors smoothly during a swipe on normal-motion settings; reduced-motion mode has no resizing/fade animation. Returning from a detail page preserves Home's vertical and horizontal positions.
- Chips launch their corresponding song searches. Provider loading/failure never alters Library mixes, recents, pinned playlists or liked songs; private mode still suppresses recents. Home contains no duplicated local sections after the new feed is ready.
- `Refresh mixes` works in Library > Mixes; Now playing remains reachable through the mini-player. Empty/offline Home shows a concise retry state and keeps navigation accessible.
- One tab switch does not trigger a new network request. A failed manual refresh keeps the previous successful shelves and offers retry. Unknown/unsupported renderer types are skipped without a crash.
- No Python runtime, login flow, new permission or arbitrary hardcoded colors are added. Validate the actual APK size only after implementation, rather than assuming the result.

## Known limits and decisions left for implementation testing

The YouTube Music Home endpoint is unofficial and its row format may change. Live provider behavior, album route viability, and exact carousel sizing must be verified during implementation on the user's Android device. If the provider supplies no working album route, the safe first release displays song/playlist cards and records album support as incomplete; it does not show dead albums. The example app's detail animation is not a requirement to replace SungMobile's existing collection overlay.
