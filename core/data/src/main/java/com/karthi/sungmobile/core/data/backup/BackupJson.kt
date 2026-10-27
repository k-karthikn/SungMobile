package com.karthi.sungmobile.core.data.backup

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.settings.*
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.YouTubeLink
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import org.json.JSONArray
import org.json.JSONObject

/** Versioned, bounded JSON format. Validation is intentionally independent of the import writes. */
internal object BackupJson {
    private const val FORMAT = "sungmobile-backup"
    private const val VERSION = 1
    private const val MAX_BYTES = 25 * 1024 * 1024
    private val trackKey = Regex("youtube:[A-Za-z0-9_-]{11}")
    private val videoId = Regex("[A-Za-z0-9_-]{11}")

    fun encode(data: BackupSnapshot): ByteArray {
        val root = JSONObject()
            .put("format", FORMAT).put("version", VERSION)
            .put("settings", settingsJson(data.settings))
            .put("history", JSONObject().put("privateMode", data.history.privateMode)
                .put("listening", data.history.listening).put("searches", data.history.searches))
            .put("tracks", rows(data.tracks) { JSONObject().put("key", it.trackKey)
                .put("title", it.title).put("artist", it.artist).put("url", it.canonicalUrl)
                .put("artwork", it.artworkUrl).put("durationMs", it.durationMs) })
            .put("likes", rows(data.likes) { JSONObject().put("key", it.trackKey).put("likedAt", it.likedAt) })
            .put("playlists", rows(data.playlists) { item -> JSONObject()
                .put("id", item.playlist.id).put("name", item.playlist.name)
                .put("pinned", item.playlist.pinned).put("createdAt", item.playlist.createdAt)
                .put("updatedAt", item.playlist.updatedAt)
                .put("entries", rows(item.entries) { JSONObject().put("id", it.id)
                    .put("key", it.trackKey).put("position", it.position) }) })
            .put("smartPlaylists", rows(data.smartPlaylists) { JSONObject()
                .put("id", it.id).put("name", it.name).put("requireLiked", it.requireLiked)
                .put("minPlays", it.minPlays).put("recentDays", it.recentDays)
                .put("sort", it.sort).put("maxSongs", it.maxSongs)
                .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt) })
            .put("listening", rows(data.listening) { JSONObject()
                .put("id", it.id).put("passId", it.passId).put("key", it.trackKey)
                .put("epoch", it.epoch).put("listenedMs", it.listenedMs)
                .put("startedAt", it.startedAt).put("lastPlayedAt", it.lastPlayedAt) })
        return root.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_BYTES) { "Backup is too large to export (25 MB limit)." }
        }
    }

    fun decode(bytes: ByteArray): BackupSnapshot {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Choose a SungMobile backup under 25 MB." }
        try {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
                "This backup format is not supported."
            }
            val settings = readSettings(root.getJSONObject("settings"))
            val historyRow = root.getJSONObject("history")
            val history = HistoryPreferences(historyRow.getBoolean("privateMode"),
                historyRow.getBoolean("listening"), historyRow.getBoolean("searches"))
            val tracks = readRows(root, "tracks", 50_000) { row -> TrackEntity().apply {
                trackKey = row.key(); title = row.shortText("title", 500)
                artist = row.shortText("artist", 500); canonicalUrl = row.shortText("url", 2_048)
                val link = YouTubeLink.parse(canonicalUrl) as? YouTubeLink.Song
                require(link?.videoId == trackKey.removePrefix("youtube:")) { "Invalid song URL." }
                artworkUrl = row.optString("artwork").takeIf { it.isNotBlank() && it != "null" }
                    ?.also { require(it.length <= 2_048 && it.startsWith("https://")) }
                durationMs = row.opt("durationMs")?.takeUnless { it == JSONObject.NULL }?.let {
                    row.getLong("durationMs").also { duration -> require(duration in 0..86_400_000) }
                }
            } }
            val likes = readRows(root, "likes", 50_000) { row -> LikeEntity().apply {
                trackKey = row.key(); likedAt = row.safeTime("likedAt")
            } }
            val playlists = readRows(root, "playlists", 5_000) { row ->
                val playlist = PlaylistEntity().apply {
                    id = row.shortText("id", 120); name = row.shortText("name", 120)
                    require(name.isNotBlank())
                    pinned = row.getBoolean("pinned")
                    createdAt = row.safeTime("createdAt"); updatedAt = row.safeTime("updatedAt")
                }
                val entries = readRows(row, "entries", 50_000) { entry -> PlaylistEntryEntity().apply {
                    id = entry.shortText("id", 120); playlistId = playlist.id
                    trackKey = entry.key(); position = entry.getInt("position").also { require(it >= 0) }
                } }
                PlaylistBackup(playlist, entries)
            }
            val smart = readRows(root, "smartPlaylists", 5_000) { row -> SmartPlaylistEntity().apply {
                id = row.shortText("id", 120); name = row.shortText("name", 120)
                require(name.isNotBlank())
                requireLiked = row.getBoolean("requireLiked")
                minPlays = row.getInt("minPlays").also { require(it in setOf(0, 1, 2, 5)) }
                recentDays = row.getInt("recentDays").also { require(it in setOf(0, 7, 30, 90)) }
                sort = row.shortText("sort", 30).also { require(it in setOf("TITLE", "RECENT", "MOST_PLAYED")) }
                maxSongs = row.getInt("maxSongs").also { require(it in setOf(25, 50, 100)) }
                require(requireLiked || minPlays > 0 || recentDays > 0)
                createdAt = row.safeTime("createdAt"); updatedAt = row.safeTime("updatedAt")
            } }
            val listening = readRows(root, "listening", 100_000) { row -> ListeningEntity().apply {
                id = row.shortText("id", 120); passId = row.shortText("passId", 120)
                trackKey = row.key(); epoch = row.safeTime("epoch")
                listenedMs = row.getLong("listenedMs").also { require(it in 1..86_400_000) }
                startedAt = row.safeTime("startedAt"); lastPlayedAt = row.safeTime("lastPlayedAt")
                qualified = true
            } }
            require(playlists.sumOf { it.entries.size } <= 100_000) { "Backup has too many playlist entries." }
            val keys = tracks.map { it.trackKey }.toSet()
            require(keys.size == tracks.size &&
                likes.all { it.trackKey in keys } &&
                playlists.flatMap { it.entries }.all { it.trackKey in keys } &&
                listening.all { it.trackKey in keys }) { "Backup has missing or duplicate song records." }
            return BackupSnapshot(settings, tracks, likes, playlists, smart, listening, history)
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (_: Exception) {
            throw IllegalArgumentException("This is not a valid SungMobile backup.")
        }
    }

    private fun settingsJson(value: PortableSettings) = JSONObject()
        .put("theme", value.appearance.theme).put("colors", value.appearance.colors)
        .put("reduceMotion", value.appearance.reduceMotion)
        .put("artworkAccent", value.appearance.useArtworkAccent)
        .put("accentColor", value.appearance.accentColor)
        .put("ambientBackdrop", value.appearance.ambientBackdrop)
        .put("backdropFollowsMusic", value.appearance.backdropFollowsMusic)
        .put("colorScheme", value.appearance.colorScheme)
        .put("lrclib", value.lyrics.lrclibEnabled).put("lyricsSize", value.lyrics.textSizeSp)
        .put("lyricsCache", value.lyrics.cacheEnabled)
        .put("speedTenths", value.speed.tenths).put("preservePitch", value.speed.preservePitch)
        .put("smartShuffle", value.smartShuffle).put("relatedAutoplay", value.relatedAutoplay)
        .put("recommendationsEnabled", value.recommendationsEnabled)
        .put("recommendationFeedback", JSONObject().apply {
            value.recommendationFeedback.forEach { (key, action) -> put(key, action.name) }
        })
        .put("lyricOffsets", numberMap(value.lyricOffsets))
        .put("trackTrims", numberMap(value.trackTrims))

    private fun readSettings(row: JSONObject): PortableSettings {
        val appearance = AppearanceSettings(
            row.shortText("theme", 10).also { require(it in setOf("SYSTEM", "LIGHT", "DARK")) },
            row.shortText("colors", 10).also { require(it in setOf("BLUE", "WALLPAPER", "ACCENT")) },
            row.getBoolean("reduceMotion"), row.getBoolean("artworkAccent"), row.getInt("accentColor"),
            row.getBoolean("ambientBackdrop"), row.getBoolean("backdropFollowsMusic"),
            row.shortText("colorScheme", 20).also {
                require(it in setOf("NEUTRAL", "BALANCED", "VIBRANT", "EXPRESSIVE", "FAITHFUL"))
            })
        val size = row.getInt("lyricsSize").also { require(it in 18..34) }
        val speed = row.getInt("speedTenths").also { require(it in 5..20) }
        return PortableSettings(appearance,
            LyricsSettings(row.getBoolean("lrclib"), size, row.getBoolean("lyricsCache")),
            PlaybackSpeed(speed, row.getBoolean("preservePitch")),
            row.getBoolean("smartShuffle"), row.getBoolean("relatedAutoplay"),
            readNumberMap(row.getJSONObject("lyricOffsets"), -10_000..10_000),
            readNumberMap(row.getJSONObject("trackTrims"), -12..0),
            row.optBoolean("recommendationsEnabled", true),
            row.optJSONObject("recommendationFeedback")?.let(::readRecommendationFeedback).orEmpty())
    }

    private fun readRecommendationFeedback(row: JSONObject): Map<String, RecommendationFeedback> {
        require(row.length() <= 200) { "Backup has too much recommendation feedback." }
        return row.keys().asSequence().associateWith { key ->
            require(trackKey.matches(key)) { "Invalid recommendation song." }
            RecommendationFeedback.valueOf(row.getString(key))
        }
    }

    private fun numberMap(values: Map<String, Int>) = JSONObject().apply {
        values.forEach { (key, value) -> put(key, value) }
    }

    private fun readNumberMap(row: JSONObject, range: IntRange): Map<String, Int> {
        require(row.length() <= 500)
        return row.keys().asSequence().associateWith { key ->
            require(videoId.matches(key))
            row.getInt(key).also { require(it in range) }
        }
    }

    private fun <T> rows(items: List<T>, encode: (T) -> JSONObject) = JSONArray().apply {
        items.forEach { put(encode(it)) }
    }

    private fun <T> readRows(parent: JSONObject, name: String, limit: Int, decode: (JSONObject) -> T): List<T> {
        val items = parent.getJSONArray(name)
        require(items.length() <= limit) { "Backup has too many $name." }
        return (0 until items.length()).map { decode(items.getJSONObject(it)) }
    }

    private fun JSONObject.shortText(name: String, max: Int): String = getString(name).also {
        require(it.isNotBlank() && it.length <= max) { "Invalid $name in backup." }
    }

    private fun JSONObject.key() = shortText("key", 19).also {
        require(trackKey.matches(it)) { "Invalid song key in backup." }
    }

    private fun JSONObject.safeTime(name: String) = getLong(name).also { require(it >= 0) }
}
