# SungMobile code organization

- Keep each file focused on one responsibility. Do not place a whole feature's UI, data format, persistence, and tests in one file.
- Put Android screens and controls in `app/src/main/java/com/karthi/sungmobile/feature/<feature>/`.
- Put reusable data formats, validation, repositories, and database access in `core/data` under separate, descriptive files.
- Keep app wiring in `app/AppContainer.kt` and `app/SungRoot.kt` small; feature behavior belongs to its feature package.
- Use clear names, small functions, and explicit data models. Preserve existing architecture and avoid duplicate logic.
- Add focused tests for data migration, restoration, and bug fixes when behavior could lose user data.

## Releases

- Start a production release with `python3 scripts/prepare_release.py <versionName>` from a clean `main` checkout. It runs the host tests and release lint, updates the app version, commits it, creates the matching `v<versionName>` tag, and pushes both to `origin`.
- A pushed `v*` tag runs `.github/workflows/release.yml`. Keep the tag equal to `v` plus `versionName`; the workflow rejects mismatches so the update manifest always points at the matching release asset.
- Keep production-signing material only in local signing properties or GitHub Actions secrets. Never commit keystores, passwords, aliases, decoded keys, or generated release assets.
- The updater consumes `update.json`. Its `versionCode`, `versionName`, `packageName`, `apkUrl`, and `sha256` must describe the exact APK published in the GitHub release. `build.json` is a release-provenance asset generated from that same APK.
