package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_revisions", indices = {@Index("playlistId")})
public class PlaylistRevisionEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String playlistId = "";
    @NonNull public String name = "";
    public long createdAt;
    @NonNull public String entriesJson = "[]";
    public int songCount;
}
