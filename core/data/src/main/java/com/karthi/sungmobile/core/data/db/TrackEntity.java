package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "library_tracks")
public class TrackEntity {
    @PrimaryKey @NonNull public String trackKey = "";
    @NonNull public String title = "";
    @NonNull public String artist = "";
    @NonNull public String canonicalUrl = "";
    @Nullable public String artworkUrl;
    @Nullable public Long durationMs;
}
