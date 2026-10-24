# Local songs and downloads — 0.12.8 (code 28)

[SungMobile-0.12.8-code28-release.apk](SungMobile-0.12.8-code28-release.apk) is the optimized, signed production APK for `com.karthi.sungmobile`. It has the same signing certificate as code 27 and a higher version code, so it can update the existing production installation in place. Do not uninstall the existing app before installing it.

This build adds Library → Local songs, default scanning of `Music/SungMobile/`, extra folders selected in Settings, and direct local playback through the existing player. Online song menus can download audio into `Music/SungMobile/` with progress and cancellation notifications on Android 10+. The shared play/pause control morphs between rounded square and flower; active synced lyrics use theme-colored word styles from the font already bundled with the app. See the [feature guide](../../docs/local/README.md).

- APK size: 8,789,771 bytes (about 8.38 MiB).
- SHA-256: `96113f57ff7b72ba77c3292bef199d082a854d71a812a7f452cba3d622661753` (also in [SHA256SUMS](SHA256SUMS)).
- Signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 27.
- `aapt` confirms `com.karthi.sungmobile`, version `0.12.8`, code `28`, minimum API 26; `apksigner verify` and `zipalign` passed.
- 134 app/playback host tests, release lint and optimized release build passed offline. Phone verification is pending.

[update.json](update.json) describes these exact APK bytes for proposed stable tag `v0.12.8`. Neither artifact was published. If publishing later, commit matching source before tagging and upload the APK and JSON unchanged. The owner performs the [manual phone checks](PHONE_TEST_CHECKLIST.md); Codex did not install the APK.
