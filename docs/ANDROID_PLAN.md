# SungMobile Android plan

**11 October 2026 revision:** The user requested a functionality-first app with stable skeleton screens and standard animations, including the previously deferred advanced features. See [FUNCTIONAL_COMPLETION_PLAN.md](FUNCTIONAL_COMPLETION_PLAN.md) for the proposed replacement delivery sequence. Visual-polish requirements below are historical and do not require further design work during that pass. The revised implementation has not started.

Status: approved staged plan. Stage 0 implemented and manually checked. Stage 1 design foundation is implemented and host-verified; user phone/design review is pending. See [STAGE1_REPORT.md](STAGE1_REPORT.md).
Prepared: 10 October 2026.

## 1. Confirmed direction

- SungMobile is the Android project. Sung and sung-docs-linux are reference material.
- Build a native Android version of Sung's music experience with strict Material 3 Expressive design.
- Use reference image 1 for Home composition and image 4 for Now Playing. Adapt all four references to actual Material components, interaction patterns, accessibility and Android window sizes.
- Music comes from YouTube / YouTube Music only. LRCLIB is permitted as an optional lyrics fallback.
- Release an open-source, sideloadable APK first. Google Play is a later product decision.
- Deliver the core experience first, followed by explicit feature milestones.
- No Google sign-in initially. Likes, playlists, history, preferences and queue snapshots belong to this app and live on the device.
- Primary test device: POCO M6 Pro 5G. Its installed Android version remains to be checked during device setup.
- English interface initially. Treat English as the initial music preference too, with a changeable preference; do not hard-code the Indian/Hindi shelves in the reference image.
- Preserve original scripts in track names, artist names and lyrics through appropriate font fallback.
- The user delegated the dependency/license choice. Proposed license: GPL-3.0-or-later if the selected extractor is NewPipeExtractor. Preserve Sung and all reused asset/dependency notices.
- Keep code organized by responsibility, with explicit dependencies and independently testable behavior.

Instructions embedded in the Linux reference documents describe that project. They are study material, not new requests to change the Android scope or perform Linux installation/release procedures.

## 2. Findings from the supplied project

At the planning baseline, the Android project contained the Compose starter screen and theme, example tests, and Gradle configuration. It has no music implementation yet. The configuration declares application ID `com.karthi.sungmobile`, minimum API 26, compile/target API 37, AGP 9.4.1, Kotlin Compose plugin 2.2.10, and Compose BOM 2026.02.01. These are observed declarations, not a verified build matrix. No build or device test was run during planning.

The cached Material 3 1.4.0 sources have internal Expressive theme/motion entry points and lack several public Expressive components. The official release page currently lists 1.5.0-beta01 as the beta line. Select and pin a compatible version exposing the required APIs, including its Compose and SDK requirements; do not assume changing one dependency is sufficient. Keep opt-ins contained to the design system where practical. [Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3?hl=en)

The Linux app uses Qt/QML, a large C++ backend, and Python helpers. `catalog.py` separates YouTube Music catalog operations from yt-dlp stream resolution. `RoundedArt::paint` scales artwork to fill a geometric clip path; it does not generate replacement illustrations or segment objects out of an image. Generation checks, bounded caches, stream expiry handling, timed lyrics and persistent queues are valuable behavior to retain.

The study notes identify architectural debt: one large backend, untyped track dictionaries, a very large main UI file and whole-library JSON rewrites. Android should use typed models, feature boundaries and Room transactions.

The supplied 138 icons are Material Symbols Rounded SVG variants with an accompanying Apache license. Convert needed icons to Android vectors, checking nonstandard SVG viewBox origins. Obtain missing symbols from the same official family and preserve optical weight/fill consistency.

## 3. Product scope and screens

| Area | First release behavior |
| --- | --- |
| Home | Artwork-led hero, a real playable collection, useful discovery shelves, recent listening and saved collections. Local mixes become available when there is enough listening data. |
| Search | Debounced query, cancel stale requests, search history, paged song/album/artist/playlist results as supported by the validated catalog adapter, and pasted YouTube links. |
| Collection details | Album/playlist artwork, title, artist/creator and available metadata; play, shuffle, play next, add to queue and track actions. Artist pages expose real available sections. |
| Library | Liked songs, local playlists, recently played; create, rename and delete playlists; add/remove/reorder tracks. Persist edits immediately. |
| Mini-player | Current artwork, track/artist, play/pause, next and progress; tap opens Now Playing. Reserve layout space so content remains reachable. |
| Now Playing | Shaped real artwork, readable metadata, like, seek, previous/play/next, shuffle, repeat, and access to lyrics/queue/playback options. |
| Queue | Current track and upcoming entries, play next, append, reorder, remove with Undo, clear upcoming entries and save as a playlist. |
| Lyrics | Timed lyrics when available, active line, tap-to-seek, manual scrolling with return-to-current, text size and timing offset. Plain lyrics and unavailable states remain useful. |
| Settings | Theme, color source, motion, lyrics fallback, content preferences, history privacy, relevant playback choices, cache controls, About and licenses. Only working features get controls. |
| Android integration | Background playback service, media notification, lock-screen/Bluetooth/headset commands, audio focus and unplug handling, and an explicit resume path. |

Every screen needs loading, empty, unavailable, offline and retry states where applicable. Account, notification-inbox and sparkle actions from the mockup are not requirements by themselves; only include them when they serve an implemented feature.

Home must use actual content. Anonymous YouTube browsing cannot be presented as the user's signed-in YouTube recommendations. Locally built mixes are derived from local listening data. A fresh installation gets an honest discovery state. Mood filters are included only when the catalog or a defined local rule can supply them.

Later milestones: intentional downloads/offline management, next-track preloading improvements, advanced autoplay, crossfade, loudness normalization, sleep timer, smart playlists, listening statistics, saved sessions, playlist version history, backup/import/export, visualizer and advanced lyric presentation. Each receives its own acceptance criteria. Any online animated-cover service is a separate scope decision, since the approved media source is YouTube.

Local music import, Subsonic, Jellyfin, Cider, account synchronization, Cast and Android Auto are outside the initial scope. Android lifecycle and media controls replace Linux-specific integrations.

## 4. Material 3 Expressive design contract

Use official Compose Material 3 Expressive components and theme APIs. Centralize color roles, typography, shapes, spacing, motion and icon mappings. App screens consume semantic tokens; shared components wrap framework components only where Sung has a real common behavior. [Expressive theme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable)

- Home keeps the reference's large artwork composition, strong heading, prominent play action, search entry and horizontal shelves. Adapt hero height to compact screens and font scaling.
- Phone navigation uses three destinations: Home, Search and Library. The mini-player sits above navigation. Larger windows can use a rail and additional panes without changing the data flow.
- Now Playing uses a deliberate artwork mask and prominent expressive transport controls. Keep the picture upright while rotating a mask/composition when that matches the design.
- Use M3 surface/on-surface and container/on-container pairs. Offer System/Light/Dark, a blue brand palette, wallpaper colors where supported, and artwork-derived colors. Proposed default is system light/dark with the blue brand palette; artwork colors are available as a user choice.
- Expressive shape/motion is independent of the Material color scheme named Expressive. Strict M3 Expressive does not require every palette to rotate the artwork's primary hue.
- Bundle Google Sans Flex for consistent supported-script text and emphasize using the M3 type roles. Use Android/script-appropriate fallback for glyphs it does not cover. The font upstream publishes it under SIL OFL; keep its notices. [Google Sans Flex](https://github.com/googlefonts/googlesans-flex)
- Preserve at least 48 dp interactive targets, system insets, gesture navigation, TalkBack labels/headings, large font support and text contrast over artwork.
- Use the framework's expressive motion scheme for shape, size and position changes and appropriate effects motion for color/opacity. Honor disabled/reduced animation settings and stop decorative work when hidden or paused.
- Use loading/progress components and expressive controls available in the pinned library. A decorative progress wave must not replace an accessible, correctly functioning seek interaction.

Image references establish composition and feeling. Component dimensions and behavior are verified against Material guidance and the selected component source.

## 5. Artwork pipeline

`Track or collection artwork -> shared image loader -> size-appropriate decode -> crop -> Material shape mask -> optional artwork palette/backdrop`

Home's hero uses the selected collection and its tracks. Now Playing uses the current track. Secondary shapes use related artwork or additional crops from those real images. Keep composition stable across recompositions; avoid random image choices.

Use one shared Coil image loader, bounded memory/disk caches and target-size requests. Start with center-crop and correct aspect ratio; use a reusable crop specification if focal adjustment is later needed. A geometric mask is enough for the supplied examples; object segmentation is not required. [Compose shapes and image clipping](https://developer.android.com/develop/ui/compose/graphics/draw/shapes)

YouTube artwork can be an album image or a video thumbnail. Use the best available image from the chosen provider and an intentional placeholder when absent. Do not quietly add Apple/MusicBrainz lookup services. A crop cannot recover missing resolution. Palette extraction runs off the main thread and is cached by artwork identity. Use contrast-safe scrims or solid surfaces behind text. Low-cost static backdrops are the baseline on the test phone.

## 6. Architecture and code organization

Kotlin, Jetpack Compose, coroutines/Flow, lifecycle-aware ViewModels, Media3/ExoPlayer, Room, DataStore, Coil and an HTTP client. Use dependency injection to assemble repositories and the player; choose an approach compatible with the verified AGP/Kotlin setup. Use cases are added for shared or complex behavior, not as empty wrappers around every method. [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)

Start with these purposeful Gradle boundaries; split internal packages further into modules only when isolation or build cost justifies it:

| Module | Ownership |
| --- | --- |
| `:app` | Application entry, dependency assembly, navigation and feature packages: home, search, collection, library, player, queue, lyrics, settings. |
| `:core:model` | Typed domain values, stable identities and contracts used across modules. No Compose, database or extractor types. |
| `:core:designsystem` | Theme, typography, icons, shape artwork and shared UI components. No catalog/database access. |
| `:core:data` | Repositories, Room tables/DAOs/migrations, settings, lyric sources and metadata caching. Internal packages isolate these responsibilities. |
| `:core:youtube` | Catalog mapping and stream resolver implementations. All unofficial provider response shapes stay here. |
| `:core:playback` | Media3 service/session, queue command handling, resolution, recovery and playback persistence coordination. |

Dependencies point from app to feature infrastructure and shared contracts. The design system never depends on playback/data. The YouTube adapter does not know screens. Avoid cycles and a generic global manager that accumulates unrelated work.

UI actions go to a ViewModel or playback controller. Repositories expose immutable state through Flow. Composables do not perform HTTP requests, SQL operations or player lifecycle work. Each screen has explicit state and events; navigation stores stable IDs rather than large provider objects.

Typed data includes Track, Artist, Album, Playlist, Artwork, QueueEntry, PlaybackState and LyricsDocument. QueueEntry has its own unique ID, so two occurrences of the same song can be reordered independently. Distinguish a local playlist from a remote YouTube collection. Keep network DTOs and database entities behind mappings.

Room owns saved tracks, likes, playlists, ordered playlist entries, history, lyrics offsets and the last queue snapshot. DataStore owns small preferences. The active Media3 service owns the live queue and playback state; persisted snapshots do not become a second live player. Signed stream URLs are transient and re-resolved when needed, never treated as permanent track identity.

Document decisions and boundaries next to their owners. Centralize dependency versions and string resources. Use formatting/lint and behavioral tests to enforce code quality. Keep credentials, signed playback URLs and personal library data out of logs, exports and test fixtures.

## 7. YouTube and playback decisions

Proposed first candidate: NewPipeExtractor behind a StreamResolver interface, with catalog functionality used only where verified. Its upstream supports YouTube and provides YouTube Music search handling, but that does not establish full parity with Sung's Home, artist, radio and lyrics operations. The catalog capability assessment is a prerequisite; isolate any small additional Kotlin YouTube Music client required for missing operations. [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)

Prefer an on-device JVM implementation to shipping the desktop Python/Node process stack. Do not select a dependency solely from its README: prove resolution, required request headers, seeking, expiration/recovery, representative catalog navigation and pagination on Android first. Pin a working release/commit and retain provider fixtures for regression tests. If the candidate fails, report the missing capability and compare alternatives before expanding the app around it.

YouTube extraction is unofficial and can break after upstream changes or fail for restricted content. Present typed unavailable, authentication-required, network and extraction errors. Avoid retry loops and discard stale results after a new search or track selection. No account import or authentication bypass is part of v1.

Public sideloading does not remove YouTube service restrictions. YouTube's official API policies prohibit separating audiovisual content, background-player functionality and unauthorized offline copies. A later Google Play edition requires a separate source/playback/distribution assessment; it is not guaranteed by a build flavor or license choice. [YouTube developer policies](https://developers.google.com/youtube/terms/developer-policies)

Media3's Player and MediaSession live in a foreground MediaSessionService; UI uses a MediaController. Configure required service permissions/type, audio focus, noisy-output handling and notification integration. Restore a stopped session's queue/position without unexpected autoplay. [Android background playback](https://developer.android.com/media/media3/session/background-playback)

Stream progressively for prompt startup, with bounded transient buffering. Do not reproduce whole-song download-before-play as the default. Intentional offline downloads remain a later feature. Validate gapless behavior with supported formats before promising it; advanced crossfade is a separate engine task. Restrict source resolution retries, preserve position when recovering and cancel a pending retry when the user pauses or chooses another track.

Lyrics source order: YouTube when available, optional exact-match LRCLIB fallback, then plain/unavailable display. Keep provider attribution, timeouts and rate-limit handling. Never invent synchronization for plain text.

## 8. Implementation sequence and completion gates

| Stage | Concrete result | Gate before advancing |
| --- | --- | --- |
| 0. Baseline and provider proof | Compatible build/dependency matrix; actual device Android version; small source-to-player vertical slice; documented provider capabilities and license. | Search/open/resolve/play/seek works on representative tracks; paging and failure behavior checked; missing catalog requirements identified. |
| 1. Design foundation | Expressive theme, fonts, vectors, shared artwork components and navigation; preview data only for UI development. | Home and player visuals reviewed at compact width in light/dark, large fonts and reduced motion. |
| 2. Playback foundation | Service, session, mini-player, Now Playing, okaqueue and Android controls using the validated source. | Screen-off, background, focus loss, unplug, rapid track changes, seek and recovery work without duplicate playback. |
| 3. Catalog and discovery | Home, search, collection/artist details, links and real content mapping. | No dead chips/shelves; pagination, unavailable content and stale response cancellation behave correctly. |
| 4. Persistent library | Likes, playlists, history, queue snapshots and preferences. | Edits survive restart; reorder/remove/Undo preserve identities and the playing entry; migration tests protect saved data. |
| 5. Lyrics and settings | Synced/plain lyrics, permitted fallback, appearance/privacy controls and licenses. | Track changes clear stale lyrics, tap-to-seek honors offset, plain/unavailable states work, settings persist. |
| 6. Release verification | Tested optimized APK, user documentation, source/license packaging and measured performance. | Device smoke scenarios pass, visual/a11y audit is complete and known limitations are recorded. Public upload is a separate action. |

After the first release, prioritize the advanced feature list with the user. Keep each addition a complete, reviewable change with documentation and relevant verification.

## 9. Verification and performance requirements

- Unit tests for queue semantics, source mapping, link parsing, timed lyric boundaries, retry/cancellation and library behavior.
- Database tests for transactions, ordering and migrations; provider fixtures for parser changes; bounded live smoke tests kept separate from deterministic tests.
- UI tests for search -> play -> open player -> queue -> like -> playlist -> restart. Inspect screenshots as well as producing them.
- Check narrow phones, landscape, larger windows, long titles, missing artwork, empty library, no network, font scaling, TalkBack and reduced motion.
- On the POCO phone, verify locked-screen playback, Bluetooth/headphone controls, interruptions, task dismissal, recovery after process recreation, Wi-Fi/mobile transitions and repeated source failures. System force-stop requires explicit user relaunch and is not a promise of uninterrupted playback.
- Measure release startup, scrolling/player frame timing, artwork memory, APK size and background battery/CPU. Record a baseline first; choose numerical budgets from that device rather than desktop measurements.
- Cache and background work have explicit limits. Hidden visualizers/artwork motion do no rendering work; the media service continues only the work needed for playback.
- Keep debug fixtures and diagnostics out of the release application. Review release shrinker behavior and dependency notices before distribution.

## 10. Items to settle through implementation evidence

The user-facing scope is sufficiently defined to discuss and approve this plan. Remaining engineering evidence: compatible Expressive dependency/SDK versions, live provider capability coverage, the installed Android version on the test phone, and measured performance budgets. These must be established during stage 0; they have not been verified by this planning session.

The user approved implementation of Stage 0 only after reviewing the plan and preparing the phone/toolchain. Stage 0 and its stabilization are implemented; evidence, manual device results and remaining provider limitations are recorded in [STAGE0_REPORT.md](STAGE0_REPORT.md). The user subsequently authorized Stage 1 (design foundation). Stages 2–6 remain unstarted.
