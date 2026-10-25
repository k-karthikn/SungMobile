# SungMobile Home: source and motion research

Research date: 25 October 2026. This is an investigation and design reference, not an implementation report.

## References inspected

- Sung desktop source at `/home/karthi/Projects/Sung/`, commit `2edcaff`; detailed content trace: [SUNG_DESKTOP_CONTENT_RESEARCH.md](SUNG_DESKTOP_CONTENT_RESEARCH.md).
- The supplied Sung desktop Home screenshot: `Listen`, four mood chips, `Quick picks`, and `Hindi Hits` shelves. The supplied crop of the chips confirms their simple outlined treatment.
- The supplied 15.58-second, 842 × 1920 mobile screen recording, `/home/karthi/Downloads/Screenrecorder-2026-10-25-16-49-04-500.mp4`, plus the supplied card crop. The recording shows a horizontal artwork carousel at about 0–8 seconds, a collection detail opening at about 8–11 seconds, and a return to the feed afterward. Its cards have large rounded corners, partially visible neighbors that expand and compress during a swipe, and a small type badge. The example app has extra play controls; the user explicitly wants **no per-card play button** in SungMobile.

## What can be reused

Sung desktop requests `YTMusic.get_home(limit=5)`, which returns provider-titled, mixed-content rows. The installed `ytmusicapi` 1.12.3 sends a YouTube Music `browse` request for `FEmusic_home`, then follows row continuations to reach the requested count (`Sung/helper/catalog.py:402-408`; installed `ytmusicapi/mixins/browsing.py:30-37,115-137`). Its mixed-row parser recognizes song/watch-playlist, album, artist, playlist and other renderer variants (`ytmusicapi/parsers/browsing.py:11-61`). Sung maps these to a common item shape (`Sung/helper/catalog.py:9-53`) and displays each row as a carousel (`Sung/qml/Main.qml:1257-1291`). The four mood chips are fixed song searches (`Sung/qml/Main.qml:1075-1080`), unrelated to Home feed extraction. The provider chooses titles such as `Quick picks` and `Hindi Hits`; these are examples, not contractual row names.

SungMobile already has `CatalogItem` and `CatalogKind`, a `CatalogSource` for search/open, OkHttp plus Gson in `core:youtube`, a `YouTubeLyricsGateway` that makes cancellable YouTube Music `browse` requests, and a Home ViewModel with local Room-backed mixes/recents/pins (`core/model/.../Catalog.kt`, `core/youtube/.../YouTubeLyricsGateway.kt`, `app/.../HomeViewModel.kt`). Its current `CatalogSource` lacks Home shelves, and its `CatalogExtractor.open()` rejects non-channel browse IDs such as most album IDs. The existing Home refresh only changes a local shuffle seed. These are the integration gaps; they cannot be solved by changing the cards alone.

## Design interpretation

Use the video for **carousel motion and card proportions**, not its branding, dark color values, fourth navigation destination, menu icons, or collection-page controls. Use Sung for **content source and shelf semantics**. Keep SungMobile’s current Material color roles, shared mini-player and navigation, and existing local Home features. A single card tap has one action: **track → start playback; playlist/album → open its detail page**. For any other provider item, show a card only after it has a working destination.

Material 3 Compose’s `HorizontalMultiBrowseCarousel` is a strong fit because it sizes and masks adjacent items as they scroll; `CarouselState` exposes the snapped item for a separate title/artist caption. This is a design inference from the recording, not a claim that the example app used Compose. Official references: [Carousel guidance](https://developer.android.com/develop/ui/compose/components/carousel), [carousel API](https://developer.android.com/reference/kotlin/androidx/compose/material3/carousel/HorizontalMultiBrowseCarousel.composable), [CarouselState API](https://developer.android.com/reference/kotlin/androidx/compose/material3/carousel/CarouselState). SungMobile pins Material 3 `1.5.0-beta01` (`gradle/libs.versions.toml`); compile against that pinned version when implementation begins.

## Risks established by source review

1. The Home endpoint and renderer tree are unofficial and may change. Port only the needed parsing paths and keep recorded response fixtures for regression checks. [ytmusicapi's browsing reference](https://ytmusicapi.readthedocs.io/en/stable/reference/browsing.html) describes variable row content.
2. A shelf can mix songs, albums, playlists, artists and unsupported content. A pretty but unopenable album card would be a functional failure; route coverage must precede showing that card type.
3. Current Home local sections are instantaneous Room flows, while online shelves need network and may fail. They need independent loading/error state so local music remains usable.
4. The recording’s compressed cards can make artwork and badges too small to read. A stable caption and accessible card label are needed; text should not be squeezed into the small edge slivers.
5. The user previously prioritized a small production APK. The Android plan should use its existing Kotlin/OkHttp/JSON stack, not package Sung’s Python runtime or the entire desktop application.

No app code, build, provider request, or installed APK was changed during this research.
