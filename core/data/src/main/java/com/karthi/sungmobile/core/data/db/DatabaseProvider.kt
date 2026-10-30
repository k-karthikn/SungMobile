package com.karthi.sungmobile.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schema 4 omits the deferred device-audio feature. Existing tables are left intact on upgrade. */
private object Stage2ToOnlineOnly : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

private object Stage7ToOnlineOnly : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

object DatabaseProvider {
    fun open(context: Context, name: String = "sung.db"): SungDatabase = Room.databaseBuilder(context.applicationContext,
        SungDatabase::class.java, name)
        .addMigrations(LibraryMigration, Stage2ToOnlineOnly, Stage7ToOnlineOnly, Stage8ToStage9,
            BackupRestoreJournalMigration).build()
}

/** Add Stage 9's local definitions without altering existing library or history rows. */
internal object Stage8ToStage9 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `smart_playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `requireLiked` INTEGER NOT NULL, `minPlays` INTEGER NOT NULL, `recentDays` INTEGER NOT NULL, `sort` TEXT NOT NULL, `maxSongs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_revisions` (`id` TEXT NOT NULL, `playlistId` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `entriesJson` TEXT NOT NULL, `songCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_revisions_playlistId` ON `playlist_revisions` (`playlistId`)")
    }
}

/** Journal row lets startup finish a backup restore if DataStore was not updated before process death. */
internal object BackupRestoreJournalMigration : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `backup_restore_journal` (`id` INTEGER NOT NULL, `settingsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}
