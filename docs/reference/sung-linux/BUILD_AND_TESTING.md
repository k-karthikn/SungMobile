# Build and testing

## Install (end user)

```bash
# CachyOS / Arch
sudo pacman -S --needed git base-devel cmake ninja python nodejs ffmpeg \
  qt6-base qt6-declarative qt6-multimedia qt6-svg qt6-wayland qt6-imageformats
git clone https://github.com/yappologistic/Sung.git && cd Sung
./scripts/install.sh            # per-user into ~/.local, never with sudo
~/.local/bin/sung
```

`install.sh` = `build.sh` + `cmake --install --prefix ~/.local` + create venv at `~/.local/lib/sung/runtime` + `pip install -r requirements.txt` + refresh icon cache / desktop database. `uninstall.sh` removes the program but keeps library and settings.

Other distros: Qt 6.8+ (Core, Gui, Quick, Qml, QuickControls2, Multimedia, Network, DBus, Svg, Wayland), C++20 compiler, CMake 3.24+, Ninja, Python 3 with venv/pip, Node.js 20+, FFmpeg, Qt image-format plugins (WebP).

## Develop

```bash
./scripts/setup.sh    # venv in ./runtime with ytmusicapi + yt-dlp
./scripts/build.sh    # Release build in ./build (BUILD_TESTING=OFF, SUNG_DIAGNOSTICS=OFF)
./scripts/run.sh      # sets SUNG_HELPER=./helper/catalog.py, SUNG_PYTHON=./runtime/bin/python
```

Useful command-line flags: `--isolated` (no single-instance socket, no MPRIS, onboarding skipped), `--offline` (no start page fetch), `--mini`, `--version`, `--screenshot <path>`, `--smoke-local <file>`, `--smoke-stream`, or a YouTube URL.

Useful environment variables: `SUNG_HELPER`, `SUNG_PYTHON`, `SUNG_NOCTALIA_COLORS`, `SUNG_OUTPUT_TAP` (replace `parec`), `SUNG_BUILD_JOBS`, plus test-only `SUNG_TEST_OUTPUT`, `SUNG_FIXTURE_HELPER`, `SUNG_CIDER_FIXTURE`, `SUNG_REQUEST_LOG`, `SUNG_BUFFER_FIXTURE`, `SUNG_MOTION_FIXTURE[_<quality>]`, `SUNG_ALBUM_FIXTURE`, `SUNG_CIDER_LIVE_TOKEN`.

## CMake targets

| Target | Contents |
| --- | --- |
| `sung` | The app. With `-DSUNG_DIAGNOSTICS=ON` also links `tests/uitest.cpp`, `immersive_test.cpp`, `immersive_edges.cpp`, `expressive_test.cpp`, `feature_test.cpp`, `motion_layout_test.cpp`, `cider_stage.cpp`, `tour.cpp`, `layout_audit.cpp`, `colour_audit.cpp`, `benchmark.cpp` + QtTest. |
| `sung-librarydata-tests` | JSON de-duplication reader |
| `sung-m3motion-tests`, `sung-m3shape-tests`, `sung-m3color-tests` | Engines vs. physics / Material reference values |
| `sung-subsonic-protocol-tests`, `sung-jellyfin-protocol-tests` | Protocol against a local mock server (malformed responses, redirects, cancellation, credential persistence, failed downloads) |
| `sung-tests` (backend), `sung-crossfade-tests`, `sung-offline-tests`, `sung-subsonic-tests`, `sung-jellyfin-tests`, `sung-cider-tests`, `sung-notification-tests` (under `dbus-run-session`), `sung-artwork-tests` (software renderer) | Whole-backend tests with fixture helpers |
| `sung-artwork-fixture` | Diagnostics helper |

## Test commands

```bash
./scripts/test.sh                 # build-tests + ctest + python -m unittest (tests/test_*.py)
./scripts/verify.sh --offline     # every stage incl. screenshots, offline (20–30 min)
./scripts/verify.sh               # adds live YouTube/network stages
python3 tests/profile.py build/sung [--mini]       # idle CPU, RSS, PSS, binary size
python3 tests/immersive_regression.py --binary <diag sung> --output verification/immersive
python3 tests/navidrome_integration.py --navidrome <bin> --test-binary build-tests/sung-subsonic-tests --output verification/navidrome
python3 tests/jellyfin_integration.py --server-binary <bin> --test-binary build-tests/sung-jellyfin-tests --output verification/jellyfin [--ui-binary <sung>] [--native-ui]
SUNG_CIDER_LIVE_TOKEN=... ctest --test-dir build-tests -R cider --output-on-failure
```

## The verification harness (`tests/verify.py`)

1. Configure + build a diagnostics build in the report folder.
2. Environment: offscreen platform, **software** Qt Quick backend, `QSG_RENDER_LOOP=basic`, HW decode off, a private fontconfig that only exposes Google Sans Flex.
3. Each stage runs in its own isolated `XDG_CONFIG_HOME`/`XDG_DATA_HOME`/`XDG_CACHE_HOME`, with `SUNG_HELPER=tests/catalog_fixture.py` (deterministic fake catalogue: fake songs, slow/error queries, timed lyrics, fake artwork, generated silent WAV buffers).
4. A stage fails on non-zero exit **or** if its log contains `ReferenceError`, `TypeError`, `Binding loop`, `Unable to assign`, `Cannot assign`, `FAIL`.
5. Stages: backend (ctest), catalog (python), mpris, immersive-regression, interaction-refinement, listening-refinement, visual-refinement, online-artwork, local-artwork, folder-import, search-selection, qol, interaction, product-polish, library-polish, playback-polish, audio-indicator, visual-delight, library-qol, visual-polish, ambient-immersive, personalization, home-rail, onboarding, library-exchange, backdrop-pulse, playback-memory, footprint, dynamic-color, navigation-motion, artist-hero, singalong, crossfade-ui, track-details, queue-history, window-wash, motion-layout, 12 `material-*` conformance stages, cider, interface-audit, layout-audit, colour-audit, (live) online-artwork-live, native-features, live-lyrics-motion, playback-recovery, ui-playback, ui-audit, ui-hidpi (1.6×), idle-performance, mini-performance, noctalia-preview.
6. Writes `report.json` (`passed: true` = green) and `report.txt`. Offline runs may skip only the live stages.

Each stage is a `--<name>-test` flag handled in `main.cpp`, declared in `tests/uitest.h`, listed in `verify.py`. Stages drive the **real UI**: find items by `objectName`, click at real coordinates, press real keys, wait for real state, take screenshots, sample frames to check motion curves, check colours/contrast and layout at multiple widths.

Harness gotchas recorded by the author:
- An unknown `--x-test` flag falls through to a normal launch and exits 0 (looks like a pass) → check the flag exists and screenshots were written.
- `console.log` from QML prints nothing under the harness → assert from C++ with `findItem` + property reads.
- Some motion/focus stages are flaky by ~200 ms; rerun the base build 3× before calling a regression. Tour screenshots differ by thousands of pixels between identical runs.
- `tests/benchmark.py` leaves a 115 MB `silence.wav` per output folder.
- Run stages one at a time; parallel runs starve each other into timeouts.

## The project's "definition of done" (CLAUDE.md)

A feature counts as built only when all are true:

1. Real automated tests against the real thing (not a mock that would pass with the feature removed).
2. A simulated user test through the UI (real clicks/keys/waits).
3. Visual captures of its states — and someone looked at them.
4. UI/UX audit: narrow/wide, focus order, keyboard reach, accessibility roles/names, disabled/empty states, light/dark, reduced motion.
5. Measurements: idle CPU, RSS/PSS, binary size, fluidity — compared with before.
6. One run on the real compositor with an isolated profile, photographed with `grim`.
7. Documentation of the Material rules in the code beside what they govern.

## Repo hygiene rules

- `.gitignore` is an **allowlist**: only listed paths/extensions may be committed; README.md and CLAUDE.md are the only Markdown files allowed.
- Commit message: imperative subject ≤ 50 (max 72) chars, optional body ≤ 5 lines wrapped at 72; no co-author or generated-by trailers; noreply author address; no session links anywhere.
- One commit = one coherent change; never mix refactor and feature.
- Ask before anything leaves the machine (push, PR, release).
- Prose rules: no filler openers/closers, no "robust/seamless/leverage…", no em dashes, no emoji, say the specific number.
