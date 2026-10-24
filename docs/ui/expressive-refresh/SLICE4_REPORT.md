# Expressive UI refresh — slice 4: Search and song rows

24 October 2026. Source-only UI update; no APK was built or installed.

Search uses a tonal single-line input, text-first Songs/Albums/Artists/Playlists chips with a check on the selected category, a centered empty state, and a result heading that labels the number of items shown. At the owner's request, the input, chips and Search action have rounded-square corners rather than capsule ends. The Search action still animates to a softer pressed shape through Material 3 Expressive. Recent searches, direct YouTube links, pagination, cancellation, retry, provider notices and all four catalog categories remain.

Home and Search song rows now call one `SongQuickActions` component for immediate Like/Unlike and the existing plus menu. Search marks the active song on its artwork only when the real playback state says it is playing; the selected row tint remains. A shared artwork-badge slot keeps this presentation in `SungSongListItem` without duplicating the row. Reduced-motion Search loading now shows a readable static label.

Verification: 7 Search, 11 Home and 17 shared-shell UI tests passed with no failures; debug lint passed. Host renders of empty Search, results, dark mode and 200% text were inspected. No catalog, playback, Room, package, signer or version-code change was made. Manual phone acceptance remains with the owner. The previously built code-25 APK does not contain this source update.
