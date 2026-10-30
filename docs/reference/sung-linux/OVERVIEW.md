# Overview

## What Sung is

Sung is a **native Linux desktop music player**. "Native" here means it is a compiled C++/Qt program, not an Electron or web app. It was written for **CachyOS (an Arch Linux variant) running Wayland**, but builds on any Linux with Qt 6.8+.

Tagline from the README: *"YouTube Music, your music files, and your music server. Native on Linux."*

It brings five music sources into one window with one queue:

| Source | How Sung reaches it |
| --- | --- |
| **YouTube Music** | Python helper using `ytmusicapi` (browse/search) and `yt-dlp` (audio). No account, no embedded browser, no ads. |
| **Local files** | Import files or whole folders; tags read with `ffprobe`; folders are watched for changes. |
| **Navidrome / Subsonic** | Subsonic REST API 1.16.1 with token auth (MD5 of password + salt), JSON responses. |
| **Jellyfin** | Jellyfin REST API with a session token in the `Authorization` header. |
| **Apple Music** | Through **Cider** (a separate Apple Music client) using its local REST API on port 10767. Sung keeps the queue; Cider makes the sound. |

Local playlists can mix songs from YouTube, files and a server in one list.

## Who it is for

A Linux user who wants one good-looking, light, keyboard-friendly player for streaming plus their own library, with no telemetry. The author treats **looks, motion and resource use** as first-class features.

## Feature list (grouped)

**Playback**
- Two-deck engine: true **crossfade** (0–12 s, equal-power curve) and **gapless** handover.
- Crossfade is skipped automatically between two tracks of the same album and into autoplay songs (they are joined seamlessly instead).
- Whole-song buffering to disk before play; the **next song is pre-fetched** 45 s before the current one ends.
- **Offline cache** ("Keep played songs"): songs already downloaded are kept under a size budget and replayed without network.
- **Volume normalisation**: ReplayGain/R128 tags for files; otherwise loudness is measured from the decoded audio (after 45 s of playback) and applied from the next play. Gain limited to −15…+6 dB.
- Per-song **volume trim** (±12 dB, 0.5 dB steps).
- Playback speed 0.5×–2× with optional pitch preservation (Qt ≥ 6.10).
- Shuffle, repeat (off / all / one), autoplay radio, "smart shuffle" that spreads artists apart.
- **Sleep timer**: minutes, end of track, end of queue; optional 30 s fade-out.
- **Resume long recordings** (≥ 20 min) where you left off; up to 400 remembered.
- Audio output device picker; optional pause when headphones are unplugged (via `pactl`).
- Listening sessions: save/restore a whole queue with position, speed, shuffle and repeat (20 sessions × 2,000 songs).

**Library**
- Likes, history (200 rows), pins on Home (24), playlists, smart playlists (32, rule-based), mixes (Recently liked, Rediscover, Unplayed).
- Folder import with recursive scan, change watching, missing-file marking (never deletes).
- Local albums/artists browser grouped by album-artist tags, disc/track order.
- M3U import/export (local files only), library JSON import/export.
- Playlist cleanup (duplicates, missing files), **playlist version history** (12 versions / 5,000 rows), custom playlist covers (cropped to 512 px).
- Multi-select, drag reordering, swipe-to-remove in the queue, single-step Undo for most edits.
- Type-ahead jump inside lists.
- Listening statistics with a GitHub-style yearly heat-map calendar and streaks.

**Lyrics**
- Synced lyrics from YouTube, the music server, Jellyfin, sidecar `.lrc` files, imported LRC, or LRCLIB as a fallback.
- Per-song timing offset (±10 s), lyric search with jump-to-line, seek-bar lyric preview, intro/gap countdown.
- **Poster-style lyrics**: the current line set with Google Sans Flex's variable axes (weight, width, roundness, slant) stretched to fill the line.
- **Sing along** layout with a fill sweeping across the line.

**Artwork and looks**
- Material 3 light/dark theme, hand-picked accent colour, or colour taken from the current cover.
- Five colour scheme variants and three contrast levels, all computed in C++.
- Animated covers (GIF/WebP/MP4/WebM sidecar files, or fetched from Apple Music's public pages for YouTube songs).
- Video-frame thumbnails replaced by real album covers (Apple, then MusicBrainz/Cover Art Archive).
- Ambient blurred-cover backdrop that slowly drifts and "breathes" with the bass.
- Immersive full-screen player (F11) with six layouts: Artwork, Lyrics, Split, Sing along, **Visualizer** (cover cut into a morphing Material shape ringed by 72 spectrum bars), **Motion** (cover video filling the screen).
- "Up next" cover carousel.
- Mini player window.

**Desktop and accessibility**
- MPRIS (media keys, desktop media widgets), optional track notifications, Noctalia desktop palette support.
- Single-instance: launching again raises the window or opens a passed YouTube link.
- Full keyboard reach, visible focus rings, headings for screen readers, 4.5:1 text contrast solved in every theme.
- Command palette (Ctrl+Shift+P), shortcut help (?).

**Privacy**
- No analytics. Passwords/tokens go in the desktop keyring (`secret-tool`). Library exports never include credentials or server URLs. A "Pause history this session" private mode stops history, stats and scrobbling.
- Optional ListenBrainz scrobbling.

## Size of the codebase

| Folder | Files | Lines | Language |
| --- | --- | --- | --- |
| `src/` | 48 | ~12,900 | C++20 |
| `qml/` | 89 | ~12,800 | QML/JavaScript |
| `helper/` | 3 | ~1,030 | Python 3 |
| `tests/` | ~50 | ~25,400 | C++ (Qt Test) and Python |
| `scripts/` | 7 | ~50 | Bash |
| `assets/icons/` | 138 SVGs | — | Material Symbols Rounded, 3 optical sizes |

The test code is about as large as the product code, which says a lot about how the project is run.

## The project's philosophy (from its `CLAUDE.md`)

The repo contains a rules file written for AI coding agents. It is a good summary of the author's values:

1. **Material 3 is the source of truth.** Every measurement, colour role, shape and motion follows Google's spec, and the code comment says which rule and why. Numbers come from the Jetpack Compose Material 3 token files, not from memory.
2. **Minimal, not timid.** No decorative text, no filler sections, but expressive shape and motion are welcome.
3. **A feature is done only when** it has real tests, a simulated-user test (real clicks), screenshots that were looked at, a UI/UX audit (narrow/wide, focus, light/dark, reduced motion), CPU/memory/size measurements, a run on the real desktop, and documentation in the code.
4. **Motion = springs.** All animations use one of six solved Material springs; hand-picked durations are treated as bugs.
5. **Performance is a feature.** The software renderer must keep working (no shaders). Work done while hidden is a defect.
6. **Plain, specific prose** in comments and commits; strict commit message shape.
