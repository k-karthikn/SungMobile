package com.karthi.sungmobile.core.data.db;

import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.List;

@Dao
public interface LibraryDao {
    @Query("SELECT * FROM library_tracks WHERE trackKey = :key") TrackEntity track(String key);
    @Upsert void saveTrack(TrackEntity track);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void like(LikeEntity like);
    @Query("DELETE FROM library_likes WHERE trackKey = :key") void unlike(String key);
    @Query("SELECT trackKey FROM library_likes WHERE trackKey LIKE 'youtube:%'") List<String> likedKeys();
    @RawQuery List<LibrarySongRow> songs(SupportSQLiteQuery query);
}
