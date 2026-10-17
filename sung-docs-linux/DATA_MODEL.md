# Data model

Sung moves data around as **`QVariantMap`s** (JSON-like dictionaries) rather than C++ structs. A "track" is a map; a list of tracks is a `QVariantList`. QML reads these maps directly. This keeps the helper JSON, the saved library and the UI all in one shape.

## 1. Item kinds

`kind` field values: `song`, `video`, `album`, `playlist`, `artist`, `genre` (server), `local` (a local playlist), `smart` (mix or smart playlist entry), `local-album`, `local-artist`.

## 2. Track map — common fields

| Field | Meaning |
| --- | --- |
| `id` | Unique, stable identity (format depends on source, see §3) |
| `kind` | See above |
| `title`, `artist`, `album` | Display strings |
| `artistId`, `albumId` | Source ids for "Go to artist/album" and same-album detection |
| `art` | Cover URL (https, file://, or `sungcover://…` for servers) |
| `seconds` | Duration in seconds (preferred) |
| `duration` | `"m:ss"` string (YouTube) |
| `discNumber`, `trackNumber`, `year` | Ordering and details |
| `available` | `false` = greyed out (missing file / unavailable) |
| `source` | `""` (YouTube or local), `subsonic`, `jellyfin`, `cider` |
| `_queueOrigin` | Only in the queue: `manual`, `collection`, `autoplay` |

### YouTube (from `catalog.py normalize`)
`id`=`videoId` (11 chars) for songs or `browseId`/`playlistId` for collections, `videoId`, `browseId`, `explicit`, `art` (Google image URL rewritten to 544 px).

### Local file (from `catalog.py local_files`)
`id`=`local_<sha256(path)>`, `videoId`=`""`, `localPath`, `localStamp` (`mtime_ns:size[|cover:mtime:size]`), `albumArtist`, `genre`, `composer`, `codec`, `sampleRate`, `bitrate`, `channels`, `bitDepth`, `art` (cached 512 px JPEG poster `file://…?v=stamp`), `motionArt` (animated sidecar URI), `replaygainTrackGain`, `replaygainAlbumGain`, `r128TrackGain`.

### Subsonic (`Subsonic::item`)
`id`=`sub_<sha256(identity\nkind\nremoteId)>`, `server`=identity, `remoteId`, `serverSong`, `rating` (0–5), `count`, `editable` (playlists you own), `art`=`sungcover://account/<identity>/<coverArtId>`.

### Jellyfin (`Jellyfin::item`)
`id`=`jf_<…>`, same as Subsonic plus `entryId` (PlaylistItemId), `deletable`, `art`=`sungcover://jellyfin/<identity>/<imageItemId>`, seconds from `RunTimeTicks / 10^7`.

### Apple Music via Cider (`Cider::item`)
`id`=`am_<…>`, `remoteType`, `catalogId` (playable catalogue id), `art` = Apple template URL filled with 600×600, `available=false` for library songs Apple no longer carries.

### Local groups (`Backend::localGroups`)
`id`=`local-group:<sha256(kind+key)>`, `kind` `local-album`/`local-artist`, `groupKey` = casefolded `albumArtist␟album` (or `artist␟`), `count`, `art`, `year`.

## 3. Identity rules

- **Server identity** = `sha256(address + "\n" + username)` (Subsonic) or `sha256(address + "\n" + userId)` (Jellyfin) or `sha256("cider\n" + address)`. **Never contains credentials.**
- Track id = prefix + sha256(identity + kind + remote id), so the same song on two servers or two accounts never collides.
- A valid id for lyric files must match `^(?:[A-Za-z0-9_-]{11}|(?:local_|sub_|jf_|am_)[a-f0-9]{64})$`.
- `owns(track)` = connected + same source + same server identity + has remoteId.

## 4. `library.json` (in `~/.local/share/Sung/sung/`)

```jsonc
{
  "musicFolders": ["/home/me/Music"],            // ≤ 64 canonical paths
  "localTracks":  [ {track…} ],                  // imported files
  "favorites":    [ {track…} ],                  // likes (newest first)
  "history":      [ {track…} ],                  // ≤ 200, one row per song, newest first
  "lastPlayed":   { "<id>": 1727350000 },        // seconds; 0 = played, date unknown; pruned to saved songs
  "plays":        [ {"at":…, "id":…, "title":…, "artist":…, "album":…, "seconds":…} ], // ≤ 20,000, oldest first
  "listeningDays":{ "2026-09-26": [3600, 14] },  // [seconds, plays], ≤ 3,660 days
  "playlistVersions": { "<playlistId>": [ {"at":…, "tracks":[…]} ] }, // ≤ 12 versions, ≤ 5,000 rows each list
  "sessions":     [ {"id","title","queue":[…],"index","position","rate","shuffle","repeat","autoplay"} ], // ≤ 20 × 2,000 songs, ≤ 8 MiB JSON
  "playlists":    [ {"id":"<uuid>","title":"…≤120","tracks":[…],"customCover":"file://…"}
                  , {"id":"<uuid>","title":"…","rules":{…},"tracks":[]} ],   // smart playlist (≤ 32)
  "pins":         [ {"kind","id","title","browseId","art","artist","source","server","remoteId"} ], // ≤ 24
  "lyricOffsets": { "<id>": -350 },              // ms, ±10,000
  "queue":        [ {track… + "_queueOrigin"} ],
  "index":        3,
  "position":     81234                          // ms
}
```

Written compact with `QSaveFile` (atomic rename), permissions 0600. If parsing fails, Sung refuses to save for the session so the original stays intact.

### Smart playlist rules

```jsonc
{ "artist":"", "title":"", "album":"",        // case-insensitive "contains", ≤ 120 chars
  "source":"any|local|youtube|subsonic",
  "yearFrom":0, "yearTo":0,                    // 0 = open end; songs without a year fail a year rule
  "minSeconds":0, "maxSeconds":0,              // ≤ 36,000; songs without a length fail a length rule
  "likedOnly":false,
  "days":0 }                                   // 0 any · -1 never played · 7/30/90/365 = not played in the last N days
```
Candidates = local files + likes + all ordinary playlists, de-duplicated by id.

### Library export (`Export library`)

`{ "sung": 1, "musicFolders", "localTracks", "favorites", "playlists", "pins", "lyricOffsets" }` — no history, stats, credentials or server URLs with tokens. Import accepts ≤ 10 MB, merges without duplicating.

### M3U export

```
#EXTM3U
#PLAYLIST:<title>
#EXTINF:<seconds>,<artist> - <title>
/absolute/path/to/file.flac
```
Only local files; streamed songs are counted as skipped. Import resolves relative paths against the playlist's folder, skips `://` entries, ≤ 5,000 entries, ≤ 4 MiB file.

## 5. Settings (`~/.config/Sung/sung.conf`, QSettings)

| Key | Default | Notes |
| --- | --- | --- |
| `volume` | 0.65 | 0–1 |
| `playbackRate` | 1.0 | 0.5–2.0 |
| `preservePitch` | true | Qt ≥ 6.10 |
| `shuffle` / `repeat` / `autoplay` | false / 0 / true | repeat 0 off, 1 all, 2 one |
| `crossfadeSeconds` / `gapless` / `prepareNext` | 0 / true / true | 0–12 s |
| `volumeNormalization` | false | |
| `resumeLongTracks` | true | ≥ 1,200 s tracks |
| `sleepFade` | true | last 30 s |
| `pauseOnDisconnect` | false | |
| `streamingQuality` | `standard` | or `saver` |
| `keepPlayedMb` | 1024 | 0 = off (UI offers up to 16 GB; code clamps to 65,536 MB) |
| `cookies` | — | path to stored cookies.txt |
| `audioDevice` | "" | base64 device id; removed if device disappears |
| `theme` | `system` | `light`, `dark` |
| `accentColor` | "" | `#rrggbb` |
| `artworkAccent` | false | |
| `colorVariant` / `colorContrast` | `tonalSpot` / 0 | |
| `motion` / `motionScheme` | true / `expressive` | |
| `precisePointer` | false | |
| `ambientBackdrop` / `backdropPulse` | true / true | |
| `animatedArtwork` / `onlineArtwork` / `albumCovers` | true / true / true | |
| `motionQuality` | 0 | 0 auto, 1080, 1920, 2160 |
| `posterLyrics` / `keepCompletedLyrics` / `lyricsFallback` | false / true / true | |
| `lyricTextSize` | 25 | 20–32 |
| `volumeStep` | 5 | 1, 2, 5, 10 % |
| `compactDensity` / `sidebarNavigation` | false / false | |
| `startPage` | `home` | `files`, `server`, `favorites` |
| `homeOrder` / `hiddenHomeSections` | — | ≤ 64 titles |
| `viewLayouts` | — | map viewKey → {density, mode}, ≤ 64 |
| `recentSearches` | — | ≤ 12 |
| `typeAheadJump` / `watchMusicFolders` | true / true | |
| `trackNotifications` / `miniPinned` / `onboarded` | false / false / false | |
| `artworkChoices` | — | songId → file URI or `disabled`, ≤ 1,000 |
| `artworkFit` | — | sha256(album key) → true, ≤ 2,000 |
| `videoCovers` | — | videoId → Apple/CAA cover URL or "" (miss), ≤ 2,000 then reset |
| `loudness/<id>` + `loudnessOrder` | — | measured dBFS, ≤ 2,000 (LRU) |
| `trim/<id>` + `trimOrder` | — | ±12 dB, ≤ 2,000 |
| `resume/<id>` + `resumeOrder` | — | ms, ≤ 400 |
| `server/provider` | `subsonic` | `jellyfin`, `cider` |
| `subsonic/{address,username,remember,folder,scrobble,bitrate}` | | bitrate 0 (original), 128, 192, 320 |
| `jellyfin/{device,address,username,remember,folder,scrobble,bitrate}` | | device = random UUID |
| `cider/{address,remember,tokenless,restoreAutoplay}` | | |
| `scrobbler/{enabled,server,account,queue}` | | queue ≤ 500 pending listens |

QML-only settings (`Settings` from QtCore): `Listening/{layout,autoHide,coverflow,motionLyrics}`, `Window/{width,height,panelWidth}`, `Navigation/expanded`.

## 6. Secrets (keyring via `secret-tool`)

| Label | Attributes | Value |
| --- | --- | --- |
| Sung music server | `application=sung account=<subsonic identity>` | Subsonic password |
| Sung Jellyfin | `account=jellyfin_<sha256(address\nuser)>` | Jellyfin access token (not the password) |
| Sung Cider | `account=cider_<sha256(address)>` | Cider app token |
| Sung listening token | `account=scrobbler` | ListenBrainz token |

If no keyring is available, logins work for the session only.

## 7. Cache files

| Path (under `~/.cache/Sung/sung/`) | Contents | Limit |
| --- | --- | --- |
| `audio-XXXXXX/` | Temporary whole-song buffers | deleted after use |
| `songs/` | Kept songs, `<32 hex>.<ext>` | `keepPlayedMb` budget, LRU by mtime |
| `art/` | Artwork HTTP disk cache | 48 MiB |
| `motion-art/` | `<albumId>[-<quality>].mp4`, `<sha256>.json` lookup records, `retry-after-v2` | 256 MiB MP4, 256 records |
| `lyrics/retry-after` | LRCLIB rate-limit marker | |
| `desktop-art-XXXXXX/cover.png` | Server cover for MPRIS (256 px) | temp |
| `cover-edit/preview.jpg` | Playlist cover crop preview (≤ 1024 px) | 1 file |

## 8. Hard limits in one place

| Thing | Limit |
| --- | --- |
| Back stack | 12 views |
| Remembered view filter/sort and scroll positions | 32 each |
| History | 200 songs |
| Recently played panel | 60 |
| Play log / calendar days | 20,000 plays / 3,660 days |
| Pins | 24 |
| Smart playlists | 32 |
| Music folders | 64 |
| Import batch to helper | 4 files (≤ 16,000 bytes of paths) per process |
| Files per import | 10,000 |
| Folder scan | 100,000 entries, depth 64, 10,000 new files, 1 MiB paths |
| Watched paths | 4,096 dirs + 10,000 files (README); code caps desired set at 14,160 |
| Sessions | 20 × 2,000 songs, 8 MiB |
| Playlist versions | 12 per playlist, 5,000 rows |
| Playlist title | 120 chars |
| Search results | 30 per page, ≤ 200 |
| YouTube playlist | 100 per page, ≤ 5,000 |
| Jellyfin opened collection | 20,000 items |
| Server playlist edit | 5,000 songs per request; GET body ≤ 7,500 bytes unless form POST |
| YouTube buffer | 32 MiB per song |
| Server buffer | 512 MiB per song |
| Animated cover file | 128 MiB / 4096×4096 local; 16 MiB (standard) / 64 MiB (large) online |
| Local cover image for RoundedArt | 2 MiB, 4096 px |
| Network cover | 4 MiB, 10,000 px |
| Imported lyrics | 256 KiB each, 8 MiB total |
| LRC lines | 4,000 |
| Helper stdin | 16 MiB |
| Server JSON response | 16 MiB |
| Error message shown | 350 chars |
| Library import file | 10 MB |
| Cookie file read | 2 MiB |
