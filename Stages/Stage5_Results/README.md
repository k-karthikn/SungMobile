# SungMobile Stage 5 results

Built 18 October 2026. **Host verified; owner accepted Stage 5 on the POCO.** The owner reported that lyric availability and timing vary between search results and differently named playlist entries. Code 9 improves the recoverable cases; the owner subsequently reported everything working fine.

- Latest APK: [Sung-0.6.1-stage5-debug.apk](Sung-0.6.1-stage5-debug.apk), version code 9. Install over code 8 without uninstalling or clearing storage.
- App ID and signing certificate are unchanged from code 8 (`com.karthi.sungmobile.stage0`; certificate SHA-256 `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`). APK SHA-256: `441cb7c2aa100b5ebc990e981a9734d0fc7c9a654be2a8f5831f13d3b63195cb`.
- Code 9 reads plain lyrics from mobile YouTube responses and, when LRCLIB is enabled, checks for a strictly matched timed result even if YouTube already supplied plain text. It keeps YouTube text if LRCLIB has no timed match. The full host suite, lint and APK build passed (zero lint errors, 13 notices).

Earlier build:

- APK: [Sung-0.6.0-stage5-debug.apk](Sung-0.6.0-stage5-debug.apk), version code 8.
- App ID: `com.karthi.sungmobile.stage0`. It is signed with the new debug key created after OS migration; its certificate SHA-256 is `137475c127f1805b413c4362ddd69573e41f70c3cde76fd38a1bf5ebabfbe850`, matching code 7. Install over the code 7 app to keep its data.
- APK SHA-256: `379ab95b45af98d4b6710e665af82c7c9b2ae32ca7917766f62c6b9e7e00eccf`.
- Host suite: 135 cases = **129 passed, six opt-in live probes skipped, zero failures**. The direct YouTube lyrics probe passed in a separate opt-in live run. Lint: zero errors, 13 dependency/version notices. Build succeeded.

The POCO test checklist is [PHONE_TEST_CHECKLIST.md](PHONE_TEST_CHECKLIST.md). No installation or physical-device test was performed by the assistant.
