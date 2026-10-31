# Production readiness review

This page tracks release risks that need device evidence or measured investigation. The [TODO list](TODO.md) is the short contributor checklist; keep the details here only while they help someone complete or verify an item.

## Current source status — 31 October 2026

- Source configuration: **0.15.5**, version code **49**. This does not assert that this version has been built as a production APK, installed, or published.
- The previous release-build record is for **0.14.9**, code **43**. Check every distributed APK before choosing a production version code.
- Device: POCO M6 Pro 5G (`23076PC4BI`), Android 15 / API 35. The last instrumentation install attempt was blocked by `INSTALL_FAILED_USER_RESTRICTED`; it ran zero tests. Approve USB app installation before retrying.
- Debug lint passes. After removing unreferenced strings and obsolete launcher assets, it reports no unused-resource findings. Other findings concern KTX and dependency suggestions, launcher guidance, or platform/configuration checks.
- The GitHub Actions workflow is configured for host tests and debug lint. Confirm its status in GitHub before relying on it as a release gate.

## Open verification work

### Interrupted downloads and local audio

`AudioTransfer` removes its pending MediaStore row in `finally`; process death skips that cleanup. Android also expires pending rows, but verify the behavior on supported devices. Exercise successful download, cancellation, network loss, force-stop, process death, and app restart through both service and job paths. Confirm completed files remain visible and playable. Add app-owned recovery only if a device shows pending rows persisting beyond platform expiry.

Also verify folder grants and local playback on device. MediaStore, Storage Access Framework grants, Android lifecycle behavior, and OEM restrictions are not fully represented by host tests.

### Production update flow

On a device with an installed production build and user library data, check for an update, download, approve installation, and verify the app opens with its data intact. Record app versions, device/API, signer continuity, and outcomes. Do not publish until the exact APK and `update.json` have passed the [release guide](updates/VERSIONING_AND_RELEASES.md).

### Playback and Android integration

Verify screen-off playback, lock-screen and notification controls, audio focus, download notifications, cancellation, and restart behavior. Host tests do not prove OEM background execution, system UI integration, or real provider playback.

### Performance

Capture release-like frame-time and recomposition traces for Home and expanded listening statistics on a representative device. Compare before and after any performance change. Keep high-frequency playback position reads in small progress components and preserve large-font behavior.

### Accessibility and maintainability

Review TalkBack labels and focus order, large text, reduced motion, narrow screens, and both themes on device. Continue splitting app wiring and large screens only where it creates a clear responsibility boundary; preserve navigation, saved state, and back behavior.

## Safeguards already in place

- Backup imports validate input before writes and use a Room recovery journal for settings replay. Playlist collision behavior is deterministic and repeated imports are idempotent.
- Updater state validates saved APK/manifest pairs before displaying an update as ready. Downloads stage and verify files before replacement, and installation validates package, version, checksum, signer, and SDK compatibility.
- The app has separate model, provider, data, playback, design-system, and feature responsibilities. Room migration schemas are checked in; never enable destructive migration for user data.
- Download service/job paths share transfer outcome handling while retaining their separate Android lifecycle entry points.
- The repository workflow runs host tests and debug lint on pull requests and pushes to `main`; it does not use release signing secrets or publish APKs.

See [Architecture](ARCHITECTURE.md), [Playback](PLAYBACK_GUIDE.md), [Contributing](../CONTRIBUTE.md), and the [documentation index](README.md) for implementation guidance.
