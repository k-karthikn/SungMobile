package com.karthi.sungmobile.core.data.db;

import androidx.annotation.*;
import androidx.room.*;

@Entity(tableName = "history_policy")
public class HistoryPolicyEntity {
    @PrimaryKey public int id = 1;
    public boolean privateMode;
    public boolean listening = true;
    public boolean searches = true;
    public long listeningEpoch;
    public long searchEpoch;
}
