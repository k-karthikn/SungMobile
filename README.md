# SungMobile

SungMobile is a Kotlin and Jetpack Compose music player for Android. It combines YouTube music discovery and playback, songs stored on the device, a local library, and listening statistics in a Material 3 interface. The project draws on the [Sung desktop app](https://github.com/k-karthikn/Sung) and uses [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) for YouTube access.

The source version is declared in [`app/build.gradle.kts`](app/build.gradle.kts). A source version is not a claim that the current checkout has been published or tested on every device. Before distributing another update, use a version code higher than every production APK already distributed.

## What the app does

- Browse Home recommendations, search songs, albums, artists and playlists, and play collections through a service-owned queue.
- Control playback from the Now Playing screen, mini-player and Android media controls. The player supports queue editing, shuffle, repeat, playback speed, a sleep timer and resume points.
- Save likes and playlists; browse local audio and downloads; review recent listening, smart mixes and listening statistics.
- View synced or plain lyrics when a source is available. The Now Playing lyric preview is hidden when none can be found.
- Choose Material 3 colors, artwork accents and other appearance options in Settings.
- Export and import a JSON backup of preferences, likes, playlists, smart playlists and qualified listening history. The backup does **not** contain audio files or downloaded music; keep those files separately. Import keeps existing library items.
- Send feedback through an installed email app, with an optional app diagnostic log. Logs are attached only when the user selects that option.

The **For you** Home row uses likes and listening signals to rank music on the device. It can request related songs from YouTube using a liked song as a seed. Private mode hides this row and pauses history-based personalization. The ordinary Home rows remain available. Recommendation choices can be cleared in Settings.

## Requirements and setup

- Android Studio with its bundled JBR, Android SDK platform 37 and Android Build Tools. The Gradle wrapper is included.
- An SDK path in ignored `local.properties` (`sdk.dir=/path/to/Android/Sdk`) or `ANDROID_HOME`.
- Android 8.0 / API 26 or newer to install the app.

From the project root, build a debug APK and run the host tests and Android debug lint:

```bash
./gradlew :app:assembleDebug
./gradlew test :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its application ID is `com.karthi.sungmobile.stage0`, so it installs separately from the production app (`com.karthi.sungmobile`) and has separate app data. If your terminal does not use Android Studio's JBR automatically, set `JAVA_HOME` to its `jbr` directory before running Gradle.

Host tests do not require live network access. Live provider probes are opt-in with `-Plive=true`; see [`docs/PLAYBACK_GUIDE.md`](docs/PLAYBACK_GUIDE.md). Automated device tests require a connected device, and passing host tests or lint does not replace a phone acceptance check.

## Build a production APK and update manifest

The production build requires the existing signing configuration at `~/.android/sungmobile-release/signing.properties`. Keep that configuration and its private key outside the repository. An update must use the same production application ID and signing key as the installed app.

1. Increase `versionCode` and set `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts). Never reuse a production version code.
2. In Android Studio's Terminal, run:

   ```bash
   python3 scripts/production_build.py
   ```

3. The script runs unit tests and release lint, builds the signed APK, verifies its package, version, signing certificate and ZIP alignment, then writes a versioned APK, `update.json` and `SHA256SUMS` to `release-assets/v<versionName>/`. It does **not** publish a GitHub release.
4. After checking the APK on a device, upload the APK and `update.json` from that same directory as assets of the matching stable GitHub release tag. Rebuild or rename the APK only if you also regenerate `update.json`.

If you already have a signed production APK, `python3 scripts/production_build.py --apk app/build/outputs/apk/release/app-release.apk` verifies and packages it without rerunning Gradle. The script refuses to overwrite different assets for the same version unless `--replace` is supplied before publication. See the [release and update guide](docs/updates/VERSIONING_AND_RELEASES.md) for updater behavior and signing precautions.

## Code layout

| Module | Responsibility |
| --- | --- |
| [`core:model`](core/model) | Provider-neutral music, queue, library and recommendation models. |
| [`core:youtube`](core/youtube) | NewPipeExtractor adapter, browse/search requests and source error mapping. |
| [`core:playback`](core/playback) | Queue coordination, Media3 playback and media session integration. |
| [`core:data`](core/data) | Room, DataStore, backups, history, local library and repositories. |
| [`core:designsystem`](core/designsystem) | Theme, icons, artwork and shared Compose controls. |
| [`app`](app) | Android app wiring, navigation and feature screens under `app/src/main/java/com/karthi/sungmobile/feature/`. |

Keep feature UI in its feature package and reusable storage/data logic in `core:data`; see [`AGENTS.md`](AGENTS.md). Earlier milestone notes live under [`docs`](docs). The old `Stages/` artifacts and deferred Stage 7 source snapshot were removed from the working tree on 30 October 2026; milestone reports may still describe those historical files.

New contributors can start with [`CONTRIBUTE.md`](CONTRIBUTE.md) for setup and checks, then use the [documentation index](docs/README.md) and [architecture guide](docs/ARCHITECTURE.md) for code ownership and dependency direction.

## License

SungMobile is licensed under [GPL-3.0-or-later](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) for dependencies and assets. Check notices and corresponding-source requirements before public distribution.
