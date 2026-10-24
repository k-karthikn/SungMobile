# Expressive UI refresh — slice 1

Date: 24 October 2026. Scope: shared mini-player and bottom navigation. No APK was built or installed; phone visual acceptance remains with the owner.

The mini-player stays in `SungApp.kt`, shared by Home, Search and Library. `MiniPlayer.kt` now shows previous/play/next/More beside actual artwork and title on phones with room. On narrower widths or larger text it drops the skip buttons to protect title space; those controls remain in Now Playing. Play uses a checked expressive shape and loading state. `MiniPlayerProgress.kt` reads the live playback clock locally, shows elapsed/total time and a slim wavy played segment, and allows seeking only for a real, seekable duration. The existing full-player wave draw remains the shared presentation. `MiniPlayerMenu.kt` exposes Lyrics, the existing Queue sheet, Like/Unlike, Shuffle, Repeat and the existing per-song volume trim. It does not offer unsupported audio-output routing.

The volume control was extracted to `TrackVolumeControl.kt` so the mini-player dialog and full-player More sheet use the same -12 to 0 dB behavior. The menu's Queue action opens Now Playing with its existing draggable Queue sheet expanded. `SungNavigationBar.kt` now uses a 56 dp minimum item height plus 4 dp container padding, while keeping three destinations and a touch area above 48 dp. `SungApp.kt` keeps keyboard/system inset and wide-screen rail behavior.

Verification: `:app:compileDebugKotlin`, all 82 `:app:testDebugUnitTest` tests, and `:app:lintDebug` passed offline. Lint reported 0 errors and 22 warnings in unrelated existing updater/resources code. The focused Compose tests cover transport callbacks without unwanted navigation, elapsed/duration labels, Shuffle and volume menu actions, and opening the expanded Queue sheet. No database, playback engine, package ID, signer or version change.

Next planned slice: Library tabs and layout, using **Liked / Playlists / Mixes / Recents / Sessions** and moving History & privacy controls into Settings without losing them. Home, Search and Lyrics redesigns remain after Library.
