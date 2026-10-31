# Design system

`core:designsystem` owns shared visual foundations and controls. Feature-specific screens stay in `app/src/main/java/com/karthi/sungmobile/feature/`; keep navigation, catalog behavior, persistence, and playback out of this module.

## Where to work

| Location | Responsibility |
| --- | --- |
| `core/designsystem/.../theme` | Color schemes, typography, spacing, shapes, and motion settings. |
| `core/designsystem/.../icon` | Typed `SungSymbol` icons and the `SungIcon` composable. |
| `core/designsystem/.../artwork` | Artwork loading, reusable masks, and layered artwork effects. |
| `core/designsystem/.../component` | Shared Compose controls and settings components. |
| `app/.../feature/<feature>` | Feature screens, controls, and presentation state. |

## Theme and motion

Wrap the app in `SungTheme`. Use `MaterialTheme.colorScheme`, `typography`, `shapes`, and `motionScheme`, together with `SungSpacing`; avoid screen-specific copies of shared colors, type, or animation values. The theme supports light and dark modes, wallpaper-derived palettes, and reduced motion. Keep controls usable with Android's system animation scale and larger font sizes.

```kotlin
SungTheme {
    SungButton(onClick = onPlay) {
        SungIcon(SungSymbol.Play)
        Text(stringResource(R.string.play))
    }
}
```

## Icons and artwork

Use `SungSymbol` for shared vector icons. Add or change an icon in the design-system module so feature screens keep a consistent icon API.

Use `SungArtwork` for catalog artwork. It uses the shared Coil loader and supports reusable masks; missing, loading, and failed images should retain a stable placeholder. Treat artwork as decorative when adjacent text already identifies the item. Provide a content description when an image communicates information unavailable elsewhere.

## Components and accessibility

Add a shared component only when it is feature-neutral and has a clear reuse case. Keep feature labels, actions, and state in the feature package. Preserve at least 48 dp touch targets, readable contrast, text wrapping, scrolling, and system/keyboard insets. Do not use color alone to communicate selection or playback state.

## Review changes

Run `./gradlew :app:lintDebug` and the relevant unit or UI tests when requested for a visual change. The Android Studio previews are in `app/src/debug/.../preview/DesignPreviews.kt`; the Compose rendering test is `DesignFoundationTest`. Review light and dark themes, large text, reduced motion, and narrow and expanded layouts. Device checks are still needed for GPU rendering, TalkBack traversal, wallpaper colors, system bars, keyboard behavior, and OEM differences.
