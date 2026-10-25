# SungMobile 0.12.11 (code 31)

[SungMobile-0.12.11-code31-release.apk](SungMobile-0.12.11-code31-release.apk) is the signed, optimized production APK for `com.karthi.sungmobile`. It was installed over code 30 on the connected Android 15 POCO with `adb install -r --no-incremental`; Android reported `Success`. The installed package reports version code 31 and version name 0.12.11. The app launched, remained running, and showed no AndroidRuntime crash in the checked log window. Installation did not clear app data.

This build includes offline embedded/sidecar lyrics for local songs, optional cached LRCLIB lookup, the refreshed Material 3 Blue palette, and bounded recovery for online streams after network errors or a long buffer stall. The earlier Android 15 download compatibility fix remains included. The original locked-screen slowdown has not yet been reproduced or accepted on this build.

- APK size: 8,806,179 bytes (about 8.40 MiB).
- SHA-256: `3a018449c59e78de7c4c0a5fcea0bad0efe77be093068b3dec7deff3e4646188` (also in [SHA256SUMS](SHA256SUMS)).
- Production signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 30.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.12.11`, code 31, minimum API 26. APK signature and zip alignment checks passed.
- 42 playback tests, app unit tests, debug/release lint, and optimized release build passed offline.

No GitHub release or updater manifest was published. Use a version code above 31 for the next production build.
