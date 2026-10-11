package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue_header")
public class QueueHeaderEntity {
    @PrimaryKey public int id = 1;
    @Nullable public String currentId;
    public long positionMs;
    @NonNull public String repeatMode = "OFF";
    public boolean shuffled;
}
