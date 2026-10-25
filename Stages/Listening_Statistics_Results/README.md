# SungMobile 0.14.0 (code 34)

[SungMobile-0.14.0-code34-release.apk](SungMobile-0.14.0-code34-release.apk) is the signed, optimized production APK for `com.karthi.sungmobile`. It upgrades code 33 in place using the same production signing certificate. It has not been installed on a phone or published.

Listening statistics now opens from a prominent Home card into a dedicated Material 3 dashboard. The page shows total qualified listening time and plays, unique songs, average time per play, a 7/30-day activity graph, top songs, and recent qualified listening. The three metrics use compact horizontal cards with distinct theme-colored icon shapes. The page has no playback or song-menu controls, and the mini-player/navigation bar is hidden while it is open. This final 0.14.0 build also includes the compact Settings redesign, Search result overflow menu, and centered active lyric line.

- Size: 8,870,855 bytes (about 8.46 MiB).
- SHA-256: `3319eeb78742106505c17911d407045cbc9dbbdb3d797fcd5d1e9b9a10621b6a` (also in [SHA256SUMS](SHA256SUMS)).
- Production signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 33.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.14.0`, code 34, minimum API 26. APK signature and zip alignment checks passed.
- Data/provider/app unit tests, release lint, and release assembly passed. Normal and enlarged dark-mode statistics renders were inspected; UI tests verify the mini-player and bottom navigation disappear on this page, and the active lyric stays centered.

The next production APK needs a version code above 34. The matching [update.json](update.json) is ready for a future `v0.14.0` GitHub release; this build has not been published.
