# Launcher icon placement

`0.12.3-icon`, code 23. The original `res/drawable/icon.png` is unchanged. Android adaptive launcher icons now use that source through one 12 dp inset foreground for both normal and round masks. This keeps the music note inside Android's visible safe area and avoids the previous zoomed foreground image. The monochrome layer that pointed to an opaque color bitmap was removed so themed launchers do not render it as a solid shape.

The existing package ID and debug signer remain. No UI, playback, storage or database behavior changes. The source logo has not been regenerated or recolored. Owner phone check is needed on the POCO launcher; no device screenshot or installation was performed by Codex.

Verification: assembleDebug and lintDebug passed; lint 0 errors and 22 warnings. APK metadata, adaptive-icon XML and retained certificate were checked. Evidence is in `Stages/Icon_Results/verification/` (files removed 30 October 2026).
