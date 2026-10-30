# Sung — codebase study notes

These notes describe **Sung**, a native Linux music player (repo: https://github.com/yappologistic/Sung), written after reading the whole repository at commit `4918f76` (26 Sep 2026, version 0.12.0, 285 commits, MIT licence).

They are meant to be used as a reference when building a different project that borrows ideas, structure or code from Sung.

## In one paragraph

Sung is a desktop music player for Linux. It plays YouTube Music (without a browser), your own music files, Navidrome/Subsonic and Jellyfin servers, and Apple Music (by remote-controlling the Cider app). The window is built with **Qt 6 Quick (QML)**, the engine is **C++20**, and the YouTube and file-tag work is done by short-lived **Python** helper processes (`ytmusicapi`, `yt-dlp`, `ffprobe`/`ffmpeg`). The whole interface follows **Google Material 3 (Expressive)** very strictly: colours, type, shapes and motion are computed from Material's own published numbers, not chosen by eye.

## The files

| File | What it covers |
| --- | --- |
| [OVERVIEW.md](OVERVIEW.md) | What Sung is, who it is for, the feature list, size of the codebase |
| [ARCHITECTURE.md](ARCHITECTURE.md) | How the pieces fit: QML ↔ C++ backend ↔ Python helpers, startup, models, navigation, playback engine, data flow diagrams |
| [UI.md](UI.md) | Every screen, panel, dialog and component; window size behaviour; keyboard shortcuts; accessibility |
| [THEME.md](THEME.md) | Colour system, dynamic colour, type scale, shape scale, spacing, elevation, motion springs, icons — with all the numbers |
| [TECH_STACK.md](TECH_STACK.md) | Languages, Qt modules, Python packages, system tools, build flags, runtime requirements |
| [SOURCES.md](SOURCES.md) | External services, APIs and reference documents the code depends on or cites |
| [METHODS_AND_TECHNIQUES.md](METHODS_AND_TECHNIQUES.md) | The clever bits explained: HCT colour solver, spring-to-bezier fitting, shape morphing, crossfade, FFT visualizer, scrim solving, artwork matching, and more |
| [PERFORMANCE.md](PERFORMANCE.md) | Every memory/CPU/size optimisation and the measured numbers behind them |
| [DATA_MODEL.md](DATA_MODEL.md) | Track object fields, ID formats, `library.json`, settings keys, caches, limits |
| [INTEGRATIONS.md](INTEGRATIONS.md) | YouTube, Subsonic, Jellyfin, Cider, LRCLIB, Apple artwork, MusicBrainz, ListenBrainz, MPRIS, notifications, keyring |
| [BUILD_AND_TESTING.md](BUILD_AND_TESTING.md) | Build scripts, CMake targets, the test harness, the project's own "definition of done" |
| [PROBLEMS.md](PROBLEMS.md) | Known limits, risks, weak spots, tech debt and things to do differently |
| [FILE_MAP.md](FILE_MAP.md) | One line (or a short paragraph) per file in the repo |
| [REBUILD_GUIDE.md](REBUILD_GUIDE.md) | How to use all of this to start a related project: what to copy, what to simplify, a suggested order of work |

## How to read them

If you only have 15 minutes: read **OVERVIEW**, then **ARCHITECTURE**, then **REBUILD_GUIDE**.

If you are going to build a UI that looks like Sung: **THEME** and **UI** first, then the colour/shape/motion parts of **METHODS_AND_TECHNIQUES**.

If you are going to build the playback side: **ARCHITECTURE** (playback section), **INTEGRATIONS**, **DATA_MODEL**, **PERFORMANCE**.

## A note on accuracy

Numbers, limits and file names in these notes were taken from the source code itself, not from the README alone. Where the README and the code disagree, the notes say so (see PROBLEMS.md). Line counts are approximate and refer to the commit above.
