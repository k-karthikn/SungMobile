# Player and Queue sheet update

Build `0.12.4`, code 24, uses the production package `com.karthi.sungmobile` and the dedicated production signing identity from code 23. There is no database or playback-engine change.

The player keeps its previous/current/next artwork, title, library actions and secondary playback features. The seek control retains Material Slider semantics and a full touch target while drawing a 12 dp-high active wave with smaller amplitude and fewer path samples. −10/+10 actions moved beside the timestamps. The main transport row now exposes Repeat, Previous, Play/Pause, Next and Shuffle, with theme-aware selection color and motion that respects Reduced Motion.

Queue moved from a separate player navigation action into a persistent Material 3 bottom sheet. The 88 dp peek has a tap target and drag handle. Standard sheet gestures expand/collapse it; Back collapses an expanded sheet before leaving the player. Expanded content includes a compact current-song summary and reuses the existing occurrence-aware queue list, including jump, Move up/down, Remove, Undo, Clear upcoming and Save queue as playlist. The list remains lazy, and queue movement actions remain in each row's options menu. Row drag-to-reorder was not added because only sheet dragging was requested; the row menu remains the accessible reorder path.

The player and design host tests cover transport callbacks, queue swipe/tap and existing queue actions. Release build, lint and APK verification are recorded in `Stages/Player_Queue_UI_Results/` (files removed 30 October 2026). The owner will perform phone visual and playback checks. Scrolling lag elsewhere in the app remains a separate investigation.
