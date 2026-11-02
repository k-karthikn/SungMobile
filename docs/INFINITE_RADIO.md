# Infinite Radio

## Reference and design

LastWave's feed uses taste and listening data to choose radio material, then its playback service extends the queue with YouTube Music radio responses and continuation tokens. The reference hero uses supplied song artwork as a faded background, a dark overlay, a short “Made for you” label, and a prominent play action.

SungMobile builds a ranked “For you” shelf from provider recommendations, liked/saved songs, top and recent listening, related tracks, and recommendation feedback. Related-song expansion selects up to four distinct seeds across “More,” likes, top tracks, recent plays, and saved tracks. Seed requests run concurrently with a bounded five-track response per seed; results are published as each request completes. The controller reuses results for 20 minutes, refreshing immediately for new explicit likes/saves or “More” feedback and dropping seeds that are no longer allowed. Ordinary listening updates do not cancel an active expansion. Infinite Radio reuses the ranked shelf as its starter selection. If that shelf is unavailable, it uses local liked/recent songs only when recommendations are enabled and private mode is off, then falls back to provider Home songs. The “Made for you” label is shown only when the ranked personal shelf supplies tracks.

The hero uses the first selected track's supplied cover, a blur, and a dark vertical fade to preserve text contrast. The layout uses SungMobile's Material theme colors and a large, accessible Play button instead of copying LastWave's exact visual styling.

## Personalization behavior

- Seed priority rotates through one explicit “More” track, a liked track, a top-played track, a recent track, then a saved-playlist track. Remaining slots are filled from the same signals, up to four seeds total.
- Related results from explicit “More” (80) and liked (60) seeds receive the strongest source boosts, followed by top-played (48), recent (40), saved-playlist (32), and provider-only (15) candidates. A song found from multiple independent sources gets up to 16 additional points. Hidden songs are removed from the ranked shelf.
- Exact liked, top-played, and recent songs receive familiarity penalties so related discoveries can rank ahead of repeats. Recent penalties decay with the event's position in listening history.
- Candidate variety is bounded to three items per seed before deferred candidates fill any remaining shelf slots. Duplicate song IDs are removed before ranking and queue extension.
- Saved playlists can activate personalization when they are the user's only available taste signal. Listening-derived signals are ignored in private mode or when listening history is disabled.

## Playback behavior

- Starting the hero starts a new radio queue with up to 20 ranked starter songs.
- Radio mode is carried across the media-session boundary on the first queue item and owned by the playback service.
- When three or fewer queued songs remain, playback requests up to five related songs and appends candidates that are not already in the queue. This gives the player time to fetch before the queue ends.
- The player makes at most one related request for a queue entry, so an empty response cannot trigger repeated requests on every playback-state update. It tries a new seed as the queue advances.
- If playback reaches the end before the related response arrives, the first usable result starts playback when it is appended.
- Radio continuation works independently of the general Related autoplay setting. Replacing the queue with an ordinary selection or stopping playback ends radio mode.
- The queue uses SungMobile's `RelatedSource` (`YouTubeSource.related`) for continuation. Unlike LastWave, this implementation does not port YouTube Music's native radio endpoint, automix playlist ID, or continuation tokens. A failed or empty related response can end the station.
- Process restoration restores the saved queue, but does not restore radio mode; the user can start the station again from Home.
- If every related response is empty or fails, the queue can still end; no replacement track is fabricated from a search query.

## Verification

Source changes were made, but compilation and device playback have not been verified in this environment. Confirm that Home hides the hero when it has no playable songs, respects private mode and the recommendations setting, starts with the displayed personal picks, fetches before the queue ends, avoids duplicates, and stops radio mode when another queue is started. Also verify text contrast with light/dark themes, large font scale, missing artwork, and reduced motion.
