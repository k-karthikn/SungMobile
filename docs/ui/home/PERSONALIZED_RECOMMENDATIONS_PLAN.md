# Plan: personalized Home recommendations

Status: **implemented, 27 October 2026**. This records the decisions and design rationale; see [the implementation report](PERSONALIZED_RECOMMENDATIONS_REPORT.md) for the delivered behavior and verification. The plan used the user's AI-generated proposal as input, but the decisions below came from SungMobile's code and the user's answers.

## Decisions confirmed with the user

| Question | Decision |
| --- | --- |
| When should `For you` appear? | Automatically when usable likes or permitted listening history exist. Include an off switch in Settings. |
| What happens in Private mode? | Hide `For you` immediately. Keep the ordinary online Home rows. |
| Can the feature request more music? | Yes. It need not rank only the five current Home rows. |
| Can a liked song be used in a related-song request? | Yes. SungMobile may send that song's ID to YouTube to retrieve related songs. The ranking and the rest of the local profile stay on the device. |

## What exists today

- The live Home is `DiscoveryHomeScreen` with `DiscoveryFeedViewModel`. It shows a provider-backed feed from `MusicBrowseGateway`, normally up to five mixed song/album/playlist rows. `HomeViewModel` and `RoomHomeRepository` serve Library mixes; they are not the active discovery feed.
- Home's existing refresh has a 20-minute freshness window and retains the last good rows after a refresh failure. A recommendation request must follow this lifecycle, not run from a Compose card or on every recomposition.
- `RelatedSource.related(seed, limit)` already exists, backed by NewPipe extraction of a song's related items, currently capped at five per call. It is also used by playback's related autoplay, but Home recommendations must have their own bounded fetch state and must not change autoplay or the queue.
- Likes, saved playlists, and qualified listening events already live in Room. `HistoryPolicy` controls Private mode and listening-history recording; clearing history increments an epoch. The statistics read is bounded by its `limit`, but the smart-playlist `candidates()` query is unbounded and should not become a Home refresh dependency.
- `CatalogItem.id` is not identical across Home and search sources. Canonical `libraryKeyOrNull()` (`youtube:<videoId>`) is the appropriate key for playable-song matching and deduplication. `subtitle` is not consistently a music artist, so it is unsafe as a universal artist signal.
- The Settings screen currently hosts several sections in `AppearanceScreen`; the new setting should be its own small section/component. The in-progress import/export format also needs to account for any new preference and feedback without breaking old backups.

## Product behavior

1. Add one **For you** song shelf between the mood chips and the ordinary provider shelves. Reuse the existing carousel, artwork, caption, accessibility and song-tap playback path. A song tap starts playback in the `For you` song context. Do not add a play overlay to each card or change album/playlist taps in the ordinary shelves.
2. **Preserve the entire existing Sung-inspired Home implementation and all current Home content.** Keep every provider shelf, item, title and order; retain mood chips, card tap routing, carousel behavior, artwork, refresh behavior and fallback messages. `For you` is only an additive row. Do not delete or replace the current Home source, UI or its existing tests. `For you` contains only playable, validated songs; do not copy an album/playlist card into it or fabricate songs to fill it.
3. Show the shelf only when personalization is on, Private mode is off, there is a usable local signal, and enough distinct candidates are available. If the user has no likes/history or there are no usable fetched or cached candidates, keep the normal Home page without an empty `For you` placeholder.
4. Add a concise **Personalized Home** switch in Settings, on by default. Its explanation must disclose that related-song lookup can send individual liked-song IDs to YouTube. The switch off state makes no recommendation-specific requests and shows only the normal provider rows. Turning it back on can use existing eligible local data.
5. Provide limited feedback on `For you` songs through a contextual menu: **More from this song** and **Hide from For you**. These names describe the behavior more accurately than the proposal's broad “More/Less like this” promise. The primary card remains a play target. Feedback is stored locally and can be reversed or cleared in Settings; do not add an extra visible button to every artwork card.

## Candidate fetching and ranking

- Start with playable songs from the ordinary Home response. Add related songs from **a small, rotating selection of liked online songs** using the existing `RelatedSource`. Fetch only during a Home refresh or when a locally cached candidate pool has genuinely expired; bound concurrent requests, timeouts, result count and seed count. Do not issue one request for every like or history event. Initial caps should be conservative (for example, at most two seed requests and five related songs per seed), then tuned with device/network testing.
- If the user has listening history but no likes, fetch additional **general Home** candidates if the provider exposes them; rank those locally. Do not use history-track IDs as related-request seeds in this first version. This limits what is disclosed to the provider under the permission the user gave. If general expansion has no extra songs, the existing Home songs can still form the shelf when there are enough distinct candidates.
- Filter invalid/non-song results, deduplicate by canonical song key, and retain the original metadata/artwork. Avoid exact repeats of the seed track and recently heard tracks where possible. Do not persist provider results as an additional copy of listening history.
- Compute a small local profile from bounded likes, qualified listening totals/recent events when listening history is enabled, and a bounded sample of saved playlist entries as a weaker explicit signal. A mere song start or skip is not a dislike. When listening history is disabled, ignore old stored events even though they still exist. If history is cleared, invalidate its contribution immediately using the policy epoch.
- Make ranking a deterministic pure-Kotlin function. Prioritize related-source relevance and explicit positive signals, apply a recent-repeat penalty and explicit hide filter, and keep a little artist/source variety. Use artist matching only where metadata can be verified; otherwise do not infer genre or taste from arbitrary titles or channel names. Do not adopt the AI proposal's numerical weights as if they were validated. Treat them as a test hypothesis and tune after actual results are inspected.
- Re-rank locally when likes, playlists, history policy, listening statistics, or feedback change. A local re-rank should not trigger a new network fetch. Network error in related lookup should fall back to usable Home candidates without blocking the ordinary feed.

## Privacy and data handling

The ranking profile, likes, playlists, history, and feedback remain local. **Related lookup is not completely private:** YouTube receives each liked song ID selected as a seed, along with normal network request metadata. Do not claim that no preference-related information leaves the phone or that this is anonymous listening. Do not send a complete library, history list, score vector, or stable app-generated user identifier.

Private mode hides the shelf, prevents new recommendation-specific fetches, ignores any in-flight results, and discards the in-memory personalized shelf while it is active. No feedback is saved during Private mode. The ordinary generic Home fetch may continue. When listening history is turned off, likes and manually saved playlist entries may still contribute, but old listening events must not. Clearing listening history removes its effect immediately; clearing recommendation feedback removes its own effect. Turning personalization off should also clear the in-memory derived profile and candidate pool, while preserving the user's underlying likes/history.

Feedback can be stored under canonical song identity with one current action. The implementation uses bounded DataStore settings for this small map, avoiding a new Room table or database migration. The switch and feedback are included in the backward-compatible backup format, with absent fields defaulting safely for older backups.

## File responsibilities for implementation

| Area | Responsibility |
| --- | --- |
| `core/model/.../discovery/RecommendationModels.kt`, `PersonalizedFeedRanker.kt` | Small inputs/results and pure deterministic ranking; no Android, Room, NewPipe or Compose calls. |
| `core/data/.../recommendation/` and focused DAO/entity/migration files | Bounded local profile queries and feedback persistence; no UI logic. |
| `core/youtube` existing `RelatedSource` and Home gateway | Provider requests only; add a bounded general-candidate path only if its endpoint provides one. No local preference profile is serialized into requests. |
| `feature/home/DiscoveryFeedViewModel.kt` plus a focused recommendation coordinator/state file | Combine provider candidates, local signals and settings; control refresh, cancellation, stale results and recomputation outside Compose. |
| `feature/home/PersonalizedShelf...` | Reuse Home card presentation and add a contextual feedback affordance without growing `DiscoveryHomeScreen` into a data/network controller. |
| Separate Settings section and `SettingsRepository` preference | Opt-out control and honest explanation; keep `AppearanceScreen` wiring small. |

Keep each concern in a readable file rather than packing the feature into the Home screen or ViewModel. Reuse existing interfaces where practical; the table gives responsibilities, not a requirement to create every named file.

## Implementation sequence and verification

1. Define eligible signals, candidate identities and the pure ranker. Test no-preference fallback, duplicate removal, related seed ordering, recent-repeat penalty, explicit hide, metadata gaps, diversity and stable output.
2. Add bounded profile reads, the personalization setting and feedback storage with a non-destructive migration. Test Private mode, disabled listening history, clear-history epoch, feedback clearing and older backup imports.
3. Add the related/general candidate fetch coordinator, with strict request caps, cancellation, refresh caching and failure fallback. Test that turning personalization off or Private mode on stops related requests and that a local feedback/like change only re-ranks cached candidates.
4. Add the Home shelf and Settings UI, preserving all original rows and the existing card tap behavior. Check narrow/large-font layouts, screen-reader labels, dark/light themes and reduced-motion behavior.
5. Run focused model/data/ViewModel/UI tests and manual device checks for first load, tab return, manual refresh, offline return, slow network, Private mode, and history clear. Build an APK only when implementation is complete and the user requests one.

Acceptance check: with `Personalized Home` off, the visible Home content and behavior must match the current implementation. With it on, the only Home addition is the optional `For you` shelf; all pre-existing rows and actions continue to work.

## Intentional differences from the pasted proposal

- Extra related-song requests are **in scope now** because the user chose them; the proposal treated them as a later possibility.
- The absolute “no local preference sent to a provider” claim is revised: selected liked-song IDs are disclosed for related lookup. The full profile and ranking remain local.
- `HomeRepository` is not the active discovery Home source; integrating solely there would not display anything on today's Home screen.
- Raw `CatalogItem.id`, unverified subtitle-as-artist assumptions, an unbounded smart-playlist scan, and the proposal's sample score weights are not safe implementation shortcuts.
- Feedback labels and rules should describe exactly what the app can do with its available metadata, rather than promising broad musical similarity it cannot yet infer.
