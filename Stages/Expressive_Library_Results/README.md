# Expressive shell and Library — 0.12.5 (code 25)

`SungMobile-0.12.5-code25-release.apk` is the optimized, signed production candidate for `com.karthi.sungmobile`. It includes the shared mini-player/navigation refresh and the Library tabs, Mixes and Settings history changes. It can update a code-24 production installation in place with the same signer; the `.stage0` debug app is a separate installation. No APK was installed or published in this task, and phone acceptance remains with the owner.

Host checks passed: 84 app unit tests, `:app:assembleRelease`, `:app:lintRelease` with zero errors (22 existing warnings), R8 minification and resource optimization, package/version inspection, zip alignment and APK Signature Scheme v2 verification. The APK is 8,737,735 bytes; SHA-256 is `d4b5d15732226143d6ab06b525ed2d437fc103a8e34629e41705eaf165d2c4af`. Signing certificate SHA-256 is `409eb23744908d19dd8724e6365d374e73feeaaf059a91ec02d0027f060049dd`, matching the code-24 production APK.

`update.json` describes **this exact APK** at proposed GitHub tag `v0.12.5`. If publishing, upload the APK and JSON unchanged as assets of a stable latest release with that exact tag. The repository's source tag should correspond to the source used for this build. If the APK, filename or tag changes, regenerate `update.json`.

[Manual phone checklist](PHONE_TEST_CHECKLIST.md) · [UI plan and reports](../../docs/ui/expressive-refresh/PLAN.md) · [Versioning guide](../../docs/updates/VERSIONING_AND_RELEASES.md)
