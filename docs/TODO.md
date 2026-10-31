# Project TODO list

This is the active, contributor-facing list of known follow-up work for the current Android project. Check items here off only after the change is implemented and its stated evidence is recorded. The [production readiness review](PRODUCTION_READINESS_REVIEW.md) has the detailed background and acceptance criteria.

## Device verification

- [ ] On a supported Android device, verify download success, cancellation, network loss, force-stop, and restart. Confirm interrupted pending MediaStore rows expire or are removed and completed audio stays available.
- [ ] Verify local folder access and playback, screen-off playback, notification controls, and playback after navigating away from the app.
- [ ] Verify the direct APK update flow with an installed production build: check, download, install, and confirm library data remains. Record the device, Android version, app versions, and outcomes.
- [ ] Re-run instrumentation after approving USB app installation on the POCO M6 Pro 5G / Android 15 device; the previous attempt was blocked by `INSTALL_FAILED_USER_RESTRICTED` and ran zero tests.
- [ ] Verify Spotify sign-in, reconnect, disconnect, playlist import, liked-song import, and account rejection/unavailable states on device. Confirm that disconnect removes saved credentials.

## Performance and accessibility

- [ ] Capture a release-like frame-time and recomposition baseline for Home and expanded listening statistics on a representative device. Make follow-up changes only for measured bottlenecks and compare the same traces afterward.
- [ ] Audit core flows with TalkBack, large text, reduced motion, narrow screens, and light and dark themes. Record device/API and any remaining accessibility issues.

## Maintainability and release process

- [ ] Continue splitting app wiring and large screens when a change creates a clear responsibility boundary; preserve navigation, saved state, and back behavior.
- [ ] Triage lint warnings by verifying resource and string references before removal. Upgrade dependencies one family at a time and include appropriate checks and device smoke coverage.
- [ ] Confirm the Android checks workflow has run successfully on GitHub. Keep signing material outside CI and the repository.
- [ ] Before a public release, inventory the resolved release dependency graph, verify required notices and corresponding source, raise `versionCode` above every distributed production build, and follow the [release guide](updates/VERSIONING_AND_RELEASES.md).

## Updating this list

Add a checkbox when work is actionable and not already tracked in an issue or pull request. Include enough context for another contributor to reproduce or verify it. Remove completed items after recording the result in the readiness review or release record; avoid copying old milestone checklists here.
