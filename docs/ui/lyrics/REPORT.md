# Lyrics and settings UI update

`0.12.2-lyrics-ui`, code 22, follows the Search UI code 21. The Lyrics screen now has a clear header, a compact card with real track artwork and provider attribution, grouped timing controls, a strong current-line highlight with a slim accent, a return-to-current-line button after manual scrolling, and an easier-to-read plain lyrics surface. The owner supplied a light Lyrics reference; the implementation uses Sung’s existing theme colors in light and dark mode. The line presentation lives in `LyricsContent.kt`; loading, seek and follow behavior remain in `LyricsScreen.kt`. Line color changes honor Reduced Motion.

Appearance keeps the existing theme and color choices and groups Motion, App updates, Lyrics and About with a shared settings surface. The lyric cache, LRCLIB, text size, notification and updater controls retain their behavior. A shared confirmation dialog gives lyric-cache, queue-clear and library confirmation actions consistent shape and buttons. Other dialogs with custom fields and previews remain purpose-built so their actions are preserved.

Application ID, signing certificate and Room schema 5 are unchanged. This pass does not claim a scrolling performance fix; device profiling remains separate. Manual phone acceptance is pending; see `Stages/Lyrics_UI_Results/`.

Verification: 15 focused host tests passed, no failures or skips; lint 0 errors and 20 warnings; debug APK assembled and prior signer verified. Evidence is in `Stages/Lyrics_UI_Results/verification/`.
