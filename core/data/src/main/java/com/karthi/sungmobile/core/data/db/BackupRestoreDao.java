package com.karthi.sungmobile.core.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface BackupRestoreDao {
    @Query("SELECT settingsJson FROM backup_restore_journal WHERE id = 1") String pendingSettings();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void save(BackupRestoreEntity journal);
    @Query("DELETE FROM backup_restore_journal WHERE id = 1") void clear();
}
