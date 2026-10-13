package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "playlist_entries", foreignKeys = {
    @ForeignKey(entity = PlaylistEntity.class, parentColumns = "id", childColumns = "playlistId", onDelete = ForeignKey.CASCADE),
    @ForeignKey(entity = TrackEntity.class, parentColumns = "trackKey", childColumns = "trackKey")
}, indices = {@Index("playlistId"), @Index("trackKey")})
public class PlaylistEntryEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String playlistId = "";
    @NonNull public String trackKey = "";
    public int position;
}
