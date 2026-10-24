# Player and swipe-up Queue — 0.12.4 (code 24)

`SungMobile-0.12.4-code24-release.apk` is the production candidate for `com.karthi.sungmobile`. It is signed with the same production certificate as code 23, so it can update that installation without deleting its local data. The older `.stage0` debug app remains separate. Nothing was installed on a device or published to GitHub in this task.

The player retains its real previous/current/next queue artwork, song library actions, Lyrics, speed, sleep timer, and More options. Its wave seek track is slimmer; small −10/+10 controls sit beside the time labels. Repeat, Previous, Play/Pause, Next and Shuffle form the main transport row. Queue is now a persistent Material 3 sheet: swipe or tap its handle to expand, swipe down, tap the handle again or press Back to collapse. Expanded Queue retains occurrence-specific play, move, remove, Undo, clear-upcoming and save-to-playlist actions.

Host validation passed: 23 player/design tests, release lint with zero errors (22 warnings), R8 minification/resource optimization, APK package/version checks, checksum and v2 signature verification. The APK is 8,704,483 bytes. Its signing certificate SHA-256 is `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 23. Details are in `build.log`, `lint-build.log`, `lint-results-release.sarif`, `signature.txt`, `SHA256SUMS` and `update.json`. The proposed update tag is `v0.12.4`; regenerate metadata if the tag or APK changes. Phone acceptance remains pending.

[Manual phone checklist](PHONE_TEST_CHECKLIST.md) · [Implementation report](../../docs/ui/player/QUEUE_SHEET_REPORT.md) · [Versioning and signing guide](../../docs/updates/VERSIONING_AND_RELEASES.md)
