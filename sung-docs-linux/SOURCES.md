# Sources

Everything Sung depends on or cites: network services it talks to, specifications it implements, and reference material named in the code and `NOTICE`.

## 1. The repository

- Code: https://github.com/yappologistic/Sung (MIT licence, author handle `yappologistic`, version 0.12.0)
- Support link in README: https://buymeacoffee.com/e_gurl
- Rules for contributors/agents: `CLAUDE.md` in the repo.

## 2. Network services the app talks to at runtime

| Service | Endpoint(s) | Purpose | Auth |
| --- | --- | --- | --- |
| YouTube Music (via ytmusicapi) | music.youtube.com internal API | Home, search, album, playlist, artist, radio, lyrics, song details | Anonymous |
| YouTube (via yt-dlp) | `https://music.youtube.com/watch?v=<id>` | Audio stream URL / download | Anonymous; optional Netscape cookies file |
| YouTube image hosts | `i.ytimg.com`, `lh3.googleusercontent.com`, `yt3.ggpht.com` … | Thumbnails (resized via URL rewriting) | — |
| Subsonic / Navidrome | `<server>/rest/<method>.view?u&t&s&v=1.16.1&c=Sung&f=json` | Library, stream, cover art, lyrics, star, rate, playlists, play queue, scrobble | Token = MD5(password + salt) |
| OpenSubsonic extensions | `getOpenSubsonicExtensions` | Detect `songLyrics`, `formPost` | same |
| Jellyfin | `/Users/AuthenticateByName`, `/Users/Me`, `/Items`, `/Artists`, `/MusicGenres`, `/Playlists/...`, `/Audio/<id>/stream`, `/Audio/<id>/Lyrics`, `/Sessions/Playing{,/Progress,/Stopped}`, `/Users/<u>/FavoriteItems/<id>` | Library, stream, lyrics, playback reporting, favourites, playlists | `Authorization: MediaBrowser Client="Sung", Device="Sung Linux", DeviceId, Version, Token` |
| Cider (local) | `http://localhost:10767/api/v1/playback/{active,play-item,play-item-href,play,pause,seek,volume,now-playing,is-playing,autoplay,toggle-autoplay}`, `/api/v1/amapi/run-v3` | Drive Apple Music playback; browse via Apple Music API pass-through | `apptoken` header (bare token) |
| Apple Music API (through Cider) | `/v1/me/storefront`, `/v1/me/library/{albums,artists,playlists,songs}`, `/v1/catalog/<sf>/{albums,playlists,artists,search,charts}` | Browse Apple Music | Cider's session |
| iTunes Search API | `https://itunes.apple.com/search`, `/lookup` | Match a YouTube song to an Apple album (artwork) | None |
| Apple Music public web pages | `https://music.apple.com/us/album/<id>` (`serialized-server-data` JSON) | Find the animated cover's HLS master playlist | None |
| Apple media CDN | `mvod.itunes.apple.com` (HLS), `isN-ssl.mzstatic.com` (images) | Download animated cover MP4, still covers | None |
| MusicBrainz | `https://musicbrainz.org/ws/2/recording?query=…&fmt=json` | Fallback album identification | None; UA `Sung/0.12.0 ( https://github.com/yappologistic/Sung )` |
| Cover Art Archive | `https://coverartarchive.org/release-group/<mbid>/front[-250/-500/-1200]` | Fallback album covers (≥ 500 px only) | None |
| LRCLIB | `https://lrclib.net/api/get?track_name&artist_name&duration[&album_name]` | Synced lyrics fallback (exact match only) | None; respects 429 Retry-After |
| ListenBrainz (or compatible) | `https://api.listenbrainz.org/1/validate-token`, `/1/submit-listens` | Scrobbling (`playing_now`, `single`, `import`) | `Authorization: Token <user token>` |

## 3. Local desktop services

| Service | Interface |
| --- | --- |
| MPRIS | D-Bus `org.mpris.MediaPlayer2.sung`, object `/org/mpris/MediaPlayer2`, interfaces `org.mpris.MediaPlayer2` and `.Player` |
| Notifications | D-Bus `org.freedesktop.Notifications` (`Notify`, `CloseNotification`, `NotificationClosed`) |
| Secret Service | via `secret-tool` (attributes `application=sung account=<key>`) |
| PulseAudio / PipeWire | `pactl`, `parec --device=@DEFAULT_MONITOR@` |
| Noctalia shell palette | `~/.local/share/color-schemes/noctalia.colors` (override `SUNG_NOCTALIA_COLORS`) |

## 4. Specifications and reference code the implementation follows

**Material Design 3 (Expressive)**
- Guidelines site: https://m3.material.io (components, color, typography, shape, motion, layout/window size classes, accessibility).
- Token source of truth used for numbers: Jetpack Compose Material 3 generated tokens — `androidx/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/*.kt` (e.g. `ColorLightTokens`, `ListTokens`, `ButtonSmallTokens`, `SliderTokens`, `SwitchTokens`, `ExpressiveMotionTokens`, `StandardMotionTokens`…).
- Compose component sources cited by file and line: `Button.kt`, `IconButton.kt`, `ButtonGroup.kt`, `SplitButton.kt`, `Menu.kt`, `MenuDefaults.kt`, `NavigationItem.kt`, `NavigationDrawer.kt`, `WideNavigationRail.kt`, `TabRow.kt`, `Slider.kt`, `Switch.kt`, `Checkbox.kt`, `RadioButton.kt`, `Chip.kt`, `TextFieldImpl.kt`, `OutlinedTextField.kt`, `AlertDialog.kt`, `ModalBottomSheet.kt`, `Tooltip.kt`, `BasicTooltip.kt`, `Carousel.kt`, `FloatingActionButtonMenu.kt`, `LoadingIndicator.kt`, `ProgressIndicator.kt`, `WavyProgressIndicator.kt`, `PullToRefresh`, `Scrollbar.kt`, `SearchBar.kt`, `PaneMotion.kt`, `MotionScheme.kt`, `MaterialTheme.kt`, `MaterialShapes.kt`.
- material-web tokens (focus ring: `md.sys.state.focus-indicator`, typescale `wdth`).
- **androidx graphics-shapes** `RoundedPolygon.kt`, `Utils.kt` — corner rounding ported to C++ (Apache-2.0).
- **Material Color Utilities (MCU)** TypeScript: `color_spec_2021.ts`, `dynamic_scheme.ts`, `dynamic_color.ts`, `contrast_curve.ts`, `contrast.ts`, `temperature_cache.ts`, `dislike_analyzer.ts` — HCT, palettes, role tones, contrast curves.
- CAM16 colour appearance model (inside HCT).
- **Oklab/Oklch** matrices: https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
- WCAG 2.x contrast ratio (1.4.3, 4.5:1).
- Google Sans Flex introduction (design.google) for poster-style typesetting.

**Qt documentation cited in comments**
- Exception safety: https://doc.qt.io/qt-6/exceptionsafety.html
- Texture atlas: https://doc.qt.io/qt-6/qtquick-visualcanvas-scenegraph-renderer.html#texture-atlas
- Async component compilation: https://doc.qt.io/qt-6/qqmlcomponent.html#CompilationMode-enum
- Implicit sharing & threads: https://doc.qt.io/qt-6/implicit-sharing.html, https://doc.qt.io/qt-6/threads-modules.html#threads-and-implicitly-shared-classes
- Customising controls (derive from templates): https://doc.qt.io/qt-6/qtquickcontrols-customize.html
- Image caching: https://doc.qt.io/qt-6/qml-qtquick-image.html#cache-prop
- Lazy initialisation: https://doc.qt.io/qt-6/qtquick-performance.html#lazy-initialization
- `QQuickPaintedItem::textureSize`

**Linux / other**
- glibc `malloc_trim`: https://man7.org/linux/man-pages/man3/malloc_trim.3.html
- MPRIS 2.2 spec; freedesktop Notifications spec.
- Subsonic API 1.16.1; OpenSubsonic; Jellyfin OpenAPI; Cider RPC docs: https://cider.gitbook.io/welcome-to-gitbook/docs/1.client/rpc
- ReplayGain (−18 dBFS reference) and EBU R128 (−23 LUFS, Q7.8 gain tags).
- ListenBrainz API thresholds (half the track or 4 minutes; ≥ 30 s).
- GitHub contribution graph (model for the listening calendar).

## 5. Credits and references in `NOTICE`

- **Material Symbols** (Apache-2.0), from https://github.com/google/material-design-icons — Rounded set, 20/24/40 px optical sizes, FILL=1 with FILL=0 for state symbols.
- `src/m3shape.cpp` ports androidx graphics-shapes `RoundedPolygon` rounding and Compose `MaterialShapes` definitions (Apache-2.0).
- Visual references: Google Material Design 3 and **Spun**.
- Architecture reference: **Unstream** — https://github.com/amiralibg/unstream (no code incorporated).
- Qt (LGPL, dynamically linked), ytmusicapi (MIT), yt-dlp (Unlicense).
- Independent of Google and YouTube.
