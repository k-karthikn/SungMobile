# Stage 1 — Design foundation

Implemented on 2026-10-10. Host verification is complete; the POCO device and visual acceptance check are left to the user. No device installation or device interaction was performed during this stage.

## Delivered

- A separate `core:designsystem` module with no provider, catalog-model or playback dependency.
- Official Compose Material 3 Expressive theme, component shapes and motion. Complete blue light/dark role tables; default follows the system light/dark mode. Optional Android wallpaper colors.
- Bundled Google Sans Flex across the normal and emphasized M3 typography roles, with its OFL/trademark notices.
- Fourteen Material Symbols Rounded vectors converted from the supplied Linux assets, with correct negative-viewBox translation and Apache license text.
- Shared Coil loader with 32 MiB memory / 96 MiB disk cache limits; center-cropped real artwork, tile/circle/pill/flower masks, and neutral missing-art fallback. Rotated mask bounds are fitted while the picture stays upright.
- Home/Search/Library navigation, a shared mini-player, full-player layout and Appearance screen. Expanded windows use a navigation rail. Search, browsing, play/pause, replay, seek and stop reuse the Stage 0 core.
- Local persistence for theme/color/reduced-motion preferences. System-disabled animations are respected; no continuous artwork animation is introduced.
- Feature packages, typed callback groups, six Android Studio preview configurations, offline host renders, and a [guide for extending the design system](DESIGN_SYSTEM.md).

Home uses actual results from the current search/collection. A fresh install has a search invitation. Library explicitly explains that saving is still to come. Preview/test data is never injected into the production catalog.

## Verification

| Check | Result |
| --- | --- |
| Core model | 4 tests passed |
| Provider HTTP adapter | 3 tests passed |
| Playback coordinator | 7 tests passed |
| App ViewModel | 4 tests passed |
| Search UI on the host | 3 tests passed |
| Appearance persistence, system motion and text contrast | 4 tests passed |
| Design renders, navigation, player callbacks and touch targets | 15 tests passed |
| **Total deterministic tests** | **40 passed; 0 failures** |
| Live YouTube probes | 4 intentionally skipped; Stage 1 does not change the provider |
| Debug APK | Built, version `0.2.0-stage1`, code `3` |
| Instrumentation APK | Built; not executed on the phone |
| Android lint | 0 errors; 10 newer-version advisories |

The core regression run and final app verification logs are in `Stages/Stage1_Results/`. Commands:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew \
  :core:model:test :core:youtube:test :core:playback:testDebugUnitTest \
  :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Twenty-one PNGs are in [Stage1_Results](../Stages/Stage1_Results/README.md). Actual Compose layouts were rendered and inspected at 393 dp width in light/dark, 200% font scale and reduced motion, plus 320 dp narrow Home and 1000 dp expanded Home. The large player button retains its touch target; long content scrolls. Fresh Home, Search, Library and Appearance states were also inspected. The selected readable blue color pairs meet 4.5:1 contrast in both themes.

The visual pass corrected a squeezed play button at 200% text, clipping at rotated artwork-mask bounds, and search-control spacing. Host image loading uses Coil's real fetch/cache pipeline with its BitmapFactory decoder; Robolectric cannot execute Android ImageDecoder's file-descriptor path. Production keeps Coil's default decoder. Fixture thumbnails are local test resources, with provenance in [third-party notices](../THIRD_PARTY_NOTICES.md).

These are host renders and behavioral tests, not phone screenshots, pixel-diff approval baselines or a complete accessibility/performance audit. TalkBack traversal, real wallpaper colors, keyboard/system insets and OEM rendering still need physical-device checking. Cache limits are configured budgets, not measured peak memory.

## Manual check on the POCO

1. Install `app/build/outputs/apk/debug/app-debug.apk`. The existing debug ID `com.karthi.sungmobile.stage0` is retained so this upgrades Stage 0. The displayed app name is now **Sung**.
2. Search a song, play it, switch to Home and tap the mini-player. Check artwork, pause/resume, seek and Stop.
3. Open Home's Appearance button. Try light/dark, Sung blue/wallpaper colors and reduced motion. Restart to check that these choices persist.
4. Increase Android font size. Scroll Home and the player, and check that controls and text remain reachable. Save any screenshots or notes in `Stages/Stage1_Results/`.

## Remaining stages

Playback is still foreground-only and plays one selected song at a time. Stage 2 owns background/screen-off playback, MediaSession/notification controls, queue and automatic next song. Discovery recommendations, full collection/artist experiences, persistent likes/playlists/history, lyrics, artwork-derived palettes and release optimization remain later work. The launcher is still the starter launcher; public-release branding/license inventory is not claimed complete.
