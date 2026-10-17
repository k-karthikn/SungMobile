# Tech stack

## Languages

| Language | Where | Notes |
| --- | --- | --- |
| **C++20** | `src/` | Compiled with `-fno-exceptions` (Qt doesn't throw; saves 201 KB of landing pads), hidden visibility, `-Wall -Wextra -Wpedantic`. Lambdas, `std::optional`, structured bindings, `decltype(auto)` templates. |
| **QML + JavaScript** | `qml/` | Qt Quick declarative UI. Precompiled to bytecode only (`--only-bytecode`), source stripped from the binary (`DISCARD_QML_CONTENTS`). |
| **Python 3** | `helper/` | Plain standard library + `ytmusicapi` + `yt-dlp`. Run as one-shot processes. |
| **Bash** | `scripts/` | Build/install wrappers. |
| **CMake** | `CMakeLists.txt` | 3.24+, Ninja generator. |

## Qt 6 (≥ 6.8) modules

| Module | Used for |
| --- | --- |
| Core | Objects, signals, `QSettings`, `QProcess`, JSON, files, timers |
| Concurrent | `QtConcurrent::run` for cover decoding on a private thread pool |
| Gui | `QImage`, `QPainter`, `QColor`, image reading |
| Quick | Scene graph, `QQuickPaintedItem`, `QQuickItem` + `QSGImageNode` custom items, image provider |
| Qml | Engine, context properties, type registration, QML module `SungUi` |
| QuickControls2 | Templates (`T.Dialog`, `T.ScrollBar`) and controls (restyled from "Basic") |
| Multimedia | `QMediaPlayer` ×2 (decks), `QAudioOutput`, `QAudioBufferOutput` (decoded PCM tap), `QMediaDevices`, `QVideoSink` (animated cover frames). FFmpeg backend. |
| Network | `QNetworkAccessManager`, `QNetworkDiskCache` (artwork), REST clients |
| DBus | MPRIS adaptors, notifications |
| Svg | `QSvgRenderer` for Material Symbols |
| Wayland | Platform plugin (package dependency) |
| ImageFormats | WebP covers (package dependency) |
| Test, GuiPrivate | Tests and diagnostics build only |
| QtQuick.Shapes | `Shape`/`ShapePath` with `CurveRenderer` for shapes, wavy lines, spectrum ring |
| QtCore (QML) | `Settings` for UI-only persisted state |
| QtQuick.Dialogs | `FileDialog`, `FolderDialog` (created lazily) |

Version-dependent code: pitch compensation and `beginFilterChange/endFilterChange` are used only on Qt ≥ 6.10; older versions fall back.

## Python packages (`helper/requirements.txt`)

| Package | Pinned | Used for |
| --- | --- | --- |
| `ytmusicapi` | 1.12.3 | YouTube Music home, search, album, playlist, artist, radio (watch playlist), lyrics, song details |
| `yt-dlp[default]` | 2026.8.19 | Resolve and download the audio stream (Opus/WebM preferred, AAC/M4A fallback). Uses **Node.js** as its JavaScript runtime (`js_runtimes: {'node': {}}`) to solve YouTube's player challenges. |

Installed in an isolated venv (`<prefix>/lib/sung/runtime` or `./runtime` for development).

## System tools called at runtime

| Tool | Package (Arch) | Why |
| --- | --- | --- |
| `python3` | python | Helper processes |
| `node` | nodejs (20+) | yt-dlp JS challenge solving |
| `ffprobe`, `ffmpeg` | ffmpeg | Tags, stream info, cover extraction (embedded art → 512 px JPEG), validating animated cover MP4s |
| `secret-tool` | libsecret | Store/lookup passwords and tokens in the Secret Service keyring |
| `pactl` | libpulse | `pactl subscribe` + `pactl -f json list sinks` to detect headphone unplug |
| `parec` | libpulse | Record the default output monitor so the visualizer works for Cider playback |
| `gtk-update-icon-cache`, `update-desktop-database` | (optional) | During install |

PipeWire answers `pactl`/`parec` through `pipewire-pulse`.

## Fonts

- **Google Sans Flex** (preferred; variable font) — not bundled.
- **Noto Sans** fallback.

## Build configuration highlights (`CMakeLists.txt`)

- `project(Sung VERSION 0.12.0 LANGUAGES CXX)`; Linux only (fatal error otherwise).
- `CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELEASE` (LTO) when supported.
- Release: `-Oz -ffunction-sections -fdata-sections`, link `-Oz -Wl,--gc-sections -Wl,-z,pack-relative-relocs`, post-build `strip --strip-unneeded`.
- QML module `SungUi` with `NO_PLUGIN`, `NO_RESOURCE_TARGET_PATH`, `DISCARD_QML_CONTENTS`; `Theme.qml` declared singleton; qmlcachegen `--only-bytecode`.
- Icons compiled into resources with zstd `--compress 19 --threshold 0`.
- `BUILD_TESTING` adds ~12 Qt Test executables; `SUNG_DIAGNOSTICS` compiles the UI test harness into the main binary.
- Install: `bin/sung`, `lib/sung/{catalog.py,online_artwork.py,requirements.txt}`, `.desktop`, 512 px icon, licences.

## Supported platforms / environment

- Linux only. Designed for CachyOS on Wayland (Hyprland referenced), works on X11/XWayland (window pinning only works there).
- Qt software renderer must keep working → **no custom shaders, no `MultiEffect`/blur effects**; all blurs and shadows are CPU or geometry.
- Hardware video decoding disabled by default.

## Data formats

- JSON (library, helper protocol, REST APIs), INI (`QSettings`, Noctalia), LRC (lyrics), M3U/M3U8 (playlists), Netscape cookies.txt (YouTube cookies), HLS playlists (Apple motion covers, parsed by hand), MP4/WebM/GIF/WebP (animated covers), JPEG/PNG/WebP (covers).

## Dev / test tooling

- Qt Test (`QTest`), CTest.
- Python `unittest` for helper tests.
- `dbus-run-session`, `qdbus6` for MPRIS/notification tests.
- `grim` (Wayland screenshots) for the "run on the real desktop" rule.
- Navidrome and Jellyfin binaries for live integration scripts; a fake Cider server (`tests/cider_fixture.py`).
