package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "smart_playlists")
public class SmartPlaylistEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String name = "";
    public boolean requireLiked;
    public int minPlays;
    public int recentDays;
    @NonNull public String sort = "TITLE";
    public int maxSongs;
    public long createdAt;
    public long updatedAt;
}
