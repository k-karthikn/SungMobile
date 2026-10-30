# Integrations

How Sung talks to each outside system, step by step.

---

## YouTube Music (`helper/catalog.py`)

**Browsing** uses `ytmusicapi.YTMusic(requests_session=True)` with every HTTP call wrapped to add a timeout (20 s; 8 s for lyrics).

| op | ytmusicapi call | Returns |
| --- | --- | --- |
| `home` | `get_home(limit=5)` | `sections: [{title, items}]` |
| `search` | `search(query, filter, limit ≤ 200)` | `items` |
| `album` | `get_album(id)` | title, artist, year, art, items (with disc numbers) |
| `playlist` | `get_playlist(id, limit ≤ 5000)` | title, art, items, total |
| `artist` | `get_artist(id)` | sections Songs / Albums / Singles / Videos / Related artists |
| `radio` | `get_watch_playlist(videoId, radio=True, limit=30)` | items (queued as autoplay) |
| `lyrics` | `get_watch_playlist(limit=1)` → `get_lyrics(browseId, timestamps=True)` | lines with start/end ms, or plain text; LRCLIB fallback if no timed lines |
| `link` | parse URL (youtube.com, music.youtube.com, m.youtube.com, youtu.be); `get_song(v)` or playlist | items |

Every item is normalised (`normalize()`), thumbnails rewritten from tiny sizes to `=w544-h544-l90-rj`. Watch-playlist items use the singular `thumbnail` key — the code handles both (a bug that once left radio songs without covers).

**Audio** uses `yt_dlp.YoutubeDL`:
- Video id validated against `[A-Za-z0-9_-]{11}`.
- Format string: Standard → `bestaudio[ext=webm]/bestaudio[ext=m4a]/bestaudio` (Opus ~130 kbps). Data saver → each with `[abr<=80]` then `worstaudio` (Opus ~67 kbps). On retry the container order flips.
- Options: quiet, no playlist, socket timeout 18 s, 2 retries, no cache dir, Node.js JS runtime, optional `cookiefile`.
- `op=buffer`: empty the private directory, download with `max_filesize` 32 MiB and a progress hook that aborts past 32 MiB, return `{file, seconds}`. If the file is too large, return the direct `url` (+headers) instead (played as a stream).
- `op=resolve`: no download, return `{url, headers, seconds}`. URLs carry `expire=`; Sung reuses a cached URL only if it has > 120 s left.

**Cookies:** user picks a Netscape `cookies.txt`; Sung checks the header line, copies it (≤ 2 MiB) to its data dir with 0600, and passes the path to yt-dlp. It never reads browser profiles.

**Autoplay:** when the queue ends with autoplay on and the last song is from YouTube, `radio` fetches similar songs, appends the new ones (not already queued), and continues.

**Updating:** if YouTube changes, `pip install --upgrade 'yt-dlp[default]' ytmusicapi` inside Sung's venv.

---

## Local files (`catalog.py` + backend)

- Allowed extensions: `.mp3 .flac .ogg .opus .m4a .aac .wav .aiff .aif .wma`.
- `scan-folders`: recursive `os.scandir`, no symlink following for dirs, canonical-path loop protection, compares each file's stamp with what the library knows, returns new/changed files, missing ones, and watch paths.
- `local-files` (4 files per process): `ffprobe` JSON (5 s timeout, `-protocol_whitelist file,crypto,data`) for format tags + stream info; merges stream tags (Vorbis/FLAC/Opus) and container tags (ID3); reads ReplayGain/R128.
- **Cover lookup per file:** case-insensitive sidecar search: `<song stem>.*` first, then `cover`, `folder`, `front`, `artwork` with `.gif .webp .mp4 .webm .jpg .jpeg .png`. Animated sidecars become `motionArt`; a 512 px JPEG poster is made with `ffmpeg` (frame 1). Otherwise the embedded picture (attached_pic stream) is extracted. Poster dir capped at 48 MiB; each poster ≤ 256 KiB.
- Re-import of the same path keeps the same id; "Locate file" re-points a missing track while keeping its id everywhere (queue, playlists, likes).
- Folder watching uses `QFileSystemWatcher` on directories and files; 1.5 s debounce; first rescan 5 s after launch.

---

## Subsonic / Navidrome (`src/subsonic.cpp`)

- **Connect:** validate http(s) URL without `/rest`, userinfo, query or fragment. `ping` with token auth: `u=<user>&t=md5(password+salt)&s=<salt>&v=1.16.1&c=Sung&f=json`, a fresh salt (UUID) per request.
- **Discover** after login: `getOpenSubsonicExtensions` (look for `songLyrics`, `formPost`), `getMusicFolders`, `getStarred2` (stars), `getPlaylists`.
- **Requests:** GET normally; if `formPost` is supported, POST the same query as `application/x-www-form-urlencoded` (needed for long playlist edits; GET limited to 7,500 bytes). No redirects followed (asks user to enter the final address). 20 s transfer timeout + 30 s hard timeout. Response ≤ 16 MiB. Error codes 40/41/50/60/70/20/30 mapped to friendly messages.
- **Browse modes → methods:** albums=`getAlbumList2(type=newest,size=100,offset)`, artists=`getArtists`, genres=`getGenres`, genre=`getSongsByGenre`, favorites=`getStarred2`, random=`getRandomSongs(100)`, playlists=`getPlaylists`, album=`getAlbum`, artist=`getArtist`, playlist=`getPlaylist`, search=`search3`. Optional `musicFolderId`.
- **Audio:** `stream?id&format=raw` (original) or `format=mp3&maxBitRate=128/192/320`, streamed to a file with a 512 MiB cap; rejects JSON/XML/HTML responses or empty files.
- **Covers:** `getCoverArt?id&size=600` built on demand from `sungcover://account/<identity>/<id>`; MPRIS gets a 256 px PNG copy.
- **Lyrics:** `getLyricsBySongId` (structured, prefers synced, converted to LRC) if the extension exists, else `getLyrics(artist,title)`.
- **Writes:** `star`/`unstar`, `setRating(0–5)`, `createPlaylist`, `updatePlaylist(songIdToAdd / songIndexToRemove / name)`, `deletePlaylist`, reorder = `createPlaylist(playlistId, songId…)` after re-fetching and checking nobody else changed it, `savePlayQueue(id…, current, position)`, `getPlayQueue`.
- **Scrobble:** `scrobble(submission=false)` on start ("now playing"), `scrobble(submission=true)` after **listened time** (not seek position) ≥ min(4 min, half the track, ≥ 1 s). Private mode sends nothing.

Tested against Navidrome 0.63.2 (README).

---

## Jellyfin (`src/jellyfin.cpp`)

- **Connect:** `POST /Users/AuthenticateByName {Username, Pw}` → `User.Id` + `AccessToken`. Every request carries `Authorization: MediaBrowser Client="Sung", Device="Sung Linux", DeviceId="<uuid>", Version="0.12.0", Token="…"`. The **token** (not the password) goes to the keyring; restore uses `GET /Users/Me`.
- **Discover:** `/Users/<id>/Views` (music libraries), all favourite audio (`Filters=IsFavorite`), playlists.
- **Paging:** `fetchRows` follows `StartIndex/Limit=100` until done when a whole collection is needed (album, playlist), capped at 20,000 items.
- **Browse:** `Items` with `IncludeItemTypes` = MusicAlbum / Audio / Playlist, `Artists`, `MusicGenres`, `GenreIds`, `ArtistIds`, `SearchTerm`, `SortBy=Random`, `ParentId`=library.
- **Playlist permissions:** `GET /Playlists/<id>/Users/<user>` → `CanEdit`; resolved for every listed playlist one at a time.
- **Audio:** `/Audio/<id>/stream?Static=true` (original) or `/stream.mp3?AudioCodec=mp3&AudioBitRate=…&Container=mp3`.
- **Covers:** `/Items/<id>/Images/Primary?maxWidth=600&quality=90` (falls back to the album's image).
- **Lyrics:** `/Audio/<id>/Lyrics` (Start ticks → LRC; 404 = no lyrics).
- **Favourites:** `POST/DELETE /Users/<u>/FavoriteItems/<id>`. No star ratings.
- **Playlists:** create `POST /Playlists`, rename `POST /Playlists/<id> {Name}`, add `POST /Playlists/<id>/Items?Ids=`, remove `DELETE …/Items?EntryIds=`, reorder/replace `POST /Playlists/<id> {Ids:[…]}`, delete `DELETE /Items/<id>`.
- **Playback reporting** (Jellyfin manages its own play counts): `Sessions/Playing` on start, `Sessions/Playing/Progress` every 10 s of position and on pause/seek, `Sessions/Playing/Stopped` on change/stop; queued and sent one at a time, consecutive progress reports coalesced. `PlayMethod` DirectPlay or Transcode.
- No server queue (use Sung sessions). 10.11 removes duplicate playlist adds; 12.0 keeps them.

---

## Apple Music through Cider (`src/cider.cpp`)

Why: Apple only streams to its own players. Cider (cider.sh) is an Apple Music client with a local REST API.

- **Setup:** in Cider, Settings → Connectivity → Manage External Application Access → create token. In Sung, provider Cider, address `http://localhost:10767`, paste token (can be empty if Cider allows tokenless).
- **Headers:** `apptoken: <token>` (no "Bearer"). Timeouts 4 s for playback calls, 20 s for Apple API pass-through.
- **Connect:** `GET /api/v1/playback/active`, then `amapi /v1/me/storefront` (country code; default `us`).
- **Browse** via `POST /api/v1/amapi/run-v3 {path}` (Apple Music API):
  - albums/artists/playlists: `/v1/me/library/...?limit=100&offset=`
  - album/playlist detail: library (`l.`/`p.` ids) or catalogue `/v1/catalog/<sf>/albums|playlists/<id>` + `/tracks`, following `next` links up to 5,000 rows
  - artist: `/albums`
  - search: `/v1/catalog/<sf>/search?term&types&limit=25&offset`
  - Discover: `/v1/catalog/<sf>/charts?types=songs&limit=50`
- **Play:** `POST /api/v1/playback/play-item {type:"songs", id:<catalogId>}` or `play-item-href {href:"/v1/me/library/songs/<id>"}`; then `pause`, `play`, `seek {position seconds}`, `volume {volume}` (debounced 120 ms).
- **Watching:** poll `now-playing` + `is-playing` (1 s playing / 2 s paused & visible / off hidden). Song "ours" if `playParams.id/catalogId` or name matches. Commands get a 3 s settling window. Start timeout 20 s. 3 failed polls → give up with a message. Resumed songs that start from 0 are seeked to the saved position.
- **Autoplay:** read `GET /playback/autoplay`; if on, toggle off and remember (`cider/restoreAutoplay`), restore when released.
- **Limits:** no speed, crossfade or normalisation for Apple songs; playlists/favourites edited in Cider; lyrics from LRCLIB; visualizer via `parec` on the default monitor (desktop may show Sung as "recording").

---

## LRCLIB (`catalog.py lyric_fallback`)

`GET https://lrclib.net/api/get?track_name&artist_name&duration[&album_name]`, 8 s timeout, ≤ 1 MiB. Accept only if normalised title and artist are equal and duration within 2 s, and synced lyrics exist (≤ 256 KiB). HTTP 429 → write `retry-after` timestamp and skip until then. Can be turned off (Settings → Find missing lyrics on LRCLIB). Sends title, artist, duration (and album).

---

## Apple public artwork + MusicBrainz / Cover Art Archive (`online_artwork.py`)

See METHODS_AND_TECHNIQUES §15. Triggered 1 s after a YouTube (or Cider) song starts playing with the window visible, only if wanted (animated covers on, or the cover is a video frame and "Album covers for music videos" is on). Result: still cover remembered per video id (2,000), animated MP4 in cache. "Find cover again" forgets and retries.

---

## ListenBrainz (`src/scrobbler.cpp`)

- Server default `https://api.listenbrainz.org` (any compatible HTTPS server allowed; changing server signs out).
- Sign in: `GET /1/validate-token` with `Authorization: Token …` → user name. Token stored only in keyring.
- `playing_now` (no timestamp, fire-and-forget) on start; a **listen** once played ≥ min(half, 4 min), never for tracks < 30 s.
- Listens queued (≤ 500, persisted in settings), sent oldest first in batches of 50 (`single` or `import`), retried every 60 s.
- Payload: `track_name`, `artist_name`, `release_name`, `additional_info {media_player:"Sung", submission_client:"Sung", duration}`.

---

## MPRIS (`src/mpris.cpp`)

- Service `org.mpris.MediaPlayer2.sung`, path `/org/mpris/MediaPlayer2`.
- Root: CanQuit, CanRaise, Identity "Sung", DesktopEntry "sung", SupportedUriSchemes ["https"]; Raise → raise window.
- Player: PlaybackStatus, LoopStatus (None/Playlist/Track ↔ repeat 0/1/2), Rate (0.5–2; 0 pauses), Shuffle, Volume, Position (µs), Metadata (`mpris:trackid` = `/org/mpris/MediaPlayer2/track/<token>`, length, artUrl (600 px or server PNG), title, artist, album, url), CanGoNext/Previous/Play/Pause/Seek/Control; methods Next, Previous (keep paused/stopped state), PlayPause, Play, Pause, Stop, Seek (past end → Next), SetPosition (checks track id), OpenUri (plays a YouTube link).
- Emits `PropertiesChanged` on track/playback/settings changes and `Seeked`.

## Desktop notifications (`src/playbacknotifier.cpp`)

Optional ("Track notifications"). `Notify("Sung", replaces_id, "sung", title, artist(escaped), [], {desktop-entry:"sung", category:"music", transient, suppress-sound, urgency:0}, 5000)` asynchronously; queues one pending; replaces its own previous notification; closes on private mode; resets if the notification daemon restarts.

## Keyring

All via `secret-tool store|lookup|clear --label=… application sung account <key>` with a 15 s timeout. Availability = `secret-tool` on PATH.

## Audio devices

`QMediaDevices::audioOutputs()`; chosen id saved base64; disappearing device → fall back to default and forget (it can't "steal" playback when it returns). Headphone unplug: `pactl subscribe` events (sink/card) debounce 120 ms → `pactl -f json list sinks` → if the active port of the current sink changes away from a headphone/headset port → pause.
