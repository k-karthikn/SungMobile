# Library UI implementation

`0.11.2-library-ui`, code 19. Application ID and debug signing identity retained; Room schema 5 unchanged. Owner accepted player UI code 18 before this work. Library phone acceptance pending.

LibraryHeader and LibrarySelectionBar live in feature/library/components; SungFilterField lives in the shared design system. Song rows reuse SungSongListItem, with expressive heart toggles and an options menu. Album/YouTube collection rows also use that presentation; remote playback actions remain disabled when disconnected. Playlist cards use a library icon instead of fabricated artwork because the local playlist summary has no cover field.

Check boxes appear only in selection mode. Select loaded, Like/Unlike, Add to playlist, Play next, Add to queue and playlist removal are retained. Back/Done exits selection. Playlist occurrence IDs still drive remove/reorder. Filtering, sorting, pagination, empty/error/loading states and clear confirmations are preserved.

History/privacy, Smart playlists and Listening statistics remain in the Library overflow menu. Cleanup/versions remains on playlist details. Sessions has direct category access and returns to Library. Its save, preview/paused restore, rename and delete workflows remain intact. Advanced management screens retain their existing detailed layouts with a consistent Back icon; this pass does not rewrite their logic.

Validation and evidence: Stages/Library_UI_Results/README.md. Search remains a separate upcoming UI slice. No claim of a phone scrolling performance fix.
