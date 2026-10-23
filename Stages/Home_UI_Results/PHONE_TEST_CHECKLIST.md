# Home UI — manual POCO check

Build: 0.11.0-home-ui, code 17. Update over the installed Sung app; do not uninstall. The package and debug signing identity are retained.

- [ ] Home follows the selected Sung blue/wallpaper palette in light and dark mode. Appearance opens from the top-right icon; the search entry opens Search.
- [ ] Liked shuffle and Recent mix show actual artwork and counts. View, Play and Save work; Refresh mixes updates them. Now playing opens the current player.
- [ ] Tapping a heart immediately toggles Like without starting a song. Plus opens the familiar menu. Add to playlist, Play next and Add to queue work; tapping the song itself starts playback.
- [ ] Recently played appears above pinned playlists and Liked songs. View all opens the correct Library list. Home retains its scroll position after Search/Library or player navigation.
- [ ] Private mode hides Recent mix and Recently played, but keeps deliberate Likes and pins.
- [ ] The shared mini-player works on Home, Search, Library and album/playlist detail pages. Play/pause and progress remain live. Full-player seek, synced lyrics and the sleep-timer countdown still use live time.
- [ ] Scroll Home repeatedly while playing, then while paused. Compare the first pass with repeated passes after artwork has loaded. Report where lag remains, whether playback was active, and whether images were already visible; no links or USB are needed.
- [ ] Larger system font/display size leaves buttons reachable and readable. The mix cards stack when needed. Reduced motion removes shape/color motion. Check the heart and plus touch targets and TalkBack labels if you use TalkBack.
- [ ] Likes, playlists, settings and the restored queue remain after updating and restarting.

Phone acceptance and measured frame/battery performance are pending. Host rendering does not establish POCO scrolling smoothness.
