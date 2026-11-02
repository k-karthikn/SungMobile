# SungMobile project board

This is the active project board for implementation follow-ups and verification. Items below are based on the current source and the evidence in the [production readiness review](PRODUCTION_READINESS_REVIEW.md), not a promise that every listed feature has been tested on a device.

## Workflow

- **Backlog** — ideas and tasks not yet prioritised.
- **Todo** — prioritised and ready to start.
- **In Progress** — actively being worked on. Move an item here when work begins.
- **In Review** — implementation is ready and waiting for feedback, CI, or verification.
- **Done** — acceptance criteria are met and evidence is recorded. Keep completed work here as a short audit trail.

Every card has a clear description, one owner, a target milestone, and acceptance criteria. `Project maintainer` is the default owner because no named assignees are maintained in this repository; replace it with a person's name when ownership is agreed. Milestones are used instead of guessed dates. Update this board when work changes status, and record test or device evidence on the card or in the readiness review.

## Backlog

_No unprioritised items are currently recorded._

## Todo

### Verify interrupted downloads and local audio on device

- **Description:** Exercise successful downloads, cancellation, network loss, force-stop/process death, and restart through service and job paths; also verify folder grants and local playback. Investigate recovery only if pending MediaStore rows persist beyond platform expiry.
- **Owner:** Project maintainer
- **Target:** Next device acceptance pass
- **Acceptance criteria:** On a supported device, completed audio remains visible and playable; interrupted transfers leave no lingering pending row after platform expiry; folder access and local playback work. Record device, Android version, paths exercised, and outcomes in the readiness review.

### Verify background playback and Android media controls

- **Description:** Check screen-off playback, lock-screen and notification controls, audio focus, download notifications, cancellation, and restart behavior.
- **Owner:** Project maintainer
- **Target:** Next device acceptance pass
- **Acceptance criteria:** Each listed behavior works on the recorded device/API, and any OEM-specific failures or follow-up work are documented in the readiness review.

### Verify production APK update and data continuity

- **Description:** Test the direct update flow on a device with a production install and existing library data.
- **Owner:** Project maintainer
- **Target:** Before the next public release
- **Acceptance criteria:** Check, download, install, and relaunch succeed; existing library data remains; app versions, device/API, signer continuity, and outcome are recorded. The exact APK and `update.json` also meet the [release guide](updates/VERSIONING_AND_RELEASES.md) before publication.

### Verify Spotify account and import flows

- **Description:** Check sign-in, reconnect, disconnect, playlist import, liked-song import, and rejected or unavailable account states on device.
- **Owner:** Project maintainer
- **Target:** Next Spotify device acceptance pass
- **Acceptance criteria:** All listed states produce the expected user-visible result, disconnect removes saved credentials, and device/API plus outcomes are recorded.

### Retry instrumentation on the Android 15 device

- **Description:** Approve USB app installation on the POCO M6 Pro 5G and rerun the instrumentation suite; the previous attempt was blocked by `INSTALL_FAILED_USER_RESTRICTED` and ran zero tests.
- **Owner:** Project maintainer
- **Target:** Next connected-device test session
- **Acceptance criteria:** Instrumentation installs and completes with a recorded test count and result, or the new blocker and device state are documented. A zero-test run does not count as a pass.

### Capture Home and listening-statistics performance baseline

- **Description:** Measure release-like frame time and recomposition for Home and expanded listening statistics on a representative device.
- **Owner:** Project maintainer
- **Target:** Before performance changes are prioritised
- **Acceptance criteria:** Save reproducible baseline traces with device/API and build details; any later optimization includes comparable before-and-after measurements.

### Audit accessibility and display variations

- **Description:** Review TalkBack labels and focus order, large text, reduced motion, narrow screens, and light and dark themes.
- **Owner:** Project maintainer
- **Target:** Next accessibility review pass
- **Acceptance criteria:** Record device/API, flows and settings covered, and remaining issues; file actionable issues as separate cards with acceptance criteria.

### Confirm the Android checks workflow on GitHub

- **Description:** Confirm the configured host-test and debug-lint workflow has completed successfully on GitHub.
- **Owner:** Project maintainer
- **Target:** Before relying on CI as a release gate
- **Acceptance criteria:** Record a successful workflow run for the current branch or `main`; if it fails, capture the failing job and create a follow-up card.

### Complete pre-release source and dependency review

- **Description:** Before a public release, review the resolved release dependency graph, required notices and corresponding source, and production version code against every distributed build.
- **Owner:** Project maintainer
- **Target:** Before the next public release
- **Acceptance criteria:** Release dependencies and notices are reviewed, corresponding-source obligations are addressed, and the chosen `versionCode` is higher than every production APK already distributed. Record the review with the release artifacts.

### Triage lint warnings and dependency upgrades

- **Description:** Verify resource and string references before removing lint findings; upgrade dependencies one family at a time with appropriate checks and device smoke coverage.
- **Owner:** Project maintainer
- **Target:** As part of the next maintenance change
- **Acceptance criteria:** Each addressed warning is checked against actual references, each dependency change is isolated to one family, and relevant checks plus device smoke results are recorded with the change.

### Split app wiring and screens at clear responsibility boundaries

- **Description:** Continue splitting app wiring and large screens when a change exposes a clear responsibility boundary.
- **Owner:** Project maintainer
- **Target:** As part of the next feature change that creates a clear boundary
- **Acceptance criteria:** Responsibility is moved to the appropriate feature or data package, app wiring remains small, and navigation, saved state, and back behavior are preserved and verified.

## In Progress

_No active work is recorded. Move a card here when someone starts it and update its owner if needed._

## In Review

_No implementation is currently awaiting review. Device and CI checks that have not started remain in Todo._

## Done

### Backup import validation and recovery

- **Description:** Validate backup input before writes and recover settings replay using a Room journal; playlist collisions are deterministic and repeated imports are idempotent.
- **Owner:** Project maintainer
- **Target:** Completed in current source; retain as verified implementation
- **Acceptance criteria:** The readiness review records these safeguards as implemented; device and import behavior is not represented as newly verified by this board update.
- **Evidence:** [`PRODUCTION_READINESS_REVIEW.md`](PRODUCTION_READINESS_REVIEW.md#safeguards-already-in-place)

### Updater package and download safeguards

- **Description:** Validate saved APK/manifest pairs, stage and verify downloads before replacement, and check package, version, checksum, signer, and SDK compatibility before install.
- **Owner:** Project maintainer
- **Target:** Completed in current source; retain as verified implementation
- **Acceptance criteria:** The updater enforces these validation steps in source; on-device end-to-end update verification remains tracked in Todo.
- **Evidence:** [`PRODUCTION_READINESS_REVIEW.md`](PRODUCTION_READINESS_REVIEW.md#safeguards-already-in-place)

### Shared download outcome handling

- **Description:** Share transfer outcome handling across service and job download paths while preserving their separate Android lifecycle entry points.
- **Owner:** Project maintainer
- **Target:** Completed in current source; retain as verified implementation
- **Acceptance criteria:** Both entry points use shared outcome handling and retain their lifecycle-specific execution paths; device lifecycle verification remains tracked in Todo.
- **Evidence:** [`PRODUCTION_READINESS_REVIEW.md`](PRODUCTION_READINESS_REVIEW.md#safeguards-already-in-place)

## Updating this board

Keep cards small enough to have a clear completion check. When a card moves to **Done**, record evidence that meets its acceptance criteria. If an item is already implemented, keep it in Done only when the evidence is available; otherwise track the missing review or device check in the appropriate active column.
