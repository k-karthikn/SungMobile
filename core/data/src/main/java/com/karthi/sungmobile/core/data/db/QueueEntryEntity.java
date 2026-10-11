package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue_entries")
public class QueueEntryEntity {
    @PrimaryKey @NonNull public String entryId = "";
    public int playIndex;
    public int originalIndex;
    @NonNull public String trackId = "";
    @NonNull public String title = "";
    @NonNull public String subtitle = "";
    @NonNull public String canonicalUrl = "";
    @Nullable public String artworkUrl;
    @Nullable public Long durationMs;
}
