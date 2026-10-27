package com.karthi.sungmobile.core.data.db;

import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

@Dao
public interface LibraryDao {
    @Query("SELECT * FROM library_tracks WHERE trackKey = :key") TrackEntity track(String key);
    @Upsert void saveTrack(TrackEntity track);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void importTracks(List<TrackEntity> tracks);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void like(LikeEntity like);
    @Query("DELETE FROM library_likes WHERE trackKey = :key") void unlike(String key);
    @Query("SELECT trackKey FROM library_likes WHERE trackKey LIKE 'youtube:%'") List<String> likedKeys();
    @Query("SELECT * FROM library_likes WHERE trackKey LIKE 'youtube:%'") List<LikeEntity> likesForBackup();
    @Query("SELECT * FROM library_tracks WHERE trackKey LIKE 'youtube:%'") List<TrackEntity> tracksForBackup();
    @RawQuery List<LibrarySongRow> songs(SupportSQLiteQuery query);
}
