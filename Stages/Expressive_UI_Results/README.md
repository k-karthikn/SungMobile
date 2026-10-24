# Complete expressive UI — 0.12.6 (code 26)

[SungMobile-0.12.6-code26-release.apk](SungMobile-0.12.6-code26-release.apk) is the optimized, signed production APK for `com.karthi.sungmobile`. It includes all five UI slices: shared player/navigation, Library, Home, Search/rows, and Lyrics/loading/settings. It can update the code-25 production app in place with the same signing certificate. The separate `.stage0` debug app has different data.

- APK size: 8,739,855 bytes (about 8.34 MiB).
- SHA-256: `f049b41db8e2d152cd439fb14ba2c9b365c18e9b77d6484bab248b8e27ddc4ee` (also in [SHA256SUMS](SHA256SUMS)).
- Signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching the code-25 APK.
- `aapt` confirms version `0.12.6`, code `26`, package `com.karthi.sungmobile`, minimum API 26; `apksigner verify` passed.
- Offline `:app:assembleRelease` and `:app:lintRelease` passed. The unchanged source had 90 passing debug unit tests in the preceding UI implementation turn.

[update.json](update.json) describes this exact APK for a proposed stable GitHub tag `v0.12.6`. Neither asset has been published. If publishing later, commit the matching source before creating the tag, then upload the APK and JSON unchanged; regenerate JSON if the APK, filename or tag changes. The owner performs [manual phone testing](PHONE_TEST_CHECKLIST.md). No APK was installed on the phone by Codex.
