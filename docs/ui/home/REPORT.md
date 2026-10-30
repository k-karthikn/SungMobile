# Home UI implementation

19 October 2026. Build `0.11.0-home-ui`, code 17. The owner requested page-by-page UI work and approved the Home reference with direct heart toggles, the existing plus menu, and pinned/Liked sections retained below Recent. Phone acceptance remains pending.

## What changed

- Larger Sung heading and tagline; Appearance uses an accessible tune icon. The reusable search entry opens the existing Search destination.
- Now playing and Refresh mixes use M3 Expressive tonal pills with rounded Material Symbols and standard pressed-shape motion.
- Artwork cards replace the two outlined mix rows. Each has actual song artwork, count, View, Play and Save controls. Cards stack on narrow layouts or above 130% font size; content is never forced into a clipped fixed-height card.
- Recent songs use a reusable provider-neutral song layout with immediate Like/Unlike and the existing plus menu. That menu still includes library and queue actions. Pinned playlists and the Liked list follow Recent. Private-mode hiding and captured playback snapshots are retained.
- The single shared mini-player and bottom navigation are updated across destinations. The navigation selection includes both icon and label inside its pill; standard Material selectable surfaces, ripple and theme motion handle interaction. Expanded layouts retain the navigation rail. No copy of these controls was added to individual pages.
- Colors come exclusively from the current Material color scheme. Sung blue, wallpaper colors, light/dark, fonts, reduced motion and system insets remain in place. Artwork stays separate from readable text.

## Scrolling-related changes and limits

Code inspection found full playback state collected at the application root and Home, plus full queue reconstruction on each 250 ms position tick. These are confirmed repeated operations, not a measured attribution of the owner's lag.

- Root/navigation consume distinct state without the ticking position/countdown. Player, Lyrics and saved-session screens still observe the original live state. Mini-player progress reads live position in its progress lambda, leaving artwork and text outside the clock subscription.
- Home observes only connected/has-track flags. Its lazy items have stable keys and content types, and preview lists are remembered. Home's lazy-list state survives destination changes.
- The controller clock poll updates timing without remapping every queue entry; track/count mismatches fall back to a full snapshot. Timeline and session-extra listeners still refresh full state. A real Media3 controller test checks retained queue identity during a clock update and rejection after a timeline change.
- Library action context is remembered and observed only by consumers. Artwork keeps one bounded Coil loader, layout-sized decoding, no crossfades, and no full-size bitmap collage generation. The tile shape is reused.

No POCO frame-time, GPU, memory or battery measurements were taken. This remains a development APK; manual first-load and warm-scroll comparisons are in the phone checklist. Further profiling depends on the owner's result. Do not describe this build as proven lag-free.

## Ownership

| Location | Responsibility |
| --- | --- |
| `feature/home/DiscoveryHomeScreen.kt` | Section ordering, lazy keys, data/privacy wiring and responsive card layout |
| `feature/home/HomeHeader.kt`, `HomeMixCard.kt`, `HomeSectionHeading.kt`, `HomeSongRow.kt` | Focused Home compositions and actions |
| `core/designsystem/component/SungSearchEntry.kt`, `SungSongListItem.kt` | Reusable layouts with callbacks/slots, no repositories or provider types |
| `app/SungNavigationBar.kt`, `feature/player/MiniPlayer.kt` | Single shared bottom surfaces |
| `app/PlaybackUiUpdates.kt` | Chrome/live-clock subscription boundary |
| `playback/PlaybackConnection.kt`, `SessionStateMapper.kt` | Lightweight position polling |

App ID remains `com.karthi.sungmobile.stage0`; Room schema stays 5 and storage formats do not change. Stage 7 remains deferred and Stage 10 skipped. No new provider, dependency, permission, download or sign-in flow is added.

## Verification

The final run passed all 64 app tests, including 10 new Home UI cases and the Media3 controller integration tests. Lint has 0 errors and 13 dependency-version notices; APK assembly passed. The saved package/version and certificate were verified. See `Stages/Home_UI_Results/` (files removed 30 October 2026) for the completed build/check summary, signature, checksum and manual phone checklist. UI tests render actual Discovery Home with offline artwork, cover heart/menu/mix actions, privacy and scroll restoration, and check that live seek/countdown values survive the clock subscription split. The existing app regression suite also covers shared navigation/player behavior.

## References used

- [Compose performance guidance](https://developer.android.com/develop/ui/compose/performance/bestpractices): defer state reads and keep frequent changes near their consumers.
- [Compose lazy lists](https://developer.android.com/develop/ui/compose/lists): stable keys and content types for item reuse.
- [Material icon button source](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/IconButton.kt): expressive shape interactions; compilation validates the project's pinned 1.5.0-beta01 API.
- [Material Symbols](https://github.com/google/material-design-icons): new Rounded symbols use the already bundled Apache-2.0 license.
