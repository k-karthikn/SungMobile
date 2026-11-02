# Spotify library import performance

## Comparison

The reference checkout is `/home/karthi/Projects/LastWave-Native`.

Both apps fetch Spotify Liked Songs from Pathfinder in pages of 50. SungMobile permits up to 100 pages while LastWave stops after 20, so Spotify retrieval can make more requests for libraries over 1,000 songs. For a 400-song library, both need about eight pages; the large difference is the follow-up YouTube Music match:

- LastWave's `SpotifyPlaylistImporter.fetchAndMatchLikedSongs()` calls `InnerTubeMusicApi.searchSongs()` with a four-request semaphore. That path posts directly to YouTube Music's search endpoint and parses song renderers.
- SungMobile previously called `CatalogSource.search()` for each Spotify row. `YouTubeSource` routed those NewPipe searches through a two-thread `ExtractorExecution`; a single search can involve more extractor work than one direct search request.

The reference implementation also searches once per row, so it does not support a claim that 400 YouTube matches complete in one second. The reported time may describe Spotify retrieval rather than the full match. SungMobile now logs Spotify fetch and match durations separately to make that distinction measurable.

## Implemented changes

1. Added `CatalogSource.searchSongsForMatching()`, with a default implementation using the existing catalog search. Other catalog providers and test fakes keep their existing behavior.
2. Added a direct YouTube Music search implementation for bulk matching. It reads the current WEB_REMIX API key, client version, and visitor data from the Music homepage once per source instance, then uses the search endpoint and the existing bounded JSON parser patterns. Normal catalog search behavior is unchanged.
3. Kept NewPipe catalog search as a fallback if the direct request fails, parsing fails, or returns no candidates.
4. Increased the import match fan-out from three to four and grouped identical normalized title/artist queries so duplicate Spotify rows only need one YouTube search.
5. Added count-and-duration log entries for Spotify retrieval and matching. These entries do not include track titles, artists, account data, or credentials.

This is an adaptation of the direct-search approach, **not an exact port of LastWave's `InnerTubeMusicApi.searchSongs()`**. LastWave has additional search recovery paths, including an unfiltered retry, refreshed visitor data, standard YouTube search fallback, and a richer candidate parser. SungMobile currently uses its own bounded parser and falls back to NewPipe if direct search fails or returns no candidates. Therefore this change is an optimization attempt, not a confirmed fix.

## Recorded timing

| Stage | Result |
|---|---|
| Before optimization | One test, 262 tracks, `01min:01sec:24` (recorded verbatim; timing format was not clarified) |
| After optimization | Not tested yet |

The before result was supplied by the user. No after-optimization timing or device verification is available yet.

## Measurement and follow-up

Use the `SpotifyLibraryImport` log tag on a device and record the `Fetched …` and `Matched …` durations for the same account and network. The first direct search may include a one-time Music homepage request to load current endpoint configuration. Compare the timings after that warm-up as well as for the first import.

If matching remains slow, use the per-stage timings to investigate request latency or fallback frequency before increasing concurrency. YouTube Music's web endpoint is undocumented and may change; the NewPipe fallback is retained for availability, but fallback-heavy sessions can still be slower.

## Verification status

Compilation could not be completed in the current environment because Gradle failed with `Could not determine a usable wildcard IP`. Tests were not run. Device verification should include a collection with several hundred tracks, a smaller playlist, duplicate titles, intermittent network failure, and a Spotify session that needs reconnecting. Confirm that match count/order and import results remain correct while reviewing the separate fetch and match timings.
