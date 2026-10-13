package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "local_playlists")
public class PlaylistEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String name = "";
    public boolean pinned;
    public long createdAt;
    public long updatedAt;
}
