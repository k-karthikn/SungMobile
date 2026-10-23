# Stage 6 plan — playback convenience

Prepared: 18 October 2026; approved by the owner and initially implemented as `0.7.0-stage6`, code 10. A timer countdown display patch was built as `0.7.1-stage6`, code 11, and the owner confirmed it works on the POCO. **Stage 6 is accepted.** This plan records the agreed scope and defaults; implementation results are in `STAGE6_REPORT.md`. Keep the same application ID, debug signing key, on-device library and manual POCO APK-testing workflow.

## Outcome and boundaries

Add playback speed with preserved pitch, a reliable sleep timer, deliberate resume of long recordings, and named saved sessions containing a queue and its playback position/mode. These are controls on the existing service-owned player, not new players or a second music source. Keep the Material 3 Expressive skeleton screens and standard/reduced-motion behavior. Do not begin downloads, crossfade, autoplay, normalization, visual polish or sign-in; those remain later stages.

The Linux Sung documents remain behavior references. Android Media3 service/session state and the existing Room/DataStore ownership determine the implementation.

## Proposed behavior defaults

1. **Speed:** 1.0× by default; user chooses 0.5–2.0× in 0.1× steps, with a one-tap reset to 1.0×. A **Preserve pitch** switch defaults on and persists with speed; when off, pitch follows speed. The chosen settings persist across songs and app restarts, while the normal media timeline remains the source of seek and lyric timestamps. Speed control belongs to the service and is reflected in Now Playing and controller state. If a format/device cannot honor a setting, show the actual applied value rather than claiming success.
2. **Sleep timer:** presets of 15, 30 and 60 minutes, a custom duration of 1–240 minutes, end of current track, or end of queue. One timer exists at a time; a new timer replaces it and Cancel removes it. It pauses playback without clearing the queue or changing position. A duration timer uses elapsed real time, not media position, so pause/buffering/speed changes do not extend it. The timer runs while the screen is off through the playback service. Do not request an exact-alarm permission for this stage.
3. **Fade:** optional 10-second volume fade only for a duration timer, off by default. It must restore the user's ordinary playback volume when canceled, replaced, skipped or completed, and must not change system volume. If reliable restoration cannot be verified on the POCO, ship the timer without fade and record that limit rather than risk stuck-low volume.
4. **Long-recording resume:** for tracks at least 20 minutes long, save a per-video resume point only after 30 seconds of actual listening. Reopening such a track offers **Resume from mm:ss** or **Start over**; it never silently jumps to a saved position. Do not offer a point within the first 30 seconds or final 30 seconds. Explicit Start over and natural completion clear that point. Private mode suppresses new per-video resume writes; existing queue recovery still works as it does today. Retain at most 100 points for 90 days; never store resolved audio URLs.
5. **Saved sessions:** an explicit **Save session** action captures ordered queue occurrences, current occurrence/position, repeat/shuffle, speed and pitch setting. User names can be edited; cap at 20 sessions and 500 entries each. An oversized queue must be reported, never silently truncated. Saving does not alter playback. Opening a session previews its contents; **Restore** replaces the active queue only after the user chooses it, restores paused at the saved position, and never autoplays. Current queue recovery remains a separate automatic snapshot, not a saved session. Removing a saved session does not affect the active queue, Likes or playlists. Explicit saving is allowed in Private mode because it is a deliberate local action.

## Implementation sequence

| Part | Work | Gate |
| --- | --- | --- |
| 6A. Playback contracts | Add typed speed/timer/session state and commands to `core:playback`; extend `PlayerEngine`, `SessionPlayer`, `PlaybackConnection` and session extras/custom commands only as needed. Keep the service as the sole authority for active speed and timer. | Connected UI and notification controllers observe accurate state; an activity recreation cannot create a second timer or player. |
| 6B. Speed | Apply speed with pitch preservation in `ExoPlayerEngine`, persist the preference in `SettingsRepository`, and expose a small Now Playing control. Test song switch, seek, lyrics timing, focus loss and restart. | Actual speed and displayed value agree; default 1.0× and reset work; no queue or lyric clock regression. |
| 6C. Sleep timer | Isolate timer state machine and deadline calculation from Compose. Bind it to service-owned playback; add a small Now Playing sheet showing mode and remaining time, plus cancel/replace. Add optional fade only after cancellation/volume restoration is tested. | Exact once pause at expiry; end-track/queue modes distinguish natural completion from manual Next, repeat and queue replacement; screen-off works. |
| 6D. Resume/session data | Add bounded per-video resume points and named sessions in `core:data`, keyed by validated YouTube identity and stable queue-entry IDs. If Room tables are added, create an explicit v2→v3 migration, export the schema and test v1→v2→v3 plus v2→v3 upgrades. Preserve the active queue, library, lyrics settings and history. | Restart retains points/sessions; duplicate queue occurrences and order survive round-trip; invalid/oversized data is rejected without touching the current queue. |
| 6E. UI and delivery | Add small resume choice and saved-session list/edit/preview controls in focused feature files. Run pure state-machine, repository/migration, service/controller and relevant UI tests; then build a signed update APK and concise POCO checklist. | Owner manually verifies real audio, screen-off timer, speed, session restore and data retention before Stage 6 acceptance. |

## Failure and lifecycle rules

- Timer state must survive leaving/rotating the activity while the service plays. On a cold process restore, playback stays paused as today; an expired timer must never resume it. Define and test how an unexpired duration timer is restored from a stored deadline or safely canceled, and show the resulting state honestly.
- A timer pause is not Stop: keep queue and position for explicit Play. Cancel any fade and restore normal volume when the timer is removed, when a different track/queue is selected, or when playback stops for another reason.
- End-of-track timer fires on natural completion of the track selected when armed, before the coordinator auto-advances to the next entry; manual Next, selecting another entry or replacing the queue cancels it. End-of-queue timer fires when the active queue naturally finishes; repeat-one/all prevent that condition until disabled or canceled. Queue edits must not fire a stale timer.
- Resume points use canonical video IDs so duplicate playlist entries share the recording point; saved sessions preserve occurrence IDs so duplicates remain distinct. Do not derive resume data from search/listening history, and do not let Private mode write it indirectly.
- Saved-session restore validates every entry before replacing the live queue. If a later YouTube resolution fails, retain the restored queue and show the existing retry/skip state. Saving a session never persists signed stream URLs or provider continuation tokens.

## POCO acceptance proposal

1. Install the Stage 6 APK over accepted code 9 without uninstalling; confirm Likes, playlists, lyrics settings/offsets and the current queue remain.
2. Change speed during playback, switch songs, seek and open synced lyrics. Check pitch, displayed speed and persisted reset after app restart.
3. Run a short custom sleep timer, then repeat with the screen off. Replace/cancel it; verify only the current timer pauses playback and volume is normal afterward. Check end-of-track and end-of-queue modes with manual Next and repeat on/off.
4. Play a recording over 20 minutes, leave and reopen it, choose Resume and Start over separately, then test completion and Private mode behavior.
5. Save a session with duplicate songs, queue edits, repeat/shuffle and a nonzero position; restore it after switching to another queue. Verify it is paused at the expected entry/position, no surprise autoplay occurs, and deleting the saved session leaves the active queue/library intact.

Record phone findings before marking Stage 6 accepted. The implementation follows these defaults except that duration timers are safely canceled on cold process restore, which also restores the queue paused.
