# Contributing to SungMobile

Thanks for taking the time to improve SungMobile. This guide covers the project layout, local checks, and the rules that help keep changes safe for playback and user data.

## Before you start

- Read the [README](README.md) for the app overview and setup requirements.
- Read the [architecture guide](docs/ARCHITECTURE.md) before moving code across modules.
- Check the relevant plan or report under [`docs/`](docs/) before changing an existing feature. Historical reports may mention artifacts that are no longer in the repository.
- Check the [active TODO list](docs/TODO.md) and [readiness review](docs/PRODUCTION_READINESS_REVIEW.md) for known follow-up work. Avoid duplicating tracked issues or treating historical stage plans as current tasks.
- For larger changes, open an issue or discussion first so the intended behavior and scope are clear.

## Set up the project

Use Android Studio with its bundled JDK, or JDK 17, and install Android SDK platform 37 and Build Tools 36.0.0. The Gradle wrapper is checked in.

Set the SDK location in an ignored `local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Alternatively, set `ANDROID_HOME`. Do not commit `local.properties`.

The debug app uses application ID `com.karthi.sungmobile.stage0`; the production app uses `com.karthi.sungmobile`. They install separately and have separate app data.

## Build and check changes

Run commands from the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew test :app:lintDebug
```

The first command builds the debug APK. The second runs host tests across the project and Android debug lint. Live provider probes are opt-in with `-Plive=true`; they need network access and may depend on external provider behavior. Do not enable them as part of routine checks unless the change needs that coverage.

Changes involving Android lifecycle behavior, playback, downloads, permissions, installation, or accessibility may also need device checks. Record the device and Android version, the actions performed, and any limitations. Passing host tests or lint does not establish device behavior.

The GitHub Actions workflow runs `test` and `:app:lintDebug` for pull requests and pushes to `main`. Keep the local check commands aligned with that workflow.

## Keep changes focused

- Keep each file focused on one responsibility. Put feature screens and controls in `app/src/main/java/com/karthi/sungmobile/feature/<feature>/`.
- Put reusable data formats, validation, repositories, and database access in `core/data`, in separate descriptive files.
- Keep app wiring in `app/AppContainer.kt` and `app/SungRoot.kt` small. Put feature behavior in its feature package.
- Follow existing module boundaries and reuse existing models and helpers instead of duplicating behavior.
- Prefer clear names, small functions, and explicit data models. Avoid unrelated formatting or dependency changes in a feature fix.
- Update user-facing documentation when behavior, setup, privacy, or limitations change.

See [`AGENTS.md`](AGENTS.md) for the repository's code-organization rules.

## Protect user data

Room schema changes must preserve existing user data and include a migration from the prior schema. Add focused migration tests for data migrations.

Backup and restore changes must validate the full input before writing, define how imported records interact with existing records, and cover interruption or retry behavior when a partial operation could lose data. Add focused restoration tests for those cases.

Do not replace, clear, or reset user data as a shortcut for a migration or bug fix. Avoid changing the production application ID or signing configuration.

## Pull requests

Keep each pull request focused and include:

- What changed and why.
- The commands you ran and their results. Call out checks you could not run.
- Device and Android-version results when device behavior is involved.
- Screenshots or a short recording for visible UI changes, when useful.
- Migration and restoration notes for changes that touch persisted data.

The repository's [pull request template](.github/pull_request_template.md) includes these prompts.

Do not commit generated build output, APKs, signing files, passwords, tokens, private keys, or user diagnostics. The project is licensed under [GPL-3.0-or-later](LICENSE); check [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) when adding or changing dependencies or assets.

## Production releases

Production releases are a maintainer task, separate from normal contributions. They require a version code higher than every production APK already distributed and the existing production application ID and signing key. Keep signing material outside the repository. Follow the [release guide](docs/updates/VERSIONING_AND_RELEASES.md) and use `scripts/production_build.py` to build and verify a release package. That script does not publish a GitHub release.
