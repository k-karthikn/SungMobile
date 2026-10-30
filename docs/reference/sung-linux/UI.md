# UI

Everything visible is QML in `qml/`. One big file, `Main.qml` (~2,800 lines), holds the window, its regions and most dialogs; 88 smaller files are components. Components named `M…` implement a specific Material 3 component and cite the Compose source they follow.

## Window

- Default size **1180 × 800**, minimum **480 × 580**. Size is remembered (`Window` settings group) unless maximised/immersive.
- Title: `"<song title> · Sung"` or `"Sung"`.
- Background: `Theme.background`, optionally with the **ambient backdrop** (blurred current cover behind a solved scrim).

### Size classes (Material's breakpoints)

| Class | Width | Pane margin | Gutter | Home card width |
| --- | --- | --- | --- | --- |
| compact | < 600 | 16 | 16 | 190 |
| medium | 600–839 | 24 | 16 | 190 |
| expanded | 840–1199 | 24 | 24 | 190 |
| large | 1200–1599 | 24 | 24 | 210 |
| extraLarge | ≥ 1600 | 24 | 24 | 230 |

Every layout decision reads `atLeastMedium` / `atLeastExpanded` / `atLeastLarge` instead of a magic number.

## Main regions

```
┌──────────────────────────────────────────────────────────────────────────┐
│ Top bar:  [≡ drawer] [← back]   (  Home | Search | Library  )  [mini][⚙] │  ← navigation bar as a centred capsule
├───────────────┬───────────────────────────────────────┬──────────────────┤
│ List pane     │  Detail / content column              │ Side panel       │
│ (Albums/      │  ┌ app bar: title, artist, summary ┐  │  Now playing     │
│  Artists grid │  │ actions: Play ▾ | filter | ...  │  │  Queue (Next /   │
│  when a       │  ├ chips / library tabs ───────────┤  │   Recently       │
│  detail is    │  │ content: shelves / track list / │  │   played)        │
│  open)        │  │ grid / empty state / skeleton   │  │  Lyrics          │
│               │  └ FAB (library add)               │  │ (drag to resize) │
├───────────────┴───────────────────────────────────────┴──────────────────┤
│ Player bar: [cover][title/artist]  [⤨][⏮][▶][⏭][↻]  [seek ────]  [🔊▾][≡][⋯] │
└──────────────────────────────────────────────────────────────────────────┘
```

- **Top bar**: Material navigation bar items laid out horizontally (64 dp, full corner, surfaceContainer) inside the top bar, because the window's bottom edge is taken by the always-visible player. Below 600 px the capsule spans the window instead of centring, so the three destinations keep their positions.
- **Optional sidebar**: Settings → Appearance → Navigation: "Sidebar" swaps the top capsule for a **navigation rail** (96 dp collapsed, 220–360 dp expanded, shows up to 6 pins when expanded, hosts the library FAB at its head).
- **Navigation drawer** (narrow windows): modal sheet from the leading edge with destinations, pins, Mini player and Settings.
- **Search bar**: in the top bar; suggestions popover shows recent searches and local matches (playlists, queue, likes, playlist songs, files, history — up to 8). Pasting a YouTube link opens it.
- **Content column**: page header (title in display-small on Home, headline-medium elsewhere; collapses on scroll), album artist/summary, collection actions (split "Play" button with Shuffle / Add to queue / Play next, filter-and-sort, pin, more), search filter chips (Songs, Albums, Artists, Playlists, Videos), library tabs, local facet tabs (Albums / Artists / Folders), import progress (wavy progress), content body.
- **Content body** can be: Home shelves (carousels), a track list, a local group grid, a playlists list/grid, a server page, skeleton placeholders while loading, or an empty state with an action. Pull-to-refresh uses the expressive loading indicator.
- **Artist hero**: an artist page shows a big portrait band that collapses into a title row as you scroll.
- **Side panel** (Now playing / Queue / Lyrics): resizable via a Material drag handle (double-click resets; width remembered). In narrow windows it becomes a **bottom sheet**.
- **Player bar**: cover (click → Now playing), marquee-safe title/artist, transport (shuffle shown ≥ 980 px), play button that morphs play↔pause and pulses with the bass, wavy seek bar with lyric preview, volume control with exact-percentage popup and output picker arrow, queue button with badge, overflow menu (`MAppBarRow` moves actions that don't fit into the overflow automatically). Two-row layout when narrow.

## Pages

| Page | What is shown |
| --- | --- |
| Home ("Listen") | YouTube Music home shelves, with a "Pinned" shelf first. Order/hide via **Customize Home**. Borrowed cover backdrop. |
| Search | Results for the query + filter, paged by 30 up to 200. |
| Album | Cover carried in from the card (container transform), artist, year · N tracks · duration, disc headings. |
| Artist | Hero + shelves (Songs, Albums, Singles, Videos, Related artists). |
| Playlist (YouTube) | Paged by 100 up to 5,000. |
| Library tabs | Liked songs, Recently played, Playlists, Mixes, Local files (with Albums / Artists / Folders facets), Music server. |
| Local playlist | Editable list (drag reorder only when unsorted and unfiltered), cover collage or custom cover. |
| Smart playlist | Rule-driven; updates live. |
| Music server | Chips for the provider's modes: Albums, Artists, Genres, Playlists, Favorites, Discover (Cider: Albums, Artists, Playlists, Discover = song chart). Toolbar: library folder, new playlist, save/restore queue (Subsonic). Empty state with "Connect server". |

## Track row (`TrackRow.qml`)

- 72 dp (comfortable) or 56 dp (compact) high; 56 / 40 dp cover.
- Shows title (MatchText highlights search matches; elided text gets a tooltip), artist/album, duration, liked heart, more button; playing indicator bars on the current song.
- **Expressive list shape**: rests nearly square (4 dp), rounds to 12 dp on hover, 16 dp on press/focus. Queue and panel lists use the **segmented** style (2 dp gaps, outer corners full step).
- Selection checkboxes appear on hover/focus or when anything is selected; a docked **selection bar** offers bulk actions.
- Dragging carries a stacked-covers preview ("3 tracks"); queue rows can be swiped sideways to remove; the dragged row takes the primary container colours.
- Context menu (`MMenu`): Play now keep queue, Play next, Add to queue, Start radio, Like, Add to playlist / server playlist, Go to album/artist, Copy link, Adjust volume…, Track details, Rate (Subsonic), Locate file, Remove.

## Lists (`TrackList.qml`)

- Groups: by folder (natural filename order) or by disc, with collapsible headings showing counts and a group play button.
- Tab walks the list in visual order (fixes Qt's default of following delegate creation order).
- **Type-ahead**: typing jumps to the first title match, falling back to artist; buffer clears after a pause.
- Ctrl+A selects all eligible rows.

## Immersive player (F11) — `ImmersivePlayer.qml`

A full-window player with a "…" menu to choose the layout (remembered):

| Layout | Description |
| --- | --- |
| **Artwork** | Large cover (largest square that fits), details centred under it. |
| **Lyrics** | Lyrics view only. |
| **Split** (default) | Cover column (~34 % of width) + lyrics; the playing line is level with the cover's middle. |
| **Sing along** | Only when lyrics are timed. One line size, emphasis by scale (0.62 rest), primary fill sweeping at the line's pace. |
| **Visualizer** | Cover cut into one of 12 round Material shapes, morphing to another every 8–16 s (slow spatial spring), ringed by 72 bars from the FFT spectrum, bass at the top. |
| **Motion** | The cover video fills the window (square or 3:4 tall, 1080/2K/4K tiers), sharp centre, blurred feathered edges; controls and details auto-hide; the sung line stays with its own scrim. |

Extras: **Up next covers** carousel (hero carousel of the queue; settling on a cover plays it), queue drawer (Ctrl+L), auto-hide controls, click artist/album to browse, artwork viewer (zoom with wheel/+/−, 0 reset), playback HUD for keyboard seeks/volume.

## Mini player (`MiniPlayer.qml`)

Separate window titled "Sung · Mini player": cover, title, current lyric line (cross-fading), transport, seek bar, volume, restore button. Draggable anywhere. "Pin above other windows" only offered off Wayland (README gives Hyprland window rules instead).

## Dialogs and popups

Built on `MDialog` (template-based; content built lazily the first time it opens; full screen below 600 px; 280–560 dp width for basic dialogs):

Settings · Onboarding (3 steps: theme+accent, music folder, start page) · Command palette · Shortcut help · Track details · Artwork controls (preview, source album, retry, disable, choose file, fit/fill) · Artwork viewer · Playlist name/rename/delete · Add to playlist · Skip duplicates? · Playlist cover crop · Playlist version history · Smart playlist rules · Playlist cleanup · Music folders · Add folder path · Server connection (provider segmented control: Subsonic / Jellyfin / Cider) · Server playlist add/rename/delete/rate · Listening sessions · Listening statistics (Week/Month/Year/All time, heat-map calendar, top artists/albums/songs) · Home editor · View layout · Output picker · Adjust volume (trim) · Lyric timing · Sleep timer menu · Volume step menu.

## Settings (searchable; categories)

- **Appearance**: Theme (System/Light/Dark), Accent color (Default, Purple `#6750a4`, Green `#386a20`, Blue `#00658e`, Terracotta `#8f4c38`, Mauve `#7d5260`, Olive `#6b5f00`), Use artwork accent, Color scheme (Neutral, Balanced=Tonal spot, Vibrant, Expressive, Faithful=Content), Contrast (Standard/Medium/High), Motion (Expressive/Standard), Animations, Animated album artwork, Online animated covers, Album covers for music videos, Motion layout video (Auto/1080p/2K/4K), Current artwork, Ambient artwork backdrop, Backdrop follows the music, Poster-style lyrics, Density (Comfortable/Compact), Current view layout, Navigation (Top bar/Sidebar), Pointer (Comfortable/Precise — Precise drops the 48 dp touch-target floor and shrinks small buttons to 36 dp).
- **Playback**: Crossfade, Gapless playback, Prepare next track, Volume normalization, Resume recordings over 20 minutes, Playback speed, Preserve pitch, Sleep timer, Fade out before sleep, Autoplay similar songs, Audio output, Pause when audio output disconnects, Volume step (1/2/5/10 %), Lyrics text size, Show completed lines, Find missing lyrics on LRCLIB, Import LRC.
- **Library**: Start page (Home/Local/Server/Liked), Music folders, Update music folders automatically, Type to jump in lists, Customize Home, Listening sessions, Listening statistics, Export/Import library, Keyboard shortcuts.
- **Connections**: Music server, YouTube streaming quality (Standard / Data saver), cookies file, ListenBrainz.
- **Privacy & data**: Keep played songs offline (Off/512 MB/1 GB/4 GB/16 GB), Clear kept songs, Pause history this session, Track notifications, Clear history, Clear artwork cache, Remove cookies.

An interface audit test allows only **one explanatory paragraph across all of Settings** — labels must explain themselves.

## Keyboard shortcuts

| Keys | Action |
| --- | --- |
| Space | Play / pause |
| Ctrl+F, Ctrl+K | Focus search |
| Ctrl+Shift+P | Command palette (quick actions, playlists, outputs) |
| Ctrl+J | Show the playing song in the queue |
| ? / F1 | Shortcut help |
| Ctrl+M | Mini player |
| F11 | Immersive player |
| Ctrl+L | Queue |
| Ctrl+Y | Lyrics panel |
| Ctrl+Right / Ctrl+Left | Next / previous |
| Right / Left | Seek ±10 s |
| Shift+Right / Shift+Left | Seek ±100 ms (fine) |
| 0–9 | Jump to that tenth of the track |
| Ctrl+Up / Ctrl+Down | Volume by the volume step |
| M | Mute |
| Alt+Left | Back |
| Ctrl+A | Select all in focused list |
| Escape | Cancel drag → clear selection → leave immersive → close panel |
| Ctrl+Q | Quit |
| Shift + drag seek bar | Fine seek (applies on release); mouse wheel on seek bar = ±5 s |

Shortcuts are disabled while a modal is open or a text field has focus, as appropriate.

## Motion between screens (`NavigationTransition.qml`)

- **Destinations** (Home/Search/Library) **fade through**: old view fades out (fast effects), new one fades in growing from 92 % (default spatial).
- **Tabs** share the **X axis**: 30 dp travel in the direction of movement.
- **Opening a detail** slides horizontally with a fade; Back reverses.
- **Container transform**: opening an album/artist/playlist from a card flies the cover into the header (and back on return when visible). Player cover flies to the Now playing panel.
- Track info in the player slides in the direction of Next/Previous.
- All of it is skipped when Animations is off.

## Accessibility

- Every control takes focus; Material focus ring (3 dp, 2 dp offset, secondary colour) shown only for keyboard focus (`visualFocus`).
- Section titles set `heading: true` so screen readers can navigate by headings.
- Text colours solved to ≥ 4.5:1 (7:1 / 11:1 at higher contrast) in both themes, also over artwork via computed scrims.
- Grids of tiny cells (listening calendar) are a single control navigable with arrows.
- Modal sheets trap Tab inside; pooled (recycled) delegates hide themselves so Tab can't land on them.
- `Accessible.announce` used for command-palette choices.

## Component library (qml/M*.qml)

MButton (filled/tonal/elevated/outlined/text; xsmall–large; icon widths narrow/uniform/wide; toggle; shape morph on press) · MButtonGroup (standard group, 15 % press expansion) · MSegmentedControl (connected group) · MSplitButton · MFabMenu (FAB → menu morph, staggered items) · MChip (filter/assist/suggestion/input) · MCheckbox · MRadioButton · MSwitch · MTextField (outlined/filled, supporting/error text) · MSearchField · MExposedDropdown · MMenu / MMenuItem (expressive menu groups) · MDialog · MBottomSheet · MNavigationBar · MNavigationItem · MNavigationDrawer · NavigationRail · MAppBarRow (auto overflow) · MDockedToolbar · MCard · MCarousel (multi-browse with masked edge items and parallax) · MBadge · MTooltip · MRichTooltip · MDivider · MDragHandle · MElevation (shadow rings, no shaders) · MFocusRing · MLoadingIndicator (7-shape morph) · MWavyProgress · MPullToRefresh · MScrollBar · MSmoothWheel (browser-style wheel/touchpad scrolling) · MShape (any Material shape, morph + pulse) · MCaret (2 dp primary caret).

App-specific: Artwork, ArtCard, PlaylistCover (2×2 collage), TrackRow, TrackList, SeekBar, SettingSlider, VolumeControl, LyricsView, PosterLine, SingAlong, SpectrumRing, ImmersivePlayer, ImmersiveCoverflow, MotionScene, AmbientBackdrop, ArtistHero, CatalogSkeleton, PlayingIndicator, PlaybackGlyph, PlaybackHud, TrackPresentation, SungText, MatchText, Icon, LibraryTabs, SelectionBar, ServerToolbar, ServerConnection, AccentPicker, ListeningCalendar, etc.
