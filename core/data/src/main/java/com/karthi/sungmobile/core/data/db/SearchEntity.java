package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "search_history")
public class SearchEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String query = "";
    @NonNull public String kind = "SONG";
    public long submittedAt;
}
