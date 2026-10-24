# Local songs follow-up — 0.12.9 (code 29)

[SungMobile-0.12.9-code29-release.apk](SungMobile-0.12.9-code29-release.apk) is the optimized, signed production APK for `com.karthi.sungmobile`. It uses the same certificate as code 28 and a higher version code, so install it over the existing production app without uninstalling it.

The Local songs tab now starts with the filter and a compact refresh icon. Its duplicate heading, description, Add folder button, and extra-folder list are removed; folder management remains in Settings → Local songs. Recent scan results stay visible when switching tabs, and the app refreshes stale scans in the background. The Android 15 download job now retries an expired/forbidden audio link once and gives a specific, safe failure message in its notification. A successful download invalidates the local scan cache. See the [feature guide](../../docs/local/README.md).

- APK size: 8,789,771 bytes (about 8.38 MiB).
- SHA-256: `375cd7c26e78afe77d68d5e004943171b3cf3489f749af4f5df21f9aa4358cc6` (also in [SHA256SUMS](SHA256SUMS)).
- Signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 28.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.12.9`, code `29`, minimum API 26. APK signature and zip alignment checks passed.
- 97 app host tests, debug/release lint, and optimized release build passed offline. Phone verification remains pending.

[update.json](update.json) describes these exact APK bytes for a proposed stable tag `v0.12.9`. Neither artifact has been published. If publishing later, tag matching committed source and upload the APK and JSON unchanged. The owner performs the [phone checks](PHONE_TEST_CHECKLIST.md); Codex did not install the APK.
