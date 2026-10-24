# Expressive UI refresh — slice 3: Home

24 October 2026. Source-only UI update; no APK was built or installed.

Home now opens with a compact Settings/Search row, a Listen heading, and the existing Now playing and Refresh mixes actions. Liked shuffle and Recent mix sit in a horizontal artwork shelf using their actual candidate covers. Recently played keeps direct heart and plus actions; pinned playlists and Liked songs remain below it. Pinned playlists use a clear collection icon with the real name and song count because the current Home model does not contain playlist cover art. No recommendation feed or new music source was added.

The existing playback snapshots, private-mode hiding, responsive scrolling, View/Play/Save mix actions, playlist navigation and shared mini-player are preserved. The Home search entry uses a short single-line label so it stays compact at increased font sizes.

Verification: all 11 Home UI tests passed, including pinned-playlist navigation, privacy and action callbacks. Light, large-text and scrolled pinned-shelf host renders were inspected; the shared-shell tests and debug lint passed. Manual phone acceptance remains with the owner. The previously built code-25 APK does not contain this source update.
