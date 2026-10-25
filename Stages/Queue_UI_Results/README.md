# SungMobile 0.13.1 (code 33)

[SungMobile-0.13.1-code33-release.apk](SungMobile-0.13.1-code33-release.apk) is the signed, optimized production APK for `com.karthi.sungmobile`. It updates the code 32 build in place with the same production signing certificate. It has not been installed on a phone or published.

The Queue now uses compact continuous rows with rounded-square artwork, one-line title and artist, and a trailing duration when known. The current track has a theme-colored highlight. Existing queue entries with malformed artist punctuation are cleaned for display, and newly loaded Home songs preserve the provider's artist separators. This APK also includes the recently completed Search button removal and player seek-track refinements.

- Size: 8,854,387 bytes (about 8.44 MiB).
- SHA-256: `0a3fa0a3b10a3e9dc69bd8673b68e5f85b747de173614dd719dbc3ae55a9ed28` (also in [SHA256SUMS](SHA256SUMS)).
- Production signer SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching code 32.
- `aapt` confirms package `com.karthi.sungmobile`, version `0.13.1`, code 33, minimum API 26. APK signature and zip alignment checks passed.
- `:core:youtube:test`, `:app:testDebugUnitTest`, `:app:lintRelease`, and `:app:assembleRelease` passed. Queue layout was also inspected in rendered normal and large-text tests.

The next production APK needs a version code above 33. No updater manifest was generated because this APK has not been published.
