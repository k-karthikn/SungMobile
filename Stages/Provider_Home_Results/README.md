# SungMobile 0.13.0 (code 32)

[SungMobile-0.13.0-code32-release.apk](SungMobile-0.13.0-code32-release.apk) is the signed, optimized production APK for `com.karthi.sungmobile`. It is an in-place update over the code 31 production app because its package ID and production signing certificate match. This APK was built and verified locally; it has not been installed on a phone or published as a GitHub release.

This build adds provider-backed Home shelves and Material 3 expanding artwork carousels. Song cards start playback; album and playlist cards open their detail pages. The four mood shortcuts use compact rounded-rectangle chips. Existing local mixes, likes, recent plays, and pinned playlists remain in Library, where the mix refresh action now lives.

- APK size: 8,854,795 bytes (about 8.44 MiB).
- SHA-256: `4ec248cad6354177c24d92ebbb3dc89c68f99ce4eeaf610e15d2ce2d96842360` (also in [SHA256SUMS](SHA256SUMS)).
- Production signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 31.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.13.0`, code 32, minimum API 26. APK signature and zip alignment checks passed.
- `:core:youtube:test`, `:app:testDebugUnitTest`, `:app:lintRelease`, and `:app:assembleRelease` passed. See the [implementation report](../../docs/ui/home/PROVIDER_HOME_IMPLEMENTATION_REPORT.md) for UI and provider checks.

The next production APK must use a version code above 32. No updater manifest was generated because this APK has not been published.
