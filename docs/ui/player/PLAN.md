# Player UI — approved implementation slice

21 October 2026. The owner authorized implementation after approving the queue-preview and More-sheet arrangement. The supplied images guide composition; existing Sung theme colors and all features remain authoritative.

1. Now Playing: actual neighboring queue occurrences, tappable previews, expressive artwork, compact transport and standard accessible scrubbing with a draw-only wave.
2. Keep Queue, Lyrics, Speed, Sleep timer and active countdown directly available. Keep resume/start-over prompts available.
3. Group Smart shuffle, Related autoplay, song attenuation, Saved sessions and Stop in More. Preserve Like and playlist actions.
4. Queue: reuse the shared artwork/song row while retaining occurrence identity, reordering, removal, Undo, clear-upcoming confirmation and save-as-playlist.
5. Verify with host tests, light/dark/large-text layouts, lint and a same-signature APK. Owner tests manually.

The wave runs only during resumed foreground playback, stops during scrubbing, and is straight when Reduced Motion is enabled. No continuously rotating artwork or new image effects. Main artwork crossfades briefly using the existing image loader.

Neighbor previews follow the current displayed queue order, including shuffle. Queue boundaries hide missing previews, even with repeat-all; the transport Next action retains its existing repeat behavior. Duplicate occurrences may legitimately show identical artwork, but tapping selects the exact entry ID.

Follow-up slices: Search; Library/collections; Lyrics/settings/dialogs. No new media source, downloads, local-file scanning, Cast, account flow or feature removal. The owner reports Home code 17 scrolling did not improve and deferred profiling; APK optimization is an unconfirmed hypothesis.
