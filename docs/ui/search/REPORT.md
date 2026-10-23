# Search UI update

`0.12.1-search-ui`, code 21. The Search page follows the supplied references while keeping Sung's theme colors and existing search behavior. The input has a clear action; category chips use icons; manual search/link opening remains prominent. Recent searches use compact rows with individual removal. The blank state uses a small theme-colored illustration made from the existing search icon.

Song results reuse `SungSongListItem` with direct heart and a plus menu for playlist and queue actions. Other result kinds retain their open/collection behavior. The page still supports typed search debounce, YouTube link opening, category selection, cancellation, paged results, provider notices, error/retry and back navigation. Search's input/header, result rows and empty state remain in separate focused files; the ViewModel and playback queue logic were not rewritten.

Material 3 Expressive buttons and toggle shapes use the theme's motion scheme, including Reduced Motion. Result rows remain lazy and keyed by provider ID. This pass does not claim to fix the separately reported scrolling lag; phone profiling remains later work.

Manual phone acceptance pending. See `Stages/Search_UI_Results/` for APK, checks and verification evidence.

Verification: 20 focused nonvisual host tests passed with 0 failures and 0 errors; lint 0 errors and 18 warnings; APK assembled and signer verified. Evidence is in `Stages/Search_UI_Results/verification/`.
