package com.karthi.sungmobile.core.data.db;

import androidx.room.*;
import java.util.List;

@Dao
public abstract class QueueDao {
    @Query("SELECT * FROM queue_entries ORDER BY playIndex")
    public abstract List<QueueEntryEntity> entries();
    @Query("SELECT * FROM queue_header WHERE id = 1")
    public abstract QueueHeaderEntity header();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertHeader(QueueHeaderEntity header);
    @Insert public abstract void insertEntries(List<QueueEntryEntity> entries);
    @Query("DELETE FROM queue_entries") public abstract void clearEntries();

    @Transaction public void replace(QueueHeaderEntity header, List<QueueEntryEntity> entries) {
        clearEntries();
        insertEntries(entries);
        insertHeader(header);
    }

    @Transaction public StoredQueue read() { return new StoredQueue(header(), entries()); }
}
