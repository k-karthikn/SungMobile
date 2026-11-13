# Objective: Automate Android Release Pipeline via GitHub Actions

You are tasked with automating our manual Android release workflow by reading our existing codebase, scripts, and documentation, and converting them into a fully automated GitHub Actions pipeline.

## Step 1: Analyze Existing Project Structure (Mandatory)
Before writing any code, inspect the repository to understand our exact setup. Do not guess formats or paths. Check the following:
1. **Build System:** Inspect `build.gradle` / `build.gradle.kts`, `app/build.gradle`, and `gradle.properties` to determine the Java version, Gradle commands, and how release signing (`signingConfigs`) is currently configured.
2. **Existing Automation/Scripts:** Search the root directory and any `scripts/` folders for existing Python, Bash, or Gradle scripts that currently generate the SHA key, `update.json`, or `build.json` files.
3. **JSON Schema & Docs:** Read any existing `update.json`, `build.json`, `README.md`, or release docs to capture the exact JSON keys, structure, version naming conventions, and download URL formats used by the app's updater.
4. **Git History:** Check recent `git tag` and `git log` entries to understand our versioning format (e.g., `v1.0.0`) and commit message style.

## Step 2: Build the GitHub Actions Workflow
Create `.github/workflows/release.yml` that triggers automatically whenever a new version tag (e.g., `v*`) is pushed to GitHub.

The workflow must perform these exact steps on `ubuntu-latest`:
1. **Checkout & Environment:** Check out the repository with full Git history (for changelog generation) and set up the required JDK and Android SDK versions matching our Gradle config.
2. **Decode Signing Key:** Decode the Base64-encoded Android keystore from GitHub Secrets (`KEYSTORE_BASE64`) into a temporary file, and pass `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` to the Gradle build.
3. **Test & Build:** Run our unit tests, then build the signed Release APK (`./gradlew assembleRelease`).
4. **Generate Metadata Files:** Run our existing script (or replicate its exact logic) to calculate the APK's SHA checksum and generate `update.json` and `build.json` matching the exact schema found in Step 1.
5. **Create GitHub Release:** Use `softprops/action-gh-release@v2` to:
   - Create a release matching the pushed Git tag.
   - Auto-generate release notes from the commit history since the previous tag.
   - Attach the signed `.apk`, `update.json`, and `build.json` as release assets.
   - Publish the release immediately (not as a draft).

## Step 3: Local Helper & AI Rules Update
1. **Local Release Script:** Create or update a simple local script (e.g., `scripts/prepare_release.py` or `justfile`) that verifies tests pass locally, bumps the version code/name if needed, commits the change, creates the Git tag, and pushes to origin.
2. **Agent Memory:** Update or create the project's AI rules file (`.cursorrules`, `CLAUDE.md`, or `.windsurfrules`—check which one exists) with instructions on how to trigger a release using this new automated pipeline.

## Step 4: Output Setup Instructions for the User
Once you have generated the files, provide me with a concise checklist covering:
1. The exact terminal command to convert my local `.jks`/`.keystore` file into a Base64 string.
2. The exact names of the GitHub Repository Secrets I need to add in **Settings > Secrets and variables > Actions**.
3. The exact command I should run from now on to trigger a full build and release.

