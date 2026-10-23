package com.karthi.sungmobile.core.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {QueueEntryEntity.class, QueueHeaderEntity.class, TrackEntity.class, LikeEntity.class, PlaylistEntity.class, PlaylistEntryEntity.class, ListeningEntity.class, SearchEntity.class, HistoryPolicyEntity.class, SmartPlaylistEntity.class, PlaylistRevisionEntity.class}, version = 5, exportSchema = true)
public abstract class SungDatabase extends RoomDatabase {
    public abstract QueueDao queueDao();
    public abstract LibraryDao libraryDao();
    public abstract PlaylistDao playlistDao();
    public abstract HistoryDao historyDao();
    public abstract AdvancedLibraryDao advancedLibraryDao();
}
