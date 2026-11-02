# Playlist Generator

The Playlist Generator creates a previewable, editable local playlist from selected song seeds or the listener's existing SungMobile taste signals. It is available from the Playlists section and saves through the existing library repository.

## Candidate generation

- A listener can choose up to four songs from their current track, library suggestions, or song search.
- With no manually selected seeds, generation samples liked and saved songs plus top and recent tracks when listening history is enabled.
- Up to four seed expansion requests run concurrently. Each asks the active music provider for at most five related songs.
- Provider Home recommendations fill out the candidate pool and are cached for 20 minutes.
- Local seed tracks are included as candidates so the familiar-to-new control can influence the finished mix.

## Ranking

`DiscoveryPlaylistRanker` uses a deterministic score from seed strength, support from multiple related-song results, likes, saved status, top-play count, recent-play penalty, and stored “more like this” / “hide” feedback. The discovery slider changes the score given to known and unfamiliar tracks. The ranker removes duplicate song identities and limits an artist to three songs when enough alternatives exist.

The user can preview and play the generated mix, remove individual songs, generate another ordering, and save it as a regular editable playlist. It does not create a rules-based Smart Playlist.

## Privacy and scope

The feature uses SungMobile's local recommendation profile and existing YouTube Music catalog sources. Private mode supplies no listening-history seeds; manually selected songs remain usable. Last.fm and external API credentials are not required.

This is a bounded heuristic recommender, not a trained model. Candidate quality depends on the provider's related-song and Home results. The provider currently returns at most five related songs per seed, so the generator supplements those results with local profile songs and cached Home recommendations.
