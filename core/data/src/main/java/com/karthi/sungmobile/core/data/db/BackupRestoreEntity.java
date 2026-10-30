package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Stores the small settings portion needed to finish an interrupted cross-store restore. */
@Entity(tableName = "backup_restore_journal")
public class BackupRestoreEntity {
    @PrimaryKey public int id = 1;
    @NonNull public String settingsJson = "";
}
