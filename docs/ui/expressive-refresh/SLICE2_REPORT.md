# Expressive UI refresh — slice 2: Library

Date: 24 October 2026. Scope: Library navigation and controls, plus the History & privacy entry in Settings. Source only; no APK was built or installed.

Library now has underlined **Liked / Playlists / Mixes / Recents / Sessions** tabs. Recents uses the existing listening-history collection, so Home's “Recently played → View all” still opens the same data. Sessions opens its existing screen. The top actions open Settings and Search. The Mixes tab shows only actual Liked shuffle and Recent mix data, then up to six saved smart playlists; Manage opens the full smart-playlist screen, and Listening statistics remains reachable there. Private mode continues to hide the Recent mix. No local-files, device-output or other screenshot-only features were added.

Liked, Recents and playlist details now use a compact Play button with a split menu for Shuffle all, Play next and Add to queue. These commands use a full collection snapshot rather than only the visible page. Filter and sort stay available; the selected sort is marked by both color and a check. Playlist creation, selection and batch operations, per-song menus, occurrence movement/removal, cleanup and versions remain. History & privacy moved from Library's overflow into Settings, retaining its private-mode, recording and clear-history controls. Back from that page returns to Settings.

Implementation is split among the Library header, list controls, Mixes content, existing ViewModels and a small Settings entry component. The shared mini-player and bottom navigation from slice 1 remain unchanged.

Verification: debug Kotlin compilation succeeded; all 84 app unit tests passed, including seven focused Library UI tests. Debug lint reported zero errors and the same 22 pre-existing warnings. An optimized, signed 0.12.5 code-25 production APK containing this slice was then built and release-linted; see [the artifact record](../../../Stages/Expressive_Library_Results/README.md). The owner will check the layout and interactions on a phone.

Next planned slice: Home, using only its existing mixes, recently played songs, pinned playlists and liked songs.
