package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "library_likes", foreignKeys = @ForeignKey(entity = TrackEntity.class, parentColumns = "trackKey", childColumns = "trackKey", onDelete = ForeignKey.CASCADE))
public class LikeEntity {
    @PrimaryKey @NonNull public String trackKey = "";
    public long likedAt;
}
