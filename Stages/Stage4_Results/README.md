# Sung Stage 4 results

**18 October 2026 follow-up:** The owner found that the mini-player was hidden on album and playlist detail pages. [Sung-0.5.1-stage4-debug.apk](Sung-0.5.1-stage4-debug.apk) (code 7) shows it there. Its SHA-256 is `a7758f47d9680ab6f25e4e1dcc13571e98861673189962cb98865cdaf4caef84`. `:app:assembleDebug` passed on the restored OS; the owner subsequently reported **Stage 4 working fine**. This APK uses a **new debug signing key** (certificate SHA-256 `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`). It cannot update the old-key code 6 installation. The owner agreed to uninstall/reinstall and lose the old local app data. Keep the new `~/.android/debug.keystore` for future updates. The verification counts below describe the original code 6 build, not the code 7 follow-up.

Delivered: **14 October 2026**. Status: **host verified; phone acceptance pending**.

- Update APK: [Sung-0.5.0-stage4-debug.apk](Sung-0.5.0-stage4-debug.apk)
- Version: `0.5.0-stage4` (code 6), app ID `com.karthi.sungmobile.stage0`.
- SHA-256: `d2d3e5da9240fb7b4947903d9c77eef8e9cd633f98e8331fb1e1a6f3fa84831c`.
- Signing certificate matches Stage 3; install as an update without uninstalling or clearing data.
- **124 host tests passed**, five opt-in network tests skipped in that suite; **all five live network tests passed separately**. No failures.
- Lint: **0 errors, 13 dependency-version notices**. APK signature verified.

Use [PHONE_TEST_CHECKLIST.md](PHONE_TEST_CHECKLIST.md) for the POCO checks. See [the implementation report](../../docs/STAGE4_REPORT.md) and [code guide](../../docs/PLAYBACK_GUIDE.md).

## Live capability matrix

Checked 14 October 2026, English localization and India content region. These results are a dated sample, not a guarantee for every YouTube item.

| Capability | Result |
| --- | --- |
| Song search and continuation | Passed. |
| On-demand audio resolution and real byte reads | Passed for two tracks, WebM audio, reported 160 kbps. |
| Album details via search-returned playlist URL | Passed; A Sky Full of Stars sample returned one track. |
| Playlist details and partial content | Passed; sampled playlist returned 40 entries against 45 reported, with partial flag. |
| Long-playlist continuation | Passed; first 101 entries, next 15, provider total 120. |
| Artist search | Passed. |
| Artist discography | **Limited**: sampled Topic channels had no browseable music tab. Labelled fallback song searches passed. Channel videos are not called discography. |
| Direct Music album browse IDs | **Unsupported**: explicit message; use album search or playlist links. |
| Anonymous online Home feed | **Not implemented/verified**. Home uses actual local Likes, pins and history. |
| Android cold/warm Share delivery, physical rotation and background playback | **Phone checklist pending**; host intake/state-restoration/controller tests passed. |

## Evidence

- `build.log`: full host build/test/lint result. A fresh dependency/runtime download was needed after resuming on 14 October; final build succeeded.
- `provider-unit-tests.log`: final provider unit results with all five network probes explicitly skipped.
- `live-provider.log` and `live-provider-results.xml`: separate opt-in live run and captured capability output.
- `host-test-counts.json`, `build-summary.json`, `lint-results.xml`, `apk-signature.txt`, `SHA256SUMS`: exact counts and package checks.

Existing Stage 3 files/APK remain available. No phone installation or device acceptance is claimed for Stage 4.
