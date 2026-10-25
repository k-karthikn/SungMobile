# How Sung fills its Home screen

Research snapshot: 25 October 2026, Sung desktop commit `2edcaff`. The two Home screenshots supplied in this conversation are the visual reference: the full desktop `Listen` page and a close-up of its four mood chips. This document records the existing implementation for later use; it does not propose that the current SungMobile Home has the same content.

## Main finding

Sung's online Home shelves are **YouTube Music Home results**, not lists composed from Sung's local likes, history, or search terms. On opening Home, Sung calls `ytmusicapi.YTMusic.get_home(limit=5)`, converts its titled rows into a small common item model, and renders each row as a horizontal carousel. Thus `Quick picks` and `Hindi Hits` in the screenshot are titles returned by the provider at that moment. Their presence, order, contents, and availability can change; Sung does not hardcode those sections.

The `Feel good`, `Focus`, `Unwind`, and `Energize` chips are the opposite: Sung hardcodes four labels and query strings. Tapping one opens Search with the song filter; it does not filter or regenerate the Home feed.

## Request-to-screen path

| Step | Sung source | Actual behavior |
| --- | --- | --- |
| Enter Home | `src/main.cpp:496-505`, `src/presentationfeatures.cpp:6-7`, `qml/Main.qml:519-527` | On ordinary startup, open the selected start page (Home by default). The Home navigation destination calls `app.home()`. |
| Request | `src/backend.cpp:380,395-429` | `home()` issues `{"op":"home"}` on the `catalog` channel. `browseRequest` sets the `Listen` title and busy state, then accepts the returned `sections`. |
| Worker transport | `src/backend.cpp:206-277` | A fresh Python helper process receives one JSON request over stdin and returns JSON over stdout. A new request cancels the previous request on that channel; the C++ watchdog allows 45 seconds for catalog work. |
| Provider call | `helper/catalog.py:402-408` | Constructs `YTMusic(requests_session=True)` without supplying account credentials, sets a default 20-second network-request timeout, and calls `api.get_home(limit=5)`. The installed `ytmusicapi` 1.12.3 implementation documents `limit` as the **number of rows** and requests `browseId=FEmusic_home`, following continuations when supplied. |
| Normalize | `helper/catalog.py:9-53,406-408` | Each nonempty provider row becomes `{title, items}`. `clean()` skips malformed/no-ID entries; `normalize()` maps song/video, album, artist and playlist fields into Sung's common `id`, `kind`, title, artist, artwork, duration, etc. Artwork prefers the last thumbnail and asks Google's image host for a larger 544 px variant when applicable. |
| Present | `qml/Main.qml:88-106,1257-1291`; `qml/ArtCard.qml:3-62` | QML reads `app.homeSections()`, gives each row a heading and `MCarousel`, and renders each entry as an artwork card. Selecting a song/video plays it; selecting a collection opens it (`src/backend.cpp:477-491`). |

`ytmusicapi` source checked for the row and endpoint behavior: `/home/karthi/.local/lib/sung/runtime/lib/python3.14/site-packages/ytmusicapi/mixins/browsing.py:30-37,115-137`. This is a description of the installed library, not a contract that YouTube Music will keep the same response format.

### Data shape

The worker returns the following conceptual shape (titles and entries are provider data):

```json
{
  "ok": true,
  "sections": [
    {
      "title": "Quick picks",
      "items": [
        {"id": "…", "videoId": "…", "kind": "song", "title": "…", "artist": "…", "art": "…"}
      ]
    }
  ]
}
```

The screenshot's `Hindi Hits` row has the same data path. Sung does not issue a separate `Hindi Hits` query. The Home request does not send Sung's listening-history IDs, liked-song IDs, region, or mood-chip choice. Because the `YTMusic` instance has no supplied authentication, treat the output as provider-controlled suggestions, **not guaranteed account-personalized recommendations**. The exact reason a particular row appeared in the screenshot cannot be established from Sung's code alone.

## Local additions and controls

Sung has one local contribution to the feed: `Backend::homeSections()` prepends a `Pinned` row when the user has pinned collections, then applies saved row order and hidden-row titles (`src/presentationfeatures.cpp:8-34`). Pins persist in Sung's library document (`src/backend.cpp:1406-1419`). The settings icon opens `qml/HomeEditor.qml:4-34`, where users can hide, reorder, or reset rows. Hiding and reordering are keyed by **row title**, which is simple but can be fragile if a provider title changes or repeats. `Pinned` can still appear when online shelves are empty (`qml/Main.qml:88-96`).

The refresh button and pull-to-refresh both call `app.refresh()` (`qml/Main.qml:997-1004,1183-1194`); `Backend::refresh()` repeats the remembered Home request (`src/backend.cpp:440-446`). The app keeps the current sections visible while a same-page refresh is busy and leaves them in place on a failed request (`src/backend.cpp:401-429`). It also keeps a view snapshot for Back navigation (`src/backend.cpp:297-317,330-357`). These are in-memory navigation/current-view states; the traced Home code does **not** persist a downloaded Home feed for offline reuse.

The mood chips are defined in `qml/Main.qml:1075-1080`:

| Chip | Song-search query |
| --- | --- |
| Feel good | `feel good songs` |
| Focus | `instrumental focus` |
| Unwind | `chill evening` |
| Energize | `workout energy` |

The search path is `Backend::search()` → `op=search` → `api.search(query, filter="songs", limit=30)` (`src/backend.cpp:381-394`, `helper/catalog.py:409-411`). These are user-triggered discovery shortcuts, not four independent Home shelf algorithms.

## What SungMobile has today

SungMobile's `DiscoveryHomeScreen` shows `Your mixes`, Recently played, pinned playlists, and liked songs from its own `HomeState` (`app/src/main/java/com/karthi/sungmobile/feature/home/DiscoveryHomeScreen.kt:28-105`). `RoomHomeRepository` observes Room changes and reads up to 50 likes and recents plus up to 12 pinned playlists in one transaction (`core/data/src/main/java/com/karthi/sungmobile/core/data/discovery/RoomHomeRepository.kt:9-21`). `HomeViewModel` creates deterministic local shuffle mixes and a refresh changes that shuffle seed (`app/src/main/java/com/karthi/sungmobile/feature/home/HomeViewModel.kt:9-29`; `core/model/src/main/kotlin/com/karthi/sungmobile/core/model/discovery/HomeData.kt:8-12`). Its refresh is therefore **not** a provider Home refresh.

SungMobile's `CatalogSource` currently exposes search, pagination and open, but no Home-feed operation (`core/model/src/main/kotlin/com/karthi/sungmobile/core/model/Catalog.kt:33-37`). Its YouTube implementation uses NewPipe extraction, not Sung's Python `ytmusicapi` (`core/youtube/src/main/kotlin/com/karthi/sungmobile/core/youtube/YouTubeSource.kt:9-28`). It can search music categories, but its `open()` path currently rejects non-channel YouTube Music browse IDs (`core/youtube/src/main/kotlin/com/karthi/sungmobile/core/youtube/catalog/CatalogExtractor.kt:13-24,62-65`). So merely copying the screenshot, adding the four searches, or reusing the local mixes would **not** reproduce Sung's provider shelves or make every returned card open correctly.

## Reuse strategy for a later implementation

1. Keep a provider-neutral `HomeShelf(title, items)` model separate from local Home data. If online shelves are added, use a dedicated catalog capability and map provider results to existing `CatalogItem` types. Preserve local mixes/recents/pins as their own sections rather than claiming they are YouTube Music `Quick picks`.
2. Fetch provider shelves off the UI thread with cancellation and a bounded timeout. Retain the last visible result during refresh, provide an explicit retry on failure, and decide separately whether a bounded disk cache is appropriate for offline Home. Sung itself only retains in-memory view state.
3. Handle mixed shelf item types before displaying them: songs/videos should play; album, artist and playlist cards must lead to working detail routes. The current unsupported browse-ID route is a concrete integration gap.
4. Keep the mood chips as explicit song searches if the desired behavior is the same as Sung's. Their labels/queries are presentation choices, not evidence of personalized mood recommendations.
5. Treat provider titles as display text and preserve order as returned. If user-controlled hide/reorder is later added, prefer a stable section identity or a documented title-based fallback because titles can change.
6. Verify with a deterministic fixture containing a song shelf, a mixed album/playlist shelf, an empty shelf, an error, and a refresh. Then verify the real service separately; the screenshot cannot guarantee future titles or rankings.

No SungMobile code or provider integration was changed as part of this research.
