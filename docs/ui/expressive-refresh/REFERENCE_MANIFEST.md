# Reference image manifest

These 14 user-provided PNGs were copied without editing on 24 October 2026. They are visual references, not product requirements. The strategy document decides which visible behaviors map to existing SungMobile features.

| Area | Reference | Original attachment | SHA-256 |
| --- | --- | --- | --- |
| Library | [liked-compact-detail](references/library/liked-compact-detail.png) | `codex-clipboard-71cf80ff-2c50-4907-a575-375b238ad48b.png` | `029b24d69d8be9007908406ce1f01a80689ec9962fa70ad8b3137475f2baa8ac` |
| Home | [home-discovery-shell](references/home/home-discovery-shell.png) | `codex-clipboard-401a6d3d-3b75-492e-b502-d52bd7577b3f.png` | `0158f1901ff7e482824f6cfd016be058c7c3587cd58a385ac6f496ca97b6764b` |
| Library | [mixes-tab](references/library/mixes-tab.png) | `codex-clipboard-89154e9a-0e65-483b-8717-e85d27f5ea62.png` | `6b05dbf14331ca2c992ed05117c712e3c42b6c98c0df1b9bd1dfb005c4c705db` |
| Library | [liked-full](references/library/liked-full.png) | `codex-clipboard-cb318649-8fd2-4afc-9612-49cd5069656e.png` | `23db479f594d896822939a48116a3bb2f774e72b44d45e395fcfc8fc19bbdaef` |
| Shared | [mini-player-menu-context](references/shared/mini-player-menu-context.png) | `codex-clipboard-2b1f04f1-6898-4322-9163-d92ed9fbe6cd.png` | `e0a709310268f6df3324d8b35131a8f232ad6d4e573870faf95e437a15f4b7c2` |
| Shared | [expressive-play-shape](references/shared/expressive-play-shape.png) | `codex-clipboard-0c1e136a-5133-4331-be5a-d060bce5f62f.png` | `1df33231979ee0e1c4dd2a3286145dd95dff2bd73ec8632cebbf07839218c26e` |
| Shared | [mini-player-loading](references/shared/mini-player-loading.png) | `codex-clipboard-e3e7ea9e-4944-4402-832c-642a16e187d0.png` | `7f36067aedd99bfd9879d4e838c39acb614a366984c41f7321fdd71dae00e026` |
| Lyrics | [synced-lyrics-main](references/lyrics/synced-lyrics-main.png) | `codex-clipboard-7534e571-597b-4f9e-addb-e0a75125b016.png` | `cc328aaac56f41073ba33ae773ec31de61475b65839fe5f114aa11361f0ed20a` |
| Lyrics | [synced-lyrics-active-line](references/lyrics/synced-lyrics-active-line.png) | `codex-clipboard-32c33ac5-da26-46cb-afce-d27438313106.png` | `21ff4811fe8129f657c40f8b48376ccb0c31fa85dbb982ffcca2df42ba13025b` |
| Shared | [mini-player-menu-detail](references/shared/mini-player-menu-detail.png) | `codex-clipboard-295c80b8-c80e-4abd-affc-928a7bd3bd07.png` | `e652cf2f1dc31bca5972382e8c9d661dcfcf341f5c56da15b556d1dc2bad9b31` |
| Shared | [mini-player-progress-detail](references/shared/mini-player-progress-detail.png) | `codex-clipboard-70369bb9-9408-40a5-a0eb-078334e37a56.png` | `8fef45390bd7720f24fdc05120d2d8f2ddc14b34bdc9714ff5548847b9a77998` |
| Library | [history-tab](references/library/history-tab.png) | `codex-clipboard-77970b08-1aa1-4b5b-a62d-69b21d40d617.png` | `81da00db3f147d463706b8c8b3a9ad7a2ee63113c6b29111e085328a18153ace` |
| Search | [search-empty](references/search/search-empty.png) | `codex-clipboard-b0cbcc49-9cd8-4644-989d-08e4f960c279.png` | `a26e973999e4facaa94a58204a913358434c4ca5c28679617f86f316bf891143` |
| Library | [song-context-menu](references/library/song-context-menu.png) | `codex-clipboard-eda4373f-1e94-4441-8300-af14fcf1d7ad.png` | `b5064e3181844fdde27e24ef27ce20071e9ee8a51ef670d8502e585796cda18a` |

## Reading notes by reference

- **Home / home-discovery-shell:** compact top actions, content shelves, shared two-row mini-player and short three-destination navigation. The pictured mood chips and chart names are desktop content; map only to current mobile mixes, pinned playlists and recently played.
- **Library / liked-compact-detail and liked-full:** text tabs with an underline, a play button plus down-arrow action menu, filtering/sorting, selected row tint and current-track indication. Ignore the Local files tab.
- **Library / mixes-tab:** a category page for generated/curated mixes. Use SungMobile's two existing local mixes and saved Smart playlists; do not claim the screenshot's exact preset mixes exist.
- **Library / history-tab:** The screenshot calls this History; in SungMobile, the existing recent-listening collection is clearer as **Recents** beside the other Library tabs. Keep its sort and bulk play actions. Place history/privacy controls in Settings.
- **Library / song-context-menu:** preserve current playback, Like, playlist and details actions. File path/location items are not applicable to the online-only app.
- **Search / search-empty:** compact search field, selected category, quiet empty state, shared bottom chrome. Four current catalog kinds remain; no Videos category.
- **Lyrics / synced-lyrics-main and synced-lyrics-active-line:** generous line spacing, a strong current-line treatment and unobtrusive controls. Only line-level sync is available; the mockup's decorated words must not imply word-level timestamps.
- **Shared / mini-player-progress-detail and mini-player-loading:** compact title/transport row plus elapsed/progress/duration row. Handle buffering, unknown duration, pause and completion as distinct playback states.
- **Shared / mini-player-menu-context and mini-player-menu-detail:** More action placement and selected-state tint/check. Omit audio-output routing; expose the existing per-song volume trim.
- **Shared / expressive-play-shape:** an abstract scalloped play/pause container, used sparingly as a prominent state marker rather than on every control.
