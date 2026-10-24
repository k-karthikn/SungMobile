# Production APK manual check — 0.12.3 (code 23)

1. Install `SungMobile-0.12.3-code23-release.apk`. Android should show **Sung** as a separate app from the earlier `.stage0` debug build; do not uninstall the debug build if you want to retain its local data.
2. Launch and confirm Home, Search, Library, Now Playing, Lyrics, Settings and the new launcher icon display correctly. A fresh production library is expected.
3. Search and play a YouTube song; test pause/resume, next/previous, queue, background playback, mini-player and notification controls. Test a song's Like and playlist actions, then relaunch to check persistence.
4. Check lyrics and Settings, including the update preference. For an update check, “No newer compatible update” or “No stable release” is expected until a matching higher-code production release is published.
5. Report any crash, missing artwork or controls, broken playback, or unexpected data behavior. The debug app's data should remain available in that separate installation.

Phone verification is intentionally left to the owner; this checklist is not an acceptance claim.
