# Lyrics/settings UI — 0.12.2-lyrics-ui (code 22)

[Installable debug APK](Sung-0.12.2-lyrics-ui-debug.apk) for manual installation over code 21. The app ID and signing key are unchanged; keep your existing installation to retain local data. Phone acceptance is pending.

The Lyrics page follows the supplied reference with real track artwork, source credit, compact timing controls, an emphasized current line and a return-to-current-line action. Sung's light/dark colors and Reduced Motion setting still apply. Settings and confirmation dialogs have a consistent layout. See the [implementation report](../../docs/ui/lyrics/REPORT.md) and [phone checklist](PHONE_TEST_CHECKLIST.md).

**15 focused host tests passed**, lint had zero errors, and the APK was built and signer verified. Verification logs are in `verification/`; `SHA256SUMS` covers the APK and metadata. `update.json` is prepared for proposed tag `v0.12.2-lyrics-ui`. It has not been published; regenerate it if the tag or APK filename changes.
