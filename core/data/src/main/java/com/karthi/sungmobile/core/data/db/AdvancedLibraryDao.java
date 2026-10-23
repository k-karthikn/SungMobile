package com.karthi.sungmobile.core.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;
import java.util.List;

@Dao
public interface AdvancedLibraryDao {
    @Query("SELECT * FROM smart_playlists ORDER BY updatedAt DESC, id") List<SmartPlaylistEntity> smartPlaylists();
    @Query("SELECT * FROM smart_playlists WHERE id = :id") SmartPlaylistEntity smartPlaylist(String id);
    @Upsert void saveSmart(SmartPlaylistEntity playlist);
    @Query("DELETE FROM smart_playlists WHERE id = :id") void deleteSmart(String id);
    @Query("SELECT t.*, CASE WHEN l.trackKey IS NULL THEN 0 ELSE 1 END AS liked, " +
        "COALESCE(h.plays, 0) AS plays, COALESCE(h.lastPlayedAt, 0) AS lastPlayedAt " +
        "FROM library_tracks t LEFT JOIN library_likes l ON l.trackKey = t.trackKey " +
        "LEFT JOIN (SELECT trackKey, COUNT(*) AS plays, MAX(lastPlayedAt) AS lastPlayedAt " +
        "FROM listening_events WHERE qualified = 1 GROUP BY trackKey) h ON h.trackKey = t.trackKey " +
        "WHERE t.trackKey LIKE 'youtube:%'") List<SmartCandidateRow> candidates();
    @Query("SELECT * FROM playlist_revisions WHERE playlistId = :playlistId ORDER BY createdAt DESC, id DESC LIMIT 20") List<PlaylistRevisionEntity> revisions(String playlistId);
    @Query("SELECT * FROM playlist_revisions WHERE id = :id") PlaylistRevisionEntity revision(String id);
    @Insert void insertRevision(PlaylistRevisionEntity revision);
    @Query("DELETE FROM playlist_revisions WHERE playlistId = :playlistId AND id NOT IN " +
        "(SELECT id FROM playlist_revisions WHERE playlistId = :playlistId ORDER BY createdAt DESC, id DESC LIMIT 20)") void trimRevisions(String playlistId);
    @Query("DELETE FROM playlist_revisions WHERE playlistId = :playlistId") void deleteRevisions(String playlistId);
    @Query("SELECT COUNT(*) AS qualifiedPlays, COALESCE(SUM(listenedMs), 0) AS listenedMs FROM listening_events WHERE qualified = 1 AND trackKey LIKE 'youtube:%'") ListeningSummaryRow summary();
    @Query("SELECT t.*, COUNT(e.id) AS plays, SUM(e.listenedMs) AS listenedMs FROM listening_events e " +
        "JOIN library_tracks t ON t.trackKey = e.trackKey WHERE e.qualified = 1 " +
        "AND e.trackKey LIKE 'youtube:%' GROUP BY e.trackKey ORDER BY plays DESC, listenedMs DESC, e.trackKey LIMIT :limit") List<TopSongRow> topSongs(int limit);
    @Query("SELECT t.*, e.id AS id, e.startedAt AS startedAt, e.lastPlayedAt AS lastPlayedAt, " +
        "e.listenedMs AS listenedMs FROM listening_events e JOIN library_tracks t ON t.trackKey = e.trackKey " +
        "WHERE e.qualified = 1 AND e.trackKey LIKE 'youtube:%' ORDER BY e.lastPlayedAt DESC, e.id DESC LIMIT :limit OFFSET :offset") List<ListeningEventRow> recentEvents(int limit, int offset);
}
