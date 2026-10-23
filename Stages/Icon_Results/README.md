# Launcher icon correction — 0.12.3-icon (code 23)

[Installable debug APK](Sung-0.12.3-icon-debug.apk) for manual installation over code 22. Keep the current installation so local data remains. [Phone checklist](PHONE_TEST_CHECKLIST.md) and [implementation report](../../docs/ui/icon/REPORT.md).

The original full logo is now centered with a 12 dp inset inside both adaptive launcher masks. The former foreground cropped the note. The opaque color bitmap was removed from the monochrome layer, which otherwise could become a solid themed shape. No artwork was regenerated.

Build and lint results are under `verification/`. The APK has application ID `com.karthi.sungmobile.stage0`, code 23 and the retained debug signing certificate. `update.json` uses proposed tag `v0.12.3-icon` and this exact APK filename; no GitHub release was published. Regenerate the metadata if the tag or APK filename changes.
