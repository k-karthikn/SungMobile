# Provider-backed Home implementation

25 October 2026. This implements the [Home plan](PROVIDER_HOME_IMPLEMENTATION_PLAN.md) using Sung's `FEmusic_home` approach documented in the [desktop content research](SUNG_DESKTOP_CONTENT_RESEARCH.md). The UI uses the supplied carousel video as its motion reference. The provider chooses shelf names, artwork, item order, and content; SungMobile does not synthesize screenshot-like rows.

## What shipped

- `core:model` exposes `HomeFeedSource` and typed `HomeShelf` data. `core:youtube` requests anonymous YouTube Music browse pages with bounded response size, timeout, shelf/item counts, and continuation depth. It maps only playable songs and openable playlist/album cards; unsupported renderers are skipped.
- Album and playlist cards open collection pages with real provider tracks. Song cards start playback in the playable-song context of their shelf. Cards have no embedded play button.
- `DiscoveryFeedViewModel` holds the online feed separately from the existing Room-backed Home/mix state. Returning to Home within 20 minutes reuses the successful in-memory feed. Manual refresh preserves visible shelves on failure.
- Home shows search/settings, `Listen`, a refresh icon, four compact 8 dp-corner Material `AssistChip` shortcuts, and titled square-artwork carousels. The Material 3 multi-browse carousel morphs card widths while scrolling; reduced-motion mode uses a plain horizontal list. Captions and album/playlist badges use theme colors.
- Local mixes, recent plays, liked songs, and pinned playlists remain in Library. The mix refresh action moved to Library > Mixes. The persistent mini-player remains the Now Playing entry point.

## Verification

The `core:youtube` and app debug unit suites passed, including parser, continuation, collection, state, action, and Robolectric UI checks. Home screenshots were reviewed at 320 dp and 393 dp, expanded width, dark and light themes, 2× text, and reduced motion. Release lint passed with no reported issues. Anonymous live provider samples confirmed usable Home shelves and album/playlist track pages; those samples are not packaged or committed.

The endpoint is unofficial and may change its renderer or access behavior. The feed is cached only in memory, so a cold offline start offers Retry and Library rather than old recommendations. Physical-phone review of the final carousel motion and collection playback remains useful after installing the APK.

The signed production build is 0.13.0 (code 32) (files removed 30 October 2026). It was built and verified locally; no phone installation or release publication was part of this implementation.
