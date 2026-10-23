# Home UI — code 17

Build **0.11.0-home-ui**, package `com.karthi.sungmobile.stage0`. Install over the existing app; the signing certificate is unchanged. Room stays at schema 5.

- [Update APK](Sung-0.11.0-home-ui-debug.apk)
- [Manual POCO checklist](PHONE_TEST_CHECKLIST.md)
- [Dark preview](previews/home-dark.png) · [Light preview](previews/home-light.png)
- [Plan](../../docs/ui/home/PLAN.md) · [Implementation report](../../docs/ui/home/REPORT.md)
- [Verification](verification.json) · [Build log](build.log) · [Checksum](SHA256SUMS)

All **64 app tests passed**, including 10 new Home UI cases. Lint: **0 errors, 13 dependency-version notices**. Debug assembly, package/version, signing certificate and checksum were checked. Light/dark, reduced-motion, large-text, 320dp and expanded renders were inspected. Offline fixtures are test-only.

The performance work removes repeated UI updates and full queue rebuilding from position ticks. Phone smoothness is **not yet verified**. No APK was installed and no USB/device automation was used. The next page awaits the owner's reference and approval.
