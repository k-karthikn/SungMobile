package com.karthi.sungmobile.core.data.db;

import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

@Dao
public interface PlaylistDao {
    @RawQuery List<PlaylistRow> playlists(SupportSQLiteQuery query);
    @Query("SELECT * FROM local_playlists ORDER BY createdAt, id") List<PlaylistEntity> allForBackup();
    @Query("SELECT p.*, COUNT(e.id) AS songCount FROM local_playlists p LEFT JOIN playlist_entries e ON e.playlistId = p.id WHERE p.id = :id GROUP BY p.id") PlaylistRow playlist(String id);
    @Insert void create(PlaylistEntity playlist);
    @Query("UPDATE local_playlists SET name = :name, updatedAt = :time WHERE id = :id") int rename(String id, String name, long time);
    @Query("UPDATE local_playlists SET pinned = :pinned WHERE id = :id") void pin(String id, boolean pinned);
    @Query("UPDATE local_playlists SET updatedAt = :time WHERE id = :id") void touch(String id, long time);
    @Query("DELETE FROM local_playlists WHERE id = :id") void delete(String id);
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :id ORDER BY position, id") List<PlaylistEntryEntity> entries(String id);
    @Query("SELECT * FROM playlist_entries WHERE id = :entryId") PlaylistEntryEntity entry(String entryId);
    @Insert void insertEntries(List<PlaylistEntryEntity> entries);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void importEntries(List<PlaylistEntryEntity> entries);
    @Query("DELETE FROM playlist_entries WHERE playlistId = :id") void clearEntries(String id);
    @Query("DELETE FROM playlist_entries WHERE playlistId = :id AND id IN (:entryIds)") void remove(String id, List<String> entryIds);
    @Query("UPDATE playlist_entries SET position = :position WHERE id = :entryId AND playlistId = :id") void position(String id, String entryId, int position);
}
