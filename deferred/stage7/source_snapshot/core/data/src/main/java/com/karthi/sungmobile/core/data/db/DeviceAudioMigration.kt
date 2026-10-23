package com.karthi.sungmobile.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive Stage 7 tables leave existing queue, library and history rows untouched. */
object DeviceAudioMigration : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS device_tracks (uri TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL, artist TEXT NOT NULL, folderKey TEXT NOT NULL,
            durationMs INTEGER NOT NULL, sizeBytes INTEGER NOT NULL, modifiedAt INTEGER NOT NULL,
            available INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS device_folders (uri TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL, rule TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS audio_downloads (videoId TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL, artist TEXT NOT NULL, state TEXT NOT NULL, uri TEXT,
            destination TEXT, error TEXT, bytes INTEGER NOT NULL, updatedAt INTEGER NOT NULL)""")
    }
}
