# Performance

Sung treats speed and memory as features. The rule from its `CLAUDE.md`: every change is weighed for per-frame work, allocations in hot paths, texture churn, timers that keep the process awake, and work done while hidden. The Qt **software renderer must keep working**, so no shader effects.

Below: every optimisation found in the code, with the measured numbers the author recorded.

## Startup

| Technique | Effect (as recorded in code) |
| --- | --- |
| Compile `Main.qml` asynchronously while the backend is constructed | 231.8 → 213.7 ms to interface (empty library, desktop); 392.7 → 370.0 ms (10,000 songs, offscreen) |
| QML precompiled to bytecode only, source stripped | Smaller binary, no runtime compile |
| Every dialog's body built on first open (`MDialog.built`, `Loader`); menu items build their insides on first show; file dialogs created on first use | Fewer objects at startup (the `SUNG_OBJECT_PROBE` checks deferred parts are absent until asked for) |
| Library JSON load interns strings and shares duplicate song objects | Lower resident memory for large libraries |
| `malloc_trim(0)` after the first frame | Returns 16 MiB (desktop) / 24 MiB (offscreen) still held after loading a 10k-song library |

## Memory

| Technique | Effect |
| --- | --- |
| `QSG_ATLAS_WIDTH/HEIGHT = 1024` (instead of window size rounded up to a power of two) | Avoids a 2048² atlas: 16 MiB heap + 15 MiB pinned (NVIDIA) for icons that only filled a tenth of it; a full UI tour at 1.6× needs 0.80 Mpx, which 1024² holds |
| Icons use `Image.cache` and one tinted raster per (symbol, size, ink) | Atlas use 1.67 Mpx → 0.80 Mpx over a tour |
| Icon fill/semibold layer only built for the 7 symbols that have a second form | Hundreds fewer `Image` items |
| `malloc_trim` after every library save and when the app becomes inactive | Four saves of a 10k library had left the process 114 MiB larger, all freed memory; trim takes 4.7 ms (save) / < 0.5 ms (inactive) |
| One decoded-cover cache (8 MiB) shared by all surfaces + QImage implicit sharing + shared in-flight decodes | No per-view copies of the same cover |
| Decode at target size (`setScaledSize`), pack opaque covers as RGB888 | ~25 % less per opaque cover vs ARGB32 |
| `RoundedArt::fitTextureSize` — render target no larger than the picture; 1×1 when empty | Big saving for full-window backdrops built from a 160 px cover |
| Blur on a tiny image, draw enlarged | Backdrop costs one small texture, no per-frame effect |
| One shared `MotionArtwork` decoder for all now-playing surfaces; frames capped at 800 px; released when hidden | Animated covers cost one extra decoder |
| Variable-font faces bounded (poster lyrics ≤ 48 faces) | Each axis combination is ~0.65 MiB; animating axes per frame cost "a quarter of a gigabyte for one line" before the fix |
| `WindowResources`: 30 s after hide/minimise, release scene graph + graphics resources | Hidden window gives GPU/CPU memory back; QML state is kept |
| Bounded collections everywhere (history 200, back stack 12, view state 32, play log 20k, etc. — see DATA_MODEL) | Memory can't grow without limit |
| `MElevation` builds no shadow rings at level 0 | Most surfaces carry zero shadow objects |
| ListViews with `reuseItems: true` and small `cacheBuffer` | Delegate pooling; pooled delegates hide from Tab |

## CPU while idle

| Technique | Effect |
| --- | --- |
| Position updates throttled to 4 Hz; only while playing and visible | Few wake-ups |
| `lyricIndexChanged` emitted only when the line changes | Lyric delegates don't re-evaluate 4× a second |
| Meter levels published ≤ 30 Hz; idle timer resets them; no work when motion is off or window hidden | |
| FFT spectrum computed only while the visualizer is on screen and playing (a few µs per frame) | Zero cost otherwise |
| `parec` output tap runs only during Cider playback with the window visible | |
| Cider polling: 1 s playing, 2 s paused+visible, none hidden | |
| Every continuous animation checks `visible && window visible && not minimised && app.motion` | Nothing animates off-screen |
| No `QSG_RENDER_LOOP=threaded` | Forcing it nearly tripled idle CPU and rendered at full refresh rate |
| Accepted idle cost | The ambient backdrop's drift/sway: ~9 % of a core while a cover shows, 0 without one |

Idle CPU varies with machine load: the same binary read 2.8 % on a quiet machine and 9 % on a busy one, so the project compares builds in **interleaved pairs** and measures the noise floor first.

## Smoothness (no dropped frames)

| Technique | Effect |
| --- | --- |
| Library saves on a background thread | Serialising a large library took ~⅓ s and used to freeze every frame on each song change |
| Cover decoding on a private 2–4 thread pool | Scrolling rows no longer stall the frame being drawn |
| `Entries::reconcile` for queue edits ≤ 512 rows | Delegates survive; no list flash |
| Wavy progress / seek wave as static geometry moved by a render-thread transform | No JavaScript painting per frame |
| Sing-along uses scale, not font size, for emphasis | Changing font size re-shapes text each frame and stutters |
| Line fill animated between position reports | Sweeps smoothly instead of stepping 4×/s |
| Scrim solving reads a 16×16 sample grid with a linearisation lookup table | Cheap enough to run on every palette animation frame |
| Colour scheme cache (256) + Oklch step precomputation | Theme animation only interpolates cached colours; Content variant uses 3 steps (13 uncached Content schemes took 136 ms vs 16 ms for 3) |
| Shape outlines built once (function-local static) | Morphs only blend radii |
| `shapeOffset` checks only edges inside an angular window (bisection) | Runs every frame during a visualizer morph |
| Motion backdrop edge rebuilt every 2nd video frame (~15 blurs/s for 30 fps) | Few hundred µs each |
| Browser-style smooth wheel (`MSmoothWheel`) | Glides per notch, touchpad 1:1 |

## Network and disk

| Technique | Effect |
| --- | --- |
| Whole-song buffering, then kept in the offline store | Replays cost no network |
| Prepare next song 45 s before the end | No gap waiting for the next stream |
| Kept songs skip preparation | No re-download |
| `posix_fadvise(DONTNEED)` on prepared buffers | Doesn't evict useful page cache |
| Buffers under `~/.cache`, not `/tmp` | `/tmp` is RAM (tmpfs) on CachyOS |
| Rename into store (same filesystem) instead of copy | No copy on the playback path |
| Stream URL cache (5 entries) reused while > 2 min from expiry | |
| Artwork HTTP disk cache 48 MiB; URL sizing asks for the drawn size | Smaller downloads, crisp covers |
| Data saver YouTube quality | ~67 kbps Opus instead of ~130 kbps |
| Helper processes exit after each request | No idle Python in memory |

## Binary size

| Technique | Effect |
| --- | --- |
| `-fno-exceptions` | −201 KB (landing pads/exception tables) |
| `-Oz`, LTO, `--gc-sections`, `-ffunction-sections -fdata-sections`, `-z pack-relative-relocs`, strip, hidden visibility | Minimal release binary |
| QML bytecode only, sources discarded | Bytecode is smaller than the source it replaces |
| Icons zstd-compressed at level 19 in resources | |

## How it is measured

- `tests/profile.py <binary> [--mini]`: launches isolated + offline, waits 2 s, samples CPU over 5 s from `/proc/<pid>/stat`, reads RSS and PSS from `smaps_rollup`, reports binary size.
- `tests/startup_benchmark.py` and `SUNG_STARTUP_PROBE`: prints `STARTUP_READY_MS` and `STARTUP_FRAME_MS`.
- `SUNG_OBJECT_PROBE=<n>`: object tree counts per class, painted items and their render-target MiB, optionally after opening a named dialog (`SUNG_PROBE_OPEN`).
- `SUNG_IDLE_PROBE=<seconds>`: frames drawn while idle, and every running animation/timer by path — to find what keeps the process awake.
- `--benchmark` (diagnostics build) and `tests/benchmark.py`; `--footprint-test`, `--playback-memory-test` stages.
- The offscreen harness can't see GPU memory (driver copies the atlas into the process), so memory is also measured on the real desktop.

## Rules worth adopting

1. Measure before and after, in interleaved pairs, against a baseline built from a `git worktree`.
2. A regression is a bug unless stated, justified and accepted.
3. Nothing animates or polls while hidden.
4. Bound every cache and list.
5. Do expensive things once at small size, then scale up.
