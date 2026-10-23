package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_tracks")
public class DeviceTrackEntity {
    @PrimaryKey @NonNull public String uri = "";
    @NonNull public String title = "";
    @NonNull public String artist = "";
    @NonNull public String folderKey = "";
    public long durationMs;
    public long sizeBytes;
    public long modifiedAt;
    public boolean available;
}
