# Now Playing + Queue UI — code 18

APK: `Sung-0.11.1-player-ui-debug.apk` (`0.11.1-player-ui`, code 18).

App ID `com.karthi.sungmobile.stage0`, Room schema 5. Same debug signing certificate SHA-256:
`137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`.
Install as an update over code 17; no uninstall or data reset required.

Host verification: 67 app tests, zero failures/errors/skips; lint zero errors and 10 dependency-version notices; APK assembly passed. Checks: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain`. APK verified using SDK apksigner. Screenshots include light/dark, large text, Reduced Motion, Queue and More. Some fixture renders intentionally have empty queue or no advanced-content slot; `more-options.png` exercises real settings controls.

New tests cover shuffled queue occurrence previews, duplicate identities, missing neighbors and More-sheet settings/Stop actions. Existing queue and player interaction tests remain passing. See test-results, build.log and lint-results.xml.

No device installation or phone acceptance claimed. Follow PHONE_TEST_CHECKLIST.md. Player implementation details: ../../docs/ui/player/REPORT.md. Search and Library redesigns follow in later slices. Reported scrolling lag is still unresolved; profiling is deferred by the owner.
