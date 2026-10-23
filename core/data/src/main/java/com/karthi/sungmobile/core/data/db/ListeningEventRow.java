package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;

public class ListeningEventRow extends TrackEntity {
    @NonNull public String id = "";
    public long startedAt;
    public long lastPlayedAt;
    public long listenedMs;
}
