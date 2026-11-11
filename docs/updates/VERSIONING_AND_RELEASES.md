# Sung updates: version numbers and publishing

## Two different version numbers

In `app/build.gradle.kts` (the values below are examples; read the file for the current source version):

```kotlin
versionCode = 123
versionName = "1.2.3"
```

- **versionCode** is Android's ordering number within an application ID. Increase it for every update to that app. Never reuse or reduce it for production hotfixes or prereleases. Choose a number higher than every production APK already distributed.
- **versionName** is the readable label. For example, a small fix can change `1.2.3` to `1.2.4`, while a feature release can use `1.3.0`. This string and the GitHub tag do not decide whether an update is newer.
- To undo a bad release, restore the desired source changes but build with a NEW, higher versionCode. Do not distribute an older-code APK as the fix.

Sung's updater rejects equal or lower codes. Android also protects against normal downgrade installations. The production application ID is **`com.karthi.sungmobile`**. Keep that ID and the dedicated production signing key for every future production APK; changing either breaks in-place updates. The earlier debug application ID, `com.karthi.sungmobile.stage0`, is a separate Android app with separate data. Installing production does not transfer Likes, playlists, history, or settings from debug. Do not uninstall the debug app until you have finished anything you need there.

## Publish a future update

1. From a clean `main` checkout, choose a new `versionName` and run `python3 scripts/prepare_release.py <versionName>`. The helper increments `versionCode` by default; pass `--version-code` when the next code must be higher than a production APK already distributed. Never reuse or reduce a production version code.
2. The helper runs host tests and release lint, writes the version change, commits it, creates the matching `v<versionName>` tag, and pushes the commit and tag to `origin`.
3. The pushed tag runs `.github/workflows/release.yml`. GitHub Actions reads the production key from repository secrets, builds the signed APK, verifies its package and signing certificate, and publishes the versioned APK, `update.json`, and `build.json` in a normal stable GitHub release. Back up the production private key securely; without the same key, future APKs cannot update an installed production app. Never commit it or its passwords. The debug APK and debug key remain separate.

The workflow requires these GitHub Actions secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. `build.json` uses schema version 1 and records the tag, APK filename, and the five updater-manifest fields for the same APK. The app itself consumes only `update.json`.

If packaging an already-built APK is needed for investigation, run `python3 scripts/production_build.py --apk app/build/outputs/apk/release/app-release.apk`. This verifies and packages the APK but skips Gradle tests, lint and assembly; it does not publish a release.

The lower-level metadata generator is available if manual packaging is needed. Replace every example path and tag with the final APK's values:

```bash
python3 scripts/make_update_manifest.py \
  --apk /absolute/path/SungMobile-0.14.2-code36-release.apk \
  --tag v0.14.2 \
  --package-name com.karthi.sungmobile \
  --aapt /home/karthi/Android/Sdk/build-tools/36.0.0/aapt \
  --output /absolute/path/update.json
```

4. GitHub Actions creates the matching release with generated notes and uploads the exact APK, `update.json`, and `build.json` as release assets. It is published as a normal stable release, not a draft or prerelease.
5. Do not rename/rebuild/replace the APK after generating the JSON. If you change it, regenerate the JSON. The SHA-256 must match the uploaded bytes.
6. Test from the previous installed code: check/download, allow installation from Sung if Android asks, then confirm the update. Verify library data remains.

The low-level manifest generator reads package ID/versionCode/versionName from the APK and hashes that same file. Unlike `production_build.py`, it does not verify the signing key; verify with SDK apksigner before publishing if you use it directly. GitHub releases should contain one matching universal APK referenced by update.json. The generator defaults to the production package; use `--package-name com.karthi.sungmobile.stage0` only when deliberately packaging an old debug build.

## How the feature behaves

- Enabled by default. Settings → App updates → Automatic updates turns scheduled work on/off.
- Android JobScheduler checks roughly every 12 hours on an unmetered network with battery not low. Reboot persistence is enabled. Android/POCO battery restrictions, force-stop, network availability and job quotas can delay checks; this is not an exact timer.
- Background checks download a newer stable release into app-private update storage. This is separate from user-started music downloads into `Music/SungMobile/`.
- Manual “Check and download now” works even when automatic updates are off and may use mobile data. Leaving Settings cancels that manual task; retry later if needed.
- An optional notification and an in-app banner announce a prepared update. Android 13+ notification permission is requested only when you tap “Enable update notifications”; denied notifications do not prevent the in-app banner.
- Tap Install update. Android may first require “Allow from this source”; return and tap Install again. Android shows the installer confirmation. This feature does **not** promise silent installation or interrupt playback with an installer from the background. Installing/restarting the app may interrupt playback.
- Checks require a strictly higher APK versionCode, matching app ID and signing certificate, compatible Android version, and matching SHA-256. Validation runs again before installation. Signing-key rotation is not supported by this updater; retain the same signer.
- Only public stable releases of this repository are used. There is no embedded GitHub token. The existing `0.3.0-s2` release was a prerelease when inspected and is ignored. Releases without update.json are reported as unsupported rather than guessed from their names.
- Install this updater-enabled build manually once. Older app versions cannot gain an updater merely because a new GitHub release exists.

References: [Android versioning](https://developer.android.com/studio/publish/versioning), [Android background scheduling](https://developer.android.com/topic/performance/background-optimization), [Package installer user confirmation](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)).
