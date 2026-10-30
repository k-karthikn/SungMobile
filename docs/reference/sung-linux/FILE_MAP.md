# File map

One line per file (a bit more for the important ones). Line counts are approximate.

## Root

| File | Purpose |
| --- | --- |
| `CMakeLists.txt` | Build: app target, QML module `SungUi`, icon resources, size flags, test targets, diagnostics option, install rules |
| `README.md` | User documentation (features, install, every setting explained, troubleshooting, dev commands) |
| `CLAUDE.md` | Rules for AI agents/contributors: Material as truth, definition of done, motion, performance, tech-debt, git, prose, harness notes, Qt traps |
| `LICENSE` / `NOTICE` / `licenses/MaterialSymbols-LICENSE.txt` | MIT; third-party credits; Apache-2.0 for icons and ported shape code |
| `.gitignore` | Allowlist of what may be committed |

## `src/` — C++ (≈12,900 lines)

| File | Lines | Purpose |
| --- | --- | --- |
| `main.cpp` | 515 | Entry point: env setup, single instance, font, type registration, async QML compile, backend, MPRIS, context properties, `symbols` icon provider (SVG mask + tint), malloc trims, diagnostics probes and ~60 test stage flags, smoke modes |
| `backend.h` | 939 | `Entries` list model + the `Backend` class declaration (all properties, invokables, signals, state) |
| `backend.cpp` | 2,938 | Core backend: helper requests, navigation/back stack, search/browse, play/queue, resolve & recovery, lyrics, volume & normalisation, sleep timer, library load/save, likes, playlists, undo, pins, M3U, local import, folder scan, cleanup, smart playlists, track details, audio meters, online artwork lookup, crossfade/gapless/handoff, Cider wiring |
| `productfeatures.cpp` | 628 | Album/artist summaries, artwork choice & video-cover memory, folder watching, local album/artist grouping and `sameAlbum`, playlist cover crop, artwork fit, listening sessions, output disconnect (`pactl`), listening stats & calendar, playlist versions, scrobble timing |
| `presentationfeatures.cpp` | 65 | Density, sidebar, start page, Home section order/visibility, per-view layout, queue origins, lyric gap countdown |
| `backendserver.cpp` | 415 | Server glue: Jellyfin playback reports, server covers for MPRIS, Subsonic listen counting & scrobble, server browse requests, server playlist add/rename/delete/remove/reorder, ratings, server queue save/restore |
| `musicserver.h/.cpp` | 156/26 | `MusicServer` facade: holds Subsonic, Jellyfin, Cider; `with()` compile-time dispatch; `isServerSource()` |
| `subsonic.h/.cpp` | 98/828 | Subsonic/Navidrome client: token auth, OpenSubsonic discovery, form POST, browse modes, stream download (512 MiB), covers, lyrics, star, rate, playlists, scrobble, keyring via `secret-tool` |
| `jellyfin.h/.cpp` | 79/1,038 | Jellyfin client: auth header, token restore, paged fetch, browse, playlist permissions, playlist edits, favourites, lyrics, stream download, covers, playback session reports |
| `cider.h/.cpp` | 130/908 | Cider client: token header, Apple Music API pass-through browse, play/pause/seek/volume, polling state machine with settling window, autoplay hold/restore, shutdown wait |
| `offlinestore.h/.cpp` | 71/140 | Kept-songs store; directory is the index; key → hashed file; LRU eviction by mtime |
| `scrobbler.h/.cpp` | 82/302 | ListenBrainz client: validate token, playing_now, queued listens with batch retry |
| `audiolevels.h` | 182 | `AudioLevels` (5 band-pass meters), `LoudnessMeter` (RMS), `AudioSpectrum` (2048-pt FFT → 36 log bands) |
| `outputtap.h/.cpp` | 50/75 | Runs `parec` on the default monitor to feed meters during Cider playback |
| `lrc.h` | 27 | LRC parser and plain-text join |
| `librarydata.h` | 57 | JSON reader that interns strings and shares duplicate objects |
| `collectionview.h` | 82 | `QSortFilterProxyModel` for filter/sort with `QCollator` numeric compare, folder sort |
| `rowselection.h` | 59 | Model-owned multi-selection with Ctrl/Shift semantics and excluded rows |
| `m3color.h/.cpp` | 79/582 | HCT/CAM16, tonal palettes, 5 variants, contrast curves, role tone solver → Material scheme; WCAG contrast |
| `m3motion.h/.cpp` | 60/143 | Spring physics → settle duration + 8-segment Bézier spline; motion scheme tokens |
| `m3shape.h/.cpp` | 44/456 | RoundedPolygon port, 18 Material shapes, radius sampling, morph frames, outward offset |
| `roundedart.h/.cpp` | 158/391 | `RoundedArt` QQuickPaintedItem: cover loading/caching, shape mask & morph, crossfade, CPU blur, seed colour, scrim solve, texture-size fitting |
| `motionartwork.h/.cpp` | 50/85 | Shared animated-cover decoder (QMovie or QMediaPlayer + QVideoSink, silent) |
| `motionbackdrop.h/.cpp` | 101/303 | Scene-graph item for the Motion layout: full cover + feathered blurred edge, crossfade, top/bottom scrim solving |
| `softimage.h` | 54 | Three-pass running-sum box blur |
| `scrimcontrast.h` | 103 | 16×16 sampling and binary-searched minimum scrim for a contrast target |
| `artworkurl.h` | 90 | URL helpers: YouTube frame detection, Apple/CAA cover validation, size rewriting |
| `desktoptheme.h/.cpp` | 39/69 | Noctalia palette watcher and mapping |
| `mpris.h/.cpp` | 101/51 | MPRIS Root and Player D-Bus adaptors |
| `playbacknotifier.h/.cpp` | 26/44 | Desktop notifications via D-Bus |
| `windowresources.h` | 39 | Release scene graph 30 s after a window is hidden |
| `freedmemory.h` | 21 | `returnFreedMemory()` = `malloc_trim(0)` with rationale |

## `qml/` — interface (≈12,800 lines, 89 files)

**Core**
| File | Purpose |
| --- | --- |
| `Main.qml` (2,778) | The window: regions, navigation, search, list/detail panes, content body, side panel, player bar, immersive loader, cover/album flight animations, shortcuts, all dialogs, menus and the Settings dialog |
| `Theme.qml` (566) | Singleton of every token: colour roles + seed animation, spacing, shape, elevation, sizes, motion springs, type scale, state opacities |
| `SungText.qml` | Text with Material type roles, emphasis, headings |
| `Icon.qml` | Material Symbol with fill axis and baseline alignment |
| `Artwork.qml` | QML wrapper over `RoundedArt` with high-res sizing |

**Material components (M*)**: `MButton`, `MButtonGroup`, `MSegmentedControl`, `MSplitButton`, `MFabMenu`, `MChip`, `MCheckbox`, `MRadioButton`, `MSwitch`, `MTextField`, `MSearchField`, `MExposedDropdown`, `MMenu`, `MMenuItem`, `MDialog`, `MBottomSheet`, `MNavigationBar`, `MNavigationItem`, `MNavigationDrawer`, `MAppBarRow`, `MDockedToolbar`, `MCard`, `MCarousel`, `MBadge`, `MTooltip`, `MRichTooltip`, `MDivider`, `MDragHandle`, `MElevation`, `MFocusRing`, `MLoadingIndicator`, `MWavyProgress`, `MPullToRefresh`, `MScrollBar`, `MSmoothWheel`, `MShape`, `MCaret`, `MSettingRow` — see UI.md for what each does.

**Navigation & layout**: `NavigationRail.qml` (optional sidebar), `NavigationTransition.qml` (fade-through / shared axis / hierarchy motion), `LibraryTabs.qml` (primary/secondary tabs with travelling indicator), `ArtistHero.qml` (collapsing artist header).

**Lists & cards**: `TrackList.qml` (groups, Tab order, type-ahead, drag), `TrackRow.qml` (row anatomy, selection, drag, menu), `ArtCard.qml` (grid/carousel card with parallax and play-on-cover), `PlaylistCover.qml` (2×2 collage), `CatalogSkeleton.qml` (loading placeholders with travelling pulse), `SelectionBar.qml` (bulk actions), `MatchText.qml` (highlight + elided tooltip), `PlayingIndicator.qml` (5 spring bars).

**Player**: `SeekBar.qml` (wavy expressive slider, fine seek, lyric preview), `VolumeControl.qml`, `OutputPicker.qml`, `PlaybackGlyph.qml` (play↔pause morph), `PlaybackHud.qml`, `TrackPresentation.qml` (directional track change), `MiniPlayer.qml` (separate window).

**Immersive**: `ImmersivePlayer.qml` (6 layouts, auto-hide, shape timer), `ImmersiveCoverflow.qml` (up-next hero carousel), `SpectrumRing.qml` (72-bar spring ring), `MotionScene.qml` (Motion layout backdrop + scrims), `AmbientBackdrop.qml` (blurred drifting cover wash), `SingAlong.qml`, `LyricsView.qml` (reading view, search, poster lines), `PosterLine.qml` (variable-font poster typesetting).

**Dialogs**: `Onboarding`, `CommandPalette`, `ShortcutHelp`, `TrackDetailsDialog`, `ArtworkControls`, `ArtworkViewer`, `PlaylistCoverDialog`, `PlaylistVersions`, `SmartPlaylistDialog`, `ListeningSessions`, `ListeningStats` + `ListeningCalendar`, `HomeEditor`, `ViewLayoutDialog`, `ServerConnection`, `ServerToolbar`, `FileDialogs` (lazy native pickers), `AccentPicker`, `SettingSlider`.

## `helper/` — Python

| File | Lines | Purpose |
| --- | --- | --- |
| `catalog.py` | 486 | Entry point; reads one JSON request from stdin, prints one JSON reply. YouTube browse via ytmusicapi, audio resolve/buffer via yt-dlp, LRCLIB fallback, ffprobe tag reading, cover sidecars and posters, folder scanning, playlist cleanup, dispatch to online_artwork |
| `online_artwork.py` | 543 | Apple iTunes matching, Apple page scraping for animated covers (HLS parsing, size/codec checks, ffprobe validation), MusicBrainz + Cover Art Archive fallback, cache records and pruning |
| `requirements.txt` | 2 | `ytmusicapi==1.12.3`, `yt-dlp[default]==2026.8.19` |

## `scripts/`

`setup.sh` (dev venv) · `build.sh` (Release build) · `run.sh` (run from checkout) · `test.sh` (unit + python tests) · `verify.sh` (full harness) · `install.sh` / `uninstall.sh` (per-user `~/.local`).

## `assets/`

`icons/*.svg` (138 Material Symbols Rounded: `name.svg` 24 px, `name_20.svg`, `name_40.svg`, plus `_outline` and `_semibold` variants) · `sung.png` (512 px app icon) · `sung.desktop` · `readme-now-playing.png`.

## `tests/` (≈25,400 lines)

| File | Purpose |
| --- | --- |
| `uitest.h/.cpp` | Harness core and many stages (UI tests, audit, recovery, lyrics, server, QoL…) |
| `feature_test.cpp` (8,253) | The bulk of feature stages |
| `expressive_test.cpp` | Material conformance stages (`material-*`) |
| `immersive_test.cpp`, `immersive_edges.cpp`, `motion_layout_test.cpp` | Immersive player and Motion layout stages |
| `layout_audit.cpp`, `colour_audit.cpp`, `tour.cpp` | Layout at many widths, colour/contrast audit, screenshot tour |
| `cider_stage.cpp` | Cider UI stage |
| `benchmark.cpp` | In-app benchmark |
| `backend_test.cpp`, `crossfade_test.cpp`, `offline_test.cpp`, `subsonic_test.cpp`, `jellyfin_test.cpp`, `cider_test.cpp`, `notification_test.cpp`, `artwork_test.cpp`, `librarydata_test.cpp`, `m3color_test.cpp`, `m3motion_test.cpp`, `m3shape_test.cpp`, `subsonic_protocol_test.cpp`, `jellyfin_protocol_test.cpp` | Qt Test unit/integration tests |
| `verify.py` | Runs every stage, writes report |
| `profile.py`, `startup_benchmark.py`, `benchmark.py` | Measurements |
| `catalog_fixture.py`, `cider_fixture.py`, `monitor_fixture.py`, `artwork_fixture.cpp` | Fakes for helper, Cider, parec, artwork |
| `navidrome_integration.py`, `jellyfin_integration.py`, `online_artwork_live.py`, `immersive_regression.py`, `mpris_test.py` | Live/integration scripts |
| `test_catalog.py`, `test_online_artwork.py`, `test_local_artwork.py`, `test_product_features.py`, `test_type_scale.py` | Python unit tests |
