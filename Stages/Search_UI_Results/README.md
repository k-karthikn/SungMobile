# Search UI — 0.12.1-search-ui (code 21)

[Installable debug APK](Sung-0.12.1-search-ui-debug.apk) for manual installation over code 20. Application ID and signing certificate match the previous build, so keep your existing app installed and its data intact. Phone acceptance is pending.

Search now has a rounded editable field with clear action, icon category chips, an explicit search/link button, recent-search rows and an empty state. Song results use direct heart and plus actions in the shared song layout; existing search and playback behavior is retained. See the [implementation report](../../docs/ui/search/REPORT.md) and [phone checklist](PHONE_TEST_CHECKLIST.md).

Host verification: **20 focused nonvisual tests passed**, zero failures; lint zero errors and 18 warnings; APK built and signer verified. Details are in `verification/`. `SHA256SUMS` covers the APK and `update.json`.

`update.json` uses proposed tag `v0.12.1-search-ui` with this exact APK filename. It is prepared for a stable GitHub release but **has not been published**. If you use a different tag or filename, regenerate the metadata with `scripts/make_update_manifest.py`.
