# Download compatibility fix — 0.12.10 (code 30)

[SungMobile-0.12.10-code30-release.apk](SungMobile-0.12.10-code30-release.apk) is the optimized, signed production APK for `com.karthi.sungmobile`. It uses the existing production signing certificate and can install over code 29 without clearing app data.

On Android 15, MediaStore rejected downloaded `audio/webm` with `Unsupported MIME type audio/webm`. Online playback supports that format, but downloads now choose the highest-bitrate compatible MP4/M4A (or another supported audio format) and save with a canonical MIME type. A phone retry saved “Kutti Story (From \"Master\")” to `Music/SungMobile`; the resulting `.m4a` file was confirmed over ADB. The final artifact only removes temporary progress logging from that phone-tested build. Other songs, cancellation, and long background transfers still need broader phone checks.

- APK size: 8,789,771 bytes (about 8.38 MiB).
- SHA-256: `e675188d09382dc8badc0bac3e5fb659f83cdbe2d018cbcfb9811727b8ba9ad9` (also in [SHA256SUMS](SHA256SUMS)).
- Signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.12.10`, code `30`, minimum API 26. APK signature and zip alignment checks passed.
- 98 app tests, 18 YouTube module tests, debug/release lint, and optimized release build passed offline.

[update.json](update.json) describes these exact APK bytes for a proposed stable tag `v0.12.10`. Neither artifact has been published. If publishing later, tag matching committed source and upload the APK and JSON unchanged. See the [phone checks](PHONE_TEST_CHECKLIST.md).
