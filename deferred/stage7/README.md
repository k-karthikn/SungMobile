# Deferred Stage 7 source

The owner deferred device-storage music and downloads on 18 October 2026 in favor of online-only SungMobile. `source_snapshot/` contains the complete Stage 7-only feature files and snapshots of every shared integration file immediately before they were disconnected. This directory is outside every Gradle source set and is not packaged in the current APK.

The historical Stage 7 APK remains in `Stages/Stage7_Results/` for reference, but it is not the current recommended build. Reintroducing the feature later requires restoring selected source, updating app wiring, storage permissions and WorkManager dependency, and planning a new Room schema migration from version 4. Do not simply replace current shared files with snapshots: they also contain earlier Stage 6 code and would overwrite intervening changes.

Current schema version 4 omits the Stage 7 entities. The 3→4 migration intentionally leaves the old device-audio tables in an existing database, so upgrading a phone that ran code 12 does not erase those records. The online-only APK does not read or write them. Existing DataStore folder/download preferences are likewise left untouched and ignored.
