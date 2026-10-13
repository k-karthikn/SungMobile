package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

public class LibrarySongRow extends TrackEntity {
    @NonNull public String rowId = "";
    public boolean liked;
    public int position;
}
