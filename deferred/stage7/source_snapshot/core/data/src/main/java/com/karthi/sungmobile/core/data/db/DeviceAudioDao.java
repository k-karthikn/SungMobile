package com.karthi.sungmobile.core.data.db;

import androidx.annotation.Nullable;
import androidx.room.*;
import java.util.List;

@Dao
public interface DeviceAudioDao {
    @Query("SELECT * FROM device_tracks ORDER BY title COLLATE NOCASE") List<DeviceTrackEntity> tracks();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void putTracks(List<DeviceTrackEntity> tracks);
    @Query("DELETE FROM device_tracks") void clearTracks();
    @Query("UPDATE device_tracks SET available = 0 WHERE folderKey = :folderKey") void markMissing(String folderKey);
    @Query("SELECT * FROM device_folders ORDER BY name COLLATE NOCASE") List<DeviceFolderEntity> folders();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void putFolder(DeviceFolderEntity folder);
    @Query("DELETE FROM device_folders WHERE uri = :uri") void removeFolder(String uri);
    @Nullable @Query("SELECT * FROM audio_downloads WHERE videoId = :videoId LIMIT 1") DownloadEntity download(String videoId);
    @Query("SELECT * FROM audio_downloads ORDER BY updatedAt DESC") List<DownloadEntity> downloads();
    @Query("SELECT COALESCE(SUM(bytes), 0) FROM audio_downloads WHERE state = 'COMPLETE'") long completedBytes();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void putDownload(DownloadEntity item);
    @Query("DELETE FROM audio_downloads WHERE videoId = :videoId") void removeDownload(String videoId);
}
