# Updater — code 20 manual acceptance

Install this build manually over the existing app. Keep its data.

1. Open Home's settings icon → App updates. Automatic updates should be on by default. Turn off/on, restart the app and confirm the choice persists.
2. Check and download now. With the repository's current prerelease-only setup, expect “No stable release is published yet”, not an older APK installation. Test offline and retry after reconnecting.
3. Optionally enable update notifications. Denial must leave the in-app update banner available when an update is ready.
4. For a real future update, use a higher versionCode (21 or above), the same package and signing key, and upload the generated update.json alongside that exact APK to a stable/latest GitHub release. See docs/updates/VERSIONING_AND_RELEASES.md.
5. From code 20, check/download the new release. Open Install, allow installation from Sung if Android requests it, return and tap Install again. Try cancelling once; then confirm. Check Likes, playlists, settings and playback after updating.
6. Background checks require an unmetered network and battery not low. Android/POCO may delay the approximately 12-hour job. Turning the toggle off cancels scheduled work; manual checks still work and may use mobile data.
7. Equal/older codes must never be offered. Host tests cover invalid checksum, mismatched package/key, incompatible Android version and blocked repository URLs.

The agent does not install or visually test the app. A live future-release install cannot be verified against the old prerelease alone. This code does not publish releases.
