# Library UI slice — 21 October 2026

The owner accepted player UI code 18 and requested Library next, ahead of Search. Use the supplied Library reference as layout guidance and retain Sung's existing theme and motion settings.

- Library header with Liked, Playlists, Recents and Sessions pills; scroll category/actions horizontally when text or screen width requires it.
- Contextual local filtering, existing Latest/Playlist order and A–Z sorting, Play all, Shuffle and explicit selection mode.
- Shared song rows with immediate Like/Unlike and options; selection mode uses checkboxes and row taps to select rather than play. Preserve all bulk actions and occurrence-based playlist editing.
- Playlist cards and detail styling; preserve pin/rename/delete, cleanup/versions and confirmations. Keep History & privacy, Smart playlists and Listening statistics in Library options.
- Direct Sessions navigation; retain restore preview, paused restore, naming and delete confirmation. Match collection/album rows to the shared layout without changing source or pagination behavior.
- Existing M3 press/selection motion, list placement animation gated by Reduced Motion, bounded artwork sizes and lazy lists. No new decorative continuous animation.

No database migration, signing change, source change, download feature, device installation or Search redesign. Manual APK acceptance stays with the owner. Phone scrolling performance remains a separate deferred investigation.
