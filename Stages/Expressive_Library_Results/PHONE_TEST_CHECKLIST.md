# Phone check — 0.12.5 (code 25)

Install the APK over the current **production** `com.karthi.sungmobile` app if present. Check that Likes, playlists, history, sessions and settings remain. The older `.stage0` debug app has separate data and is unaffected.

- Open Home, Search and Library with a song playing. The same compact mini-player should show artwork, title, previous/play/next, More and live seek progress; its buttons should work without opening the full player accidentally.
- In Library, visit Liked, Playlists, Mixes, Recents and Sessions. Home → Recently played → View all should land on Recents.
- In Liked, Recents and a playlist, try Play all and the split menu's Shuffle all, Play next and Add to queue. Check filtering, Latest/A–Z sorting, Select and batch actions.
- In Playlists, try opening and editing a playlist, moving/removing an occurrence, and Cleanup & versions. In Mixes, check Liked shuffle, Recent mix, saved smart playlists, Manage and Listening statistics.
- Open Settings from Library, then History & privacy. Check Private mode and history controls, then Back returns to Settings. Private mode should hide history-based Recent mix content.
- Check the Library layout in the app's light/dark palette and at a larger system font size. Confirm controls remain readable and tappable.

Report any failed step with the screen, action and result. Phone testing and installation are done by the owner; no USB connection is required for this checklist.
