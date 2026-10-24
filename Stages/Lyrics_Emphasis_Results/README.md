# Lyrics emphasis and player control — 0.12.7 (code 27)

[SungMobile-0.12.7-code27-release.apk](SungMobile-0.12.7-code27-release.apk) is the optimized, signed production APK for `com.karthi.sungmobile`. It updates code 26 in place with the same signing certificate. The separate `.stage0` debug app has different data.

The active synced lyric line now has no filled background or side bar. Current-line text uses the theme's `onSurface` color and selected words use its `primary` color with stronger weight; this is static typography, not word-by-word timing. The provider credit no longer displays “Source: Source:”. Mini-player, Now Playing and Queue peek use one shared expressive play/pause control, including the same icon transition and loading treatment. All existing playback and lyric actions remain. The reference's floating timestamp was not added.

- APK size: 8,756,239 bytes (about 8.35 MiB).
- SHA-256: `4878676457596e42b8521c77296a8d6b3db7adc9a7fc0bab24b090a29b2408fb` (also in [SHA256SUMS](SHA256SUMS)).
- Signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 26.
- `aapt` confirms `com.karthi.sungmobile`, version `0.12.7`, code `27`, minimum API 26; `apksigner verify` passed.
- 91 debug unit tests, debug/release lint and optimized release build passed offline.

[update.json](update.json) describes this exact APK for proposed stable tag `v0.12.7`. Neither asset was published. If publishing later, commit the matching source before tagging and upload the APK and JSON unchanged. The owner performs [manual phone testing](PHONE_TEST_CHECKLIST.md); Codex did not install the APK.
