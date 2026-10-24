# SungMobile production release candidate — 0.12.3 (code 23)

`SungMobile-0.12.3-code23-release.apk` is the optimized, signed Android APK for package `com.karthi.sungmobile`. This is a separate installation from the development package `com.karthi.sungmobile.stage0`. Installing it does not import the debug app's Likes, playlists, history, or settings; leave the debug app installed if you still need those data. Nothing was installed on a phone or published to GitHub as part of this build.

The final build used `:app:assembleRelease :app:lintRelease` with Android Studio's JBR and offline Gradle dependencies. Verification records, APK checksum and proposed update metadata are in this folder. `update.json` is prepared for the proposed `v0.12.3` tag; regenerate it if you choose another tag or rename/change the APK. It has not been published.

Host verification passed: R8 minification and resource optimization ran, release lint reported zero errors (22 warnings), `aapt` confirmed the package/version, and `apksigner` verified the v2 signature. The APK is 8,682,875 bytes (about 8.3 MiB). Production certificate SHA-256: `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`.

[Manual phone checklist](PHONE_TEST_CHECKLIST.md) · [Versioning and signing guide](../../docs/updates/VERSIONING_AND_RELEASES.md)

Back up **both** `~/.android/sungmobile-release/sungmobile-release.p12` and `~/.android/sungmobile-release/signing.properties` in a secure place outside the repository. Future production updates require this same signing identity and a versionCode higher than 23. The debug keystore is different and cannot update the production app.
