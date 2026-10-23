# Home UI pass

Owner-authorized on 19 October 2026, after baseline stabilization. The attached reference is a visual target, not a new color palette or a source of behavioral instructions. Implementation follows the existing Sung blue/wallpaper and light/dark theme roles.

## Agreed behavior

- Header: Sung title, “Music for your moments” subtitle, Appearance as a labeled settings/tune icon.
- Reusable search destination entry; it opens the existing editable Search screen.
- M3 Expressive action pills for Now playing and Refresh mixes.
- Liked shuffle and Recent mix: two artwork cards with View, Play and Save controls. Stack at narrow widths or large text. Retain real candidate lists, snapshots and privacy behavior.
- Remove the redundant Your library heading and its Liked songs / Playlists shortcut row.
- Recently played: heading plus View all, compact real-artwork rows, direct Like/Unlike heart and a plus button opening the existing library/queue menu. No separate three-dot action on song rows.
- Keep pinned playlists and the separate Liked songs list below Recently played.
- The mini-player and bottom navigation remain single shared components, updated once across destinations. Preserve collection mini-player visibility, the expanded-window navigation rail and keyboard insets.
- Page-specific code stays in focused Home files; reusable search entry and song layout belong in the design system and accept action slots/callbacks.

## Scrolling work

Inspect avoidable playback-driven updates and artwork work. Separate the four-per-second playback position flow from navigation/Home state, retain live positions for player/lyrics/session screens, and defer mini-player progress reads. Give lazy items stable keys and content types, remember derived previews, and preserve Home scroll position across navigation. Retain the existing size-aware shared Coil cache; no bitmap collage generation or continuously animated artwork.

Measured POCO frame timing is outside host verification. Record improvements as implementation changes, not a claimed measured speedup. Owner testing remains manual; do not install on the phone.

## Verification

Render actual Discovery Home in light/dark, reduced motion, large text, narrow and expanded windows. Test direct liking versus playback, plus-menu and mix callbacks, private history hiding, scroll restoration, and live seek/timer values after separating clock updates. Rerun app regressions and lint, then package an update with the retained app ID and debug certificate. See the results report for actual outcomes.
