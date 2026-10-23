package com.karthi.sungmobile.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_folders")
public class DeviceFolderEntity {
    @PrimaryKey @NonNull public String uri = "";
    @NonNull public String name = "";
    @NonNull public String rule = "INCLUDE";
}
