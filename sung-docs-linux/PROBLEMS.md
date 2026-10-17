# Problems, limits and risks

An honest list of what is weak, fragile or limited in Sung — useful so a new project can avoid the same traps. Grouped from "user-visible" to "inside the code".

## 1. Product limits (stated or implied by the code)

| Limit | Why it exists |
| --- | --- |
| **Linux only** | CMake refuses other systems; uses `setsid`, `kill`, `/proc`, `malloc_trim`, D-Bus, `pactl`, `secret-tool`. |
| Needs Qt **6.8+**, Python 3, **Node.js 20+**, FFmpeg | yt-dlp needs a JS runtime for YouTube's player challenges. Heavy dependency chain for a "light" app. |
| Font **not bundled** | Looks are tuned for Google Sans Flex; on Noto Sans, poster lyrics and emphasis lose most of their effect. |
| **Playback starts after a download** | Every YouTube/server song is buffered whole before playing. "Starting a song can take a moment." Songs > 32 MiB fall back to direct streaming. |
| **YouTube breaks when Google changes things** | Depends on unofficial `ytmusicapi` and `yt-dlp`; README tells users to `pip --upgrade` them. Region locks and "sign in to confirm you're not a bot" need a cookies file. |
| YouTube likes/playlists **don't sync** with the Google account | Browsing is anonymous by design. |
| No lossless from YouTube | Opus ~130 kbps max. |
| No SponsorBlock / segment skipping | Not implemented. |
| **One music-server account at a time** | `MusicServer` holds one provider. Server playlists can only hold that server's songs. |
| Apple Music needs **Cider + a subscription** and Cider must stay open | Apple only streams to its own players. Speed, crossfade, normalisation don't apply; the desktop may show Sung as "recording" (parec); other sounds move the visualizer; picking a song inside Cider makes Sung stop following. |
| **Mini player can't stay on top on Wayland** | No Wayland protocol for it; README gives Hyprland window rules. |
| Folder monitoring bounded (4,096 dirs / 10,000 files) | inotify limits; network mounts without notifications need manual rescan. |
| Animated covers depend on **scraping Apple's web page** (`serialized-server-data`) | Unofficial and fragile; any page redesign breaks it. Lookups are hard-coded to the **US** storefront (`country=us`, `/us/album/`). |
| No translations | All strings are English literals; no `qsTr` anywhere. |
| No packaging | Install is a per-user build script; no AUR/Flatpak/AppImage in repo. |
| No CI in the repo | The `.gitignore` allowlist doesn't even permit a `.github/` folder; the full verify run is manual and takes 20–30 min. |

## 2. Legal / policy risks

- Downloading YouTube audio with yt-dlp is against YouTube's terms of service; the project states it is independent of Google/YouTube but the risk remains for any derivative.
- Scraping Apple Music web pages for animated covers and using iTunes Search for matching is unofficial.
- MusicBrainz/Cover Art Archive and LRCLIB are community services with rate limits; Sung identifies itself and backs off on 429, but a popular fork must be careful.

## 3. Security and privacy notes

- **Subsonic token auth = MD5(password + salt)** (a protocol limitation). The plain password is kept in memory while connected and in the keyring.
- HTTP server addresses are accepted (README asks for HTTPS on remote servers). Cider runs over plain HTTP on localhost with a bare token header.
- If the keyring isn't available, login works only for the session (good), but the cookies file is stored on disk in plaintext (mode 0600).
- Good practices already in place: credentials never in ids or exports, URL allow-lists and redirect re-validation in the artwork helper, `ffprobe/ffmpeg -protocol_whitelist file,crypto,data`, size caps on every download and response, video-id regex validation, `QSaveFile` atomic writes with 0600, no redirects followed for server APIs, error text truncated. Logs may contain signed media URLs (README warns not to post them).

## 4. Architecture / code-quality problems

1. **God object.** `Backend` has ~130 properties and ~140 invokable methods across ~4,000 lines in four files. Navigation, playback, library, lyrics, artwork, stats and settings all live in one class. Hard to test in isolation, hard to reason about which signal triggers what. A new project should split it (PlaybackService, QueueModel, LibraryStore, NavigationController, LyricsService, ArtworkService, SettingsStore).
2. **Untyped data.** Tracks, playlists, requests and results are `QVariantMap`s with string keys (`"localPath"`, `"remoteId"`, `"_queueOrigin"`…). Typos fail silently; validation is scattered (`playable()`, `owns()`, `RowSelection::eligible()` each re-implement "is this playable"). A typed `Track` struct (or `Q_GADGET`) with one `isPlayable()` would remove a class of bugs.
3. **One huge QML file.** `Main.qml` is ~2,800 lines with the whole Settings dialog, ~25 dialogs, menus and shortcuts inline. Splitting by region (TopBar, PlayerBar, SidePanel, SettingsDialog…) would help.
4. **Very dense code style.** Many functions are written as single long lines with several statements (`if(...){...;...;}else{...}` on one line). Comments are excellent, but the code itself is hard to scan and diff.
5. **Inheritance used only for code sharing.** `Jellyfin` and `Cider` inherit `Subsonic` but *hide* (not override) all its members; each also declares its own `m_network`, `m_settings`, `m_channels` etc., so the base copies sit unused. `MusicServer::with()` compares the provider string on every call. A proper interface (`IMusicSource` with virtual methods) or composition would be clearer.
6. **Whole-library JSON rewrite on every change.** Any edit rewrites `library.json` completely (600 ms debounce, background thread). History and the play log store full track maps, so the file grows with use. Serialising a large library took ~⅓ s (which is why it was moved off the GUI thread). SQLite would make writes incremental and queries cheaper.
7. **Linear scans in hot-ish paths.** `recordHistory()` rebuilds a set of every saved id on each play to prune `lastPlayed`; smart playlists and mixes re-filter every candidate on each `libraryChanged`; `localMatches`, `isLiked` (for local likes), `isPinned` are linear. Fine for thousands of songs; will hurt at hundreds of thousands.
8. **`OfflineStore` lists the directory on every query** (`has`, `take`, `bytes`, `count`, `evict`). `keptSongsSize` is read on settings changes. OK for a few thousand files; an in-memory index refreshed lazily would scale better.
9. **QSettings as a database.** Bounded caches (2,000 loudness values, 2,000 video covers, 2,000 fit choices, 1,000 artwork choices, order lists, scrobble queue) live in the INI settings file, which Qt rewrites as a whole.
10. **Python start-up cost per request.** Each search/album/lyrics request starts Python and imports ytmusicapi (and yt-dlp for audio). This keeps idle memory at zero but adds latency to every action. A persistent worker with an idle timeout would be a middle ground.
11. **Error handling by string matching.** Friendly errors come from substring checks (`"Sign in"`, `"bot"`, `"Unable to download"`) on helper output, which breaks if yt-dlp rewords its messages.
12. **Version string duplicated** in `CMakeLists.txt`, `main.cpp` (twice), `jellyfin.cpp`, `online_artwork.py`; and `catalog.py`'s LRCLIB User-Agent still says `Sung/0.11.0`. Generate it from CMake instead.
13. **README vs code mismatches (minor).** README says kept songs go "up to 16 GB" (true for the UI picker) while the backend clamps to 65,536 MB; README's watch limit is 4,096 + 10,000 while the code's set cap is 14,160. Smart-playlist source rule `"subsonic"` actually means *any* server (Jellyfin and Cider included).
14. **Single-step, typed Undo.** Undo stores one previous state per category (`queue`, `queue-order`, `history`, `playlists`) with hand-written invalidation rules (`invalidateUndo`). Easy to get wrong when adding features; a command stack would be more robust.
15. **No exceptions** (`-fno-exceptions`). Fine with Qt, but any standard-library allocation failure terminates the process.
16. **Hardware video decoding off by default.** Good for stability, but the Motion layout's 2K/4K HEVC covers are then decoded in software (the code notes ~2× the decoding cost of 1080p).
17. **Accepted idle CPU.** The ambient backdrop drift costs ~9 % of a core while a cover is shown — accepted by the author, but notable for laptops.
18. **Flaky UI tests acknowledged.** Some frame-sampled motion checks and 200 ms focus checks fail occasionally; screenshots differ between identical runs.
19. **Diagnostics compiled into the main binary.** With `SUNG_DIAGNOSTICS` the app links QtTest and ~60 stage entry points in `main.cpp`; the long `if(args.contains(...))` chain is easy to break (an unknown flag silently launches normally and exits 0).
20. **Heavy process requirements for full verification**: Google Sans Flex installed, network, real audio session, Qt Test tools, dbus tools, optionally Navidrome/Jellyfin binaries and Hyprland.

## 5. Things that look odd but are deliberate

- Navigation bar placed at the **top** (Material puts it at the bottom) because the player owns the bottom edge.
- Selections/menus use **secondary** colours instead of Material's tertiary/token defaults (tertiary turns green over warm covers).
- No crossfade inside an album — not a bug, a musical choice.
- QML `Basic` style restyled by hand instead of Qt's Material style — Qt's Material style lags M3 Expressive.
- Blurs and shadows done on the CPU / with stacked rectangles — keeps the software renderer working.
- Buffers in `~/.cache` not `/tmp` — `/tmp` is RAM on CachyOS.

## 6. What to do differently in a new project (summary)

- Split the backend into services with typed models; keep QML dumb.
- Use SQLite (or at least append-only logs) for library, history and stats.
- Define a `MusicSource` interface; make YouTube, local, Subsonic, Jellyfin, Cider all implement it (YouTube currently lives outside `MusicServer`).
- Keep the helper-process idea but consider a pooled worker; return structured error codes.
- Generate version and constants from one place; add i18n from day one.
- Add CI (build + unit tests + a short offscreen smoke stage) and packaging (Flatpak/AUR).
- Keep: the token/generation pattern, the offline store design, the two-deck engine, the colour/motion/shape engines, bounded caches, the in-app UI test harness, and the "measure everything" habit.
