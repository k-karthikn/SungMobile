# Methods and techniques

The interesting engineering in Sung, each explained in plain words first, then how it works, then where it lives. These are the parts most worth reusing.

---

## 1. Material dynamic colour from one seed (HCT solver)

**Plain words:** pick one colour; derive a whole matching, accessible colour scheme from it, the way Android does.

**How:**
1. Measure the seed in **HCT** = Hue and Chroma from the CAM16 colour appearance model + **Tone** = CIE L* (0 black … 100 white). Tone is what drives contrast, so contrast guarantees can be written as tone rules.
2. Build five **tonal palettes** (primary, secondary, tertiary, neutral, neutral variant) = a fixed hue + chroma that can be read at any tone. The variant (Tonal spot, Vibrant…) decides the hue rotations and chroma.
3. For each role pick an initial tone (e.g. primary 40 light / 80 dark), then **search tone in 0.25 steps** until the role reaches its contrast-curve target against *all* the surfaces it can sit on. Pairs like primary/primaryContainer are solved together so they stay ≥ 10 tones apart and avoid the muddy T50–59 band (MCU's "tone delta pair").
4. To turn (hue, chroma, tone) back into sRGB, the CAM16 inverse is iterated to the requested Y; if sRGB can't hold that chroma, chroma is **stepped down by 0.25 from the top** until it fits (MCU walks the gamut boundary analytically; stepping lands within a fraction of a unit).
5. Content ("Faithful") variant extras: tertiary from a **temperature analogue** (walk the hue wheel by cumulative "warmth" change), "disliked" dark yellow-greens pushed to tone 70.

**Where:** `src/m3color.cpp` (`measure`, `solve`, `palettesFor`, `scheme`, `toneFor`, `resolvePair`), cached in `Backend::colorScheme`.

## 2. Smooth theme changes in Oklch

**Plain words:** when the cover changes, the whole UI colour glides to the new palette instead of snapping, without passing through ugly in-between colours.

**How:** in `Theme.qml`, interpolate the *seed* in Oklch into 13 steps, compute a full scheme per step (cached), then animate a single `seedProgress` 0→1 on the critically damped defaultEffects spring. Each role reads `role(name)`, which interpolates between the two neighbouring schemes in Oklch (lightness, chroma, hue the short way). Greys borrow the other colour's hue so they don't swing around the wheel.

## 3. Springs turned into Qt easing curves

**Plain words:** Material describes motion as physical springs. Qt only understands "duration + curve". Sung solves the spring maths and produces an exact matching curve.

**How (`src/m3motion.cpp`):**
- Step response of a unit-mass spring: underdamped `x(t) = 1 − e^(−ζω₀t)(cos ω_d t + (ζω₀/ω_d) sin ω_d t)`, critically damped `x(t) = 1 − e^(−ω₀t)(1 + ω₀t)`, with ω₀ = √k.
- **Duration** = time until it stays within 1 % (Compose's threshold): analytic envelope for underdamped, bisection for critical.
- **Curve** = 8 cubic Bézier segments fitted using the analytic derivative as tangents (Hermite → Bézier: control points a third along each tangent). Last segment forced to land exactly on (1,1).
- Exposed to QML as `{ms, curve, damping, stiffness}` for six tokens; QML uses `easing.type: Easing.BezierSpline; easing.bezierCurve: Theme.springSpatial; duration: Theme.springSpatialMs`.
- For targets that keep moving (meter bars, spectrum ring), QML **integrates the spring per frame** (`a = −k(x−target) − 2ζ√k·v`, 4 sub-steps) instead of using a curve.

## 4. Material shapes and morphing without Compose

**Plain words:** draw Android's "cookie", "clover", "sunny" shapes in Qt and smoothly morph between them.

**How (`src/m3shape.cpp`):**
- Port of graphics-shapes `RoundedPolygon`: each vertex has a rounding radius; corners are cut back along both edges, short sides share the cut proportionally, and each corner becomes one circular-arc cubic (handles `4/3·tan(θ/4)·r`).
- Shapes are normalised to the unit square by the **bounds of their control points** (exactly what Compose does, so a circle sits slightly inside its box).
- Every shape is **star-shaped about its centre**, so it has exactly one radius per angle. The outline is sampled densely; `radiusAt(angle)` uses bisection on the unwrapped angle list + a ray/segment intersection.
- A **morph frame** = per-angle linear blend of two shapes' radii (720 samples), clamped to the box so spring overshoot doesn't get cut off.
- `shapeOffset()` pushes an outline outward by a margin *perpendicular to the edge* (not along the ray), so the visualizer's bars keep an even gap even down a clover's notch; it only checks edges within an angular window found by bisection (runs every frame during a morph).
- Same outline feeds the cover mask (`RoundedArt`), placeholders (`MShape`), the loading indicator and the spectrum ring, so they line up exactly.

## 5. Two-deck crossfade and gapless playback

**Plain words:** two players: while one plays, the other quietly loads the next song; at the end they hand over with a smooth equal-loudness blend (crossfade) or an instant switch (gapless).

**How:** see ARCHITECTURE → Playback engine. Key ideas:
- The spare deck is only armed with a source that needs **no network** (local file or pre-downloaded buffer), so the handover can't stall.
- **Equal-power** blend: `cos(tπ/2)` and `sin(tπ/2)` keep total power constant (linear fades dip in the middle).
- **Musical judgement**: no crossfade between two tracks of the same album (continuous albums/live sets) or into autoplay; those are joined gaplessly instead. No crossfade on tracks shorter than 2× the fade.
- The shuffle destination is decided *before* arming (so the pair judged is the pair played).
- A single PCM tap moves with the active deck.

## 6. Whole-file buffering + offline store with no index file

**Plain words:** download the entire song first, then keep it so the next play is instant and works offline.

**How:**
- Helper downloads into a private `QTemporaryDir` under the cache (≤ 32 MiB for YouTube, ≤ 512 MiB for servers).
- `OfflineStore::keep()` **renames** (same filesystem, no copy) the file into `~/.cache/.../songs/<sha256(key)[:32]>.<ext>`. Key = `yt|<videoId>|<quality>` or `<source>|<serverIdentity>|<id>|<bitrate>`; local files are never kept.
- **The directory is the database**: age = file mtime (touched on every play), size = file size. Eviction = sort by mtime, delete oldest until under budget. No second record to drift out of sync. A corrupt kept file is `forget()`-ten on decode error.

## 7. One-process-per-request helpers with cancellation

**Plain words:** each YouTube/file job is a fresh Python process; starting a new search kills the old one.

**How:** `Backend::request(channel, args, done)`; `setsid()` in the child so `kill(-pid, SIGKILL)` kills the process group (yt-dlp spawns children); JSON in/out; watchdog timer; result ignored if the channel has a newer process. No server, no idle worker, nothing running when idle.

## 8. Stale-result protection everywhere (tokens and generations)

Every async path captures a counter and checks it on return: `m_trackToken` (track changed?), `m_preparationGeneration`, `m_onlineArtworkGeneration`, `m_coverPlayToken`, `m_decode` (in each `RoundedArt`), `m_generation` (in each server client, bumped on disconnect), `m_serial` (Cider commands). This is the main reason the app has few race bugs.

## 9. Loudness normalisation without a full R128 meter

- Tags: ReplayGain track/album gain used directly; R128 (`Q7.8` vs −23 LUFS) converted: `gain = r128/256 + 5` to match the −18 dBFS reference.
- Otherwise `LoudnessMeter` keeps a running broadband RMS of decoded PCM (one multiply per sample); after ≥ 45 s it stores `levelDb` per track (2,000 kept) and the next play applies `−18 − measured`.
- Total gain clamped −15…+6 dB; the mixer can't exceed 1.0, so boosts only help when the user's volume leaves headroom. User volume is never rewritten.

## 10. Audio meters and FFT visualizer

- **5-band meter** (`AudioLevels`): five biquad band-pass filters (100, 350, 1,200, 3,500, 10,000 Hz, Q≈0.65) per channel (channels separate so out-of-phase stereo doesn't cancel), RMS → dB → 0–1 over a 48 dB range. Denormals flushed. Published ≤ 30 Hz.
- **36-band spectrum** (`AudioSpectrum`): ring buffer of the last 2,048 samples per channel; only when the visualizer is visible, a Hann-windowed radix-2 FFT per channel, power averaged, bands log-spaced 40 Hz–16 kHz, **+3 dB/octave tilt around 1 kHz** (music falls ~3 dB/octave), mapped over 54 dB. Peak per band.
- Ring: 72 bars; band index mirrored so bass is at the top and treble meets at the bottom; each bar a spring (fastSpatial ζ 0.6 / k 800) integrated per frame; drawn as one `PathMultiline` with round caps.

## 11. Scrim solving: readable text over any picture

**Plain words:** put just enough of a dark/light veil over the cover so text on top is readable — no more.

**How (`src/scrimcontrast.h`):** when a cover is decoded, sample a 16×16 grid of it. For a candidate scrim alpha, compute every sample's composited luminance (surface + (1−α)·pixel) and the minimum WCAG contrast vs the ink. Binary-search α (12 iterations) for the smallest value ≥ `base` that hits `Theme.textContrast`. Uses an sRGB→linear lookup table with interpolation so per-frame palette changes don't call `pow`. `MotionBackdrop` samples separate top/bottom bands, solves before blurring (so highlights aren't averaged away), and keeps the **max** scrim a looping video ever needed so text doesn't flicker.

## 12. CPU blur that works on the software renderer

`softimage::softened`: three passes of a separable **running-sum box blur** (O(pixels) regardless of radius) ≈ Gaussian. Always on a small image (e.g. 128–160 px) that is then drawn enlarged — the enlargement *is* the softness. Edges clamp. Used by ambient backdrop, motion edges, blurred covers.

## 13. Motion backdrop with feathered edges from two textures

Full cover texture (cropped via source rect, not clipped) + one tiny edge texture: the visible part shrunk to ~128 px, blurred (σ = 48 view px), with alpha = `1 − smoothstep(x)·smoothstep(y)` so it's opaque at borders and clear inside. Overlaid, that gives a sharp centre melting into blur — two `QSGImageNode`s, no shaders. Edge rebuilt every 2nd video frame.

## 14. Image loading pipeline

- One decoded-image cache (8 MiB) shared by all surfaces; QImage implicit sharing means two views of one cover share pixels.
- Concurrent requests for the same local cover subscribe to one decode (`localLoads` map of `QFutureWatcher`s).
- Private decode pool of 2–4 threads (`idealThreadCount/2`) so scrolling can't starve the global pool.
- `QImageReader::setScaledSize` decodes straight to the needed size; opaque RGB32 repacked as RGB888 (25 % smaller).
- **URL sizing**: Google `=wN-hN`, Apple `/NxNbb.`, Cover Art Archive `-250/-500/-1200`, YouTube `hqdefault` → `maxresdefault` when drawn > 240 px; capped at 1,600; on failure retry original.
- Generation token per surface discards results for a cover it already moved away from.
- `setTextureSize` shrinks the render target to the picture's real resolution for unrounded surfaces (backdrops), and to 1×1 when nothing is drawn.

## 15. Artwork matching (YouTube song → real album cover / animated cover)

Python `online_artwork.lookup`:
1. iTunes search `artist + title` (40 results, US). Normalise strings (NFKC, casefold, strip punctuation; remove "(Official Video)"-style labels but keep live/remix distinctions; strip " - Topic").
2. Exact match needs artist + title equal, album equal if known, duration within 5 s, numeric collection id. Without album metadata, refuse if results span more than one album name.
3. If no exact match but an album is known: verify the album's actual track list via `lookup?entity=song`. For singles/missing albums: pick the **earliest release** among matching albums (≥ 2 tracks), refuse if ambiguous.
4. Still cover = Apple `artworkUrl100` (validated by regex). If none, query MusicBrainz recordings (artist+title, length ±5 s) → release groups → Cover Art Archive front, accepted only if ≥ 500 px (parses JPEG/PNG headers itself).
5. Animated cover: fetch the album web page, parse `serialized-server-data`, find the header item for that album id/title/artist, take `videoArtwork` (or `tallVideoArtwork`) HLS master; choose a variant by shape/size/codec/SDR/≤30 fps/bandwidth; the variant must be a bounded, unencrypted single-MP4 VOD (byte ranges summed to pre-check size); download (≤ 16 MiB standard, ≤ 64 MiB large); validate with ffprobe (one video stream, h264/hevc, ≤ 30 fps, ≤ 60 s); atomically move into cache.
6. Records cached 1 day (miss) / 7 days (hit); 429 sets a global retry-after; one automatic retry in the backend. Only HTTPS to allow-listed hosts, redirects re-validated.

## 16. Lyrics parsing and timing

- LRC parser: multiple timestamps per line, `[offset:]`, equal timestamps merged (translations), end = next start (last line: duration or +5 s), ≤ 4,000 lines, ≤ 256 KiB.
- Current line by **binary search** on start times; `lyricIndexChanged` emitted only when the index changes (not 4× a second).
- Line "span" for fill animations: gap to the next line, but if that gap is > 4× the song's median gap (solos, last line) use the median instead, so the fill doesn't creep.
- Gap countdown shows when an explicit gap ≥ 5 s is coming (blank timed line or line end markers).
- Sources tried in order: imported LRC → sidecar `.lrc` → server lyrics → YouTube (ytmusicapi timestamps) → LRCLIB exact match (title, artist, duration ±2 s).

## 17. Smart shuffle (spread artists)

Group upcoming songs by artist, shuffle groups and their songs, then repeatedly take from the artist with the most remaining (priority queue), swapping with the next if it's the same artist as the previous pick. Result: same-artist songs spread out evenly.

## 18. Queue model reconciliation

`Entries::reconcile(newRows)` walks the new list; for each position, if the row moved it emits `beginMoveRows`, if new `beginInsertRows`, trailing extras removed. Keeps QML delegates alive (no flicker, animations continue) for lists ≤ 512 rows.

## 19. Library load de-duplication

`librarydata::Reader` interns every string and re-uses identical objects with the same `id` (up to 8 variants per id) while parsing JSON, so the same song in queue, history, playlists and versions is one shared `QVariantMap` in memory.

## 20. Saving off the GUI thread safely

The library document is a `QVariantMap` of implicitly shared values. Copy it on the GUI thread (cheap, reference-counted), hand it to a one-thread pool, serialise + `QSaveFile` there. Edits on the GUI thread detach their own copies. One thread guarantees write order. Then `malloc_trim`.

## 21. Folder watching within limits

Helper scan returns visited dirs (≤ 4,096) + audio files (≤ 10,000) as watch paths; scan aborts at 100,000 entries, depth 64, or 1 MiB of paths. Change notifications debounce 1.5 s into a "quiet" rescan; unchanged files skipped by `mtime_ns:size` stamp (+ cover stamp). Missing files only marked unavailable, never removed.

## 22. Cider remote control

Treat an external player as a deck: command → mark "settling" for 3 s (readings that disagree are the command still taking effect) → then Cider's word is final. Interpolate position with a local clock between 1 s polls. Detect "someone picked another song in Cider" and stop following rather than fighting. Turn off Cider's own autoplay while driving and restore it after (flag survives crashes). On quit, a short local event loop waits ≤ 800 ms for the pause to be sent.

## 23. Variable-font poster lyrics without memory blow-up

Each distinct `font.variableAxes` combination is a new FreeType face (~0.65 MiB). Poster lyrics therefore quantise the width axis to 6 levels, give each of 4 word styles one weight/roundness/slant, and animate in two steps (halfway, home), scaling words in between: at most 48 faces however long the song. Word widths are measured once per song with one persistent `Text`.

## 24. Listening statistics

- A play log (one row per play, ≤ 20,000) + per-day totals (`yyyy-MM-dd → [seconds, plays]`, ≤ 3,660 days) so the calendar survives log truncation.
- Rankings by total time, then plays, then name (stable order); top 10.
- Calendar: GitHub layout (column per week, locale's first weekday), 5 levels by **quartiles** of listened days; current streak counts until a full day passes; longest streak over all days.

## 25. Playlist version history

Before every playlist edit, push the previous track list (skip if unchanged). Keep ≤ 12 versions and ≤ 5,000 rows across them. Restoring is itself an edit (snapshotted, undoable).

## 26. Icon rendering: mask once, tint anywhere

`image://symbols/name/points/hex`: pick optical size from points (≤ 22 → 20 px art, > 32 → 40 px art), render SVG once to a premultiplied mask (cached 512 KiB), copy and fill with `CompositionMode_SourceIn` for the ink. Theme changes don't re-parse SVGs.

## 27. Testing technique: the harness inside the app

With `SUNG_DIAGNOSTICS`, the real binary contains test "stages" that drive the real UI: `findItem` by `objectName`, synthesised mouse clicks at real coordinates, key presses, waits on real state, screenshots, frame-sampled motion checks, colour and layout audits. Fixtures stand in for Python helpers (`catalog_fixture.py`), Cider (`cider_fixture.py`), and `parec` (`monitor_fixture.py`). See BUILD_AND_TESTING.md.
