package com.karthi.sungmobile.core.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {QueueEntryEntity.class, QueueHeaderEntity.class}, version = 1, exportSchema = true)
public abstract class SungDatabase extends RoomDatabase {
    public abstract QueueDao queueDao();
}
