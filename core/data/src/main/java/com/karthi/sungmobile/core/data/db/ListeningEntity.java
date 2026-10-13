package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "listening_events", foreignKeys = @ForeignKey(entity = TrackEntity.class, parentColumns = "trackKey", childColumns = "trackKey"), indices = {@Index("trackKey")})
public class ListeningEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String passId = "";
    @NonNull public String trackKey = "";
    public long epoch;
    public long listenedMs;
    public long startedAt;
    public long lastPlayedAt;
    public boolean qualified;
}
