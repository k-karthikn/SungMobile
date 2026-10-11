# Sung design foundation

Stage 1 builds the visual foundation around the Stage 0 core. Production screens receive real provider results. Offline screenshot fixtures live under `app/src/test` and are not APK resources.

## Where to work

| Location | Purpose |
| --- | --- |
| `core/designsystem/.../theme` | Complete light/dark blue M3 color roles, Google Sans Flex typography, spacing, expressive motion and theme options. |
| `core/designsystem/.../icon` | Typed Material Symbols Rounded vocabulary. Add vectors here when needed. |
| `core/designsystem/.../artwork` | One Coil loader; reusable tile, circle, pill and flower masks; layered artwork composition. |
| `core/designsystem/.../component` | Shared expressive button, section heading and accessible loading presentation. |
| `app/.../app/SungApp.kt` | Home/Search/Library navigation, mini-player, Now Playing and Appearance routes. |
| `app/.../feature` | Stateless feature screens. Data enters through parameters; actions leave through callbacks. |
| `app/.../app/SungViewModel.kt` | Existing search/playback coordinator wiring, renamed from `StageZeroViewModel`. |

The design-system module has no catalog, provider or player dependencies. Do not put network searches or playback ownership in composables. Keep new screens in feature packages and pass provider-neutral models from `core:model`.

## Use the foundation

Wrap the app once in `SungTheme`. The default is the system light/dark setting with Sung blue colors. Appearance allows explicit light/dark, wallpaper colors on Android 12+, and reduced motion. Artwork-derived palettes remain a later appearance feature. These preferences persist locally. Android's disabled animation setting is also observed while the UI is active.

```kotlin
SungTheme {
    SungButton(onClick = onPlay) {
        SungIcon(SungSymbol.Play)
        Text(stringResource(R.string.play))
    }
}
```

Use `MaterialTheme.colorScheme`, `typography`, `shapes`, and `motionScheme`, plus `SungSpacing`. The official expressive theme supplies the shape and motion tokens. All 30 normal/emphasized typography roles use the bundled variable font; Android supplies missing-script glyphs through its font fallback chain.

```kotlin
SungArtwork(
    model = song.artworkUrl,
    modifier = Modifier.size(144.dp),
    mask = ArtworkMask.PILL,
)
```

The pill mask is rotated, not the bitmap. `ArtworkComposition` layers three masks using supplied artwork identities. Adjacent text already names the song, so these images are decorative to TalkBack. Supply `description` when an image carries information not available elsewhere. Missing, loading and failed images retain a neutral disc placeholder. No random art or object segmentation is used.

Coil resolves target size from layout constraints, center-crops, shares requests and caches, and cancels disposed requests. Cache budgets are 32 MiB memory and 96 MiB disk. Crossfades and continuous decorative animation are intentionally absent. Coil 3.3.0 matches the Kotlin 2.2 toolchain; the current 3.6 line requires newer Kotlin metadata.

## Extend screens

Use `CatalogActions` for search, pagination and collection selection; `PlayerActions` for toggle/seek/stop. `SungApp` owns navigation only. A song selection calls the existing coordinator, and every player surface reads the same `PlaybackState`.

The Home shelf is explicitly “From your search” or “From this collection.” Fresh installs show a search invitation. Library explains that saving is not yet available. No generated recommendations, account controls, likes, queue, lyrics, or automatic next-song controls pretend to work.

New layouts must allow text wrapping and scrolling, reserve system/keyboard insets, and maintain 48 dp touch targets. Home stacks its hero at narrow widths or large font scales. Expanded windows use a rail and bounded content width. Player controls scroll into view at large font sizes.

## Verify changes

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

`DesignFoundationTest` uses Robolectric native graphics and Roborazzi to render actual Compose UI, with test-only local YouTube thumbnail fixtures. The host uses Coil's BitmapFactory decoder because Robolectric cannot run Android ImageDecoder's file-descriptor decoding path; production retains Coil's default decoder. Images are written to `app/build/outputs/design-foundation/`. Review Home and Player in light/dark, 200% text, reduced motion, and narrow/expanded widths. These tests render current artifacts, not approved pixel-diff baselines. Review the PNGs when changing the design. Android Studio also has six offline `@Preview` configurations in `app/src/debug/.../preview/DesignPreviews.kt`.

Device testing remains necessary for GPU rendering, TalkBack traversal, wallpaper color extraction, system bars/keyboard and OEM behavior. Stage 1 does not change the existing foreground-only playback lifecycle. Stage 2 owns the service, notification, queue and automatic next song.
