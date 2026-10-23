# Library UI — code 19

APK: `Sung-0.11.2-library-ui-debug.apk`, version `0.11.2-library-ui`, code 19.

Application ID: `com.karthi.sungmobile.stage0`. Room schema 5 unchanged.
Signing certificate SHA-256: `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850` (same as code 18).
Install as an update; no uninstall or data clearing required.

Validation: 72 app tests passed, zero failures/errors/skips. Lint: zero errors and 10 dependency-version notices. APK build and apksigner verification passed; aapt confirmed package/version. `git diff --check` passed.

Command: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain`.

New tests check independent heart/menu actions, selection instead of playback, bulk selection exit, Sessions and smart-playlist reachability, and retained playlist-management menu actions. Light, dark with 2× text, and playlist renders inspected; placeholder artwork is intentional in these fixtures. Evidence: screenshots/, test-results/, lint-results.xml, build.log.

The owner accepted player UI code 18. This Library build has not been installed or tested by the agent on a phone. See PHONE_TEST_CHECKLIST.md and ../../docs/ui/library/REPORT.md. Search UI and deferred scrolling profiling remain separate work.
