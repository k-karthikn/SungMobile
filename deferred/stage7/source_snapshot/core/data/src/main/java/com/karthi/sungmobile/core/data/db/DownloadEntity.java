package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "audio_downloads")
public class DownloadEntity {
    @PrimaryKey @NonNull public String videoId = "";
    @NonNull public String title = "";
    @NonNull public String artist = "";
    @NonNull public String state = "QUEUED";
    @Nullable public String uri;
    @Nullable public String destination;
    @Nullable public String error;
    public long bytes;
    public long updatedAt;
}
