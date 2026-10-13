package com.karthi.sungmobile.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration: the accepted Stage 2 queue remains intact. */
object LibraryMigration : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE queue_header ADD COLUMN playbackPassId TEXT")
        db.execSQL("""CREATE TABLE IF NOT EXISTS library_tracks (
            trackKey TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, artist TEXT NOT NULL,
            canonicalUrl TEXT NOT NULL, artworkUrl TEXT, durationMs INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS library_likes (
            trackKey TEXT NOT NULL PRIMARY KEY, likedAt INTEGER NOT NULL,
            FOREIGN KEY(trackKey) REFERENCES library_tracks(trackKey) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS local_playlists (
            id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, pinned INTEGER NOT NULL,
            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS playlist_entries (
            id TEXT NOT NULL PRIMARY KEY, playlistId TEXT NOT NULL, trackKey TEXT NOT NULL, position INTEGER NOT NULL,
            FOREIGN KEY(playlistId) REFERENCES local_playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(trackKey) REFERENCES library_tracks(trackKey) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_entries_playlistId ON playlist_entries(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_entries_trackKey ON playlist_entries(trackKey)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS listening_events (
            id TEXT NOT NULL PRIMARY KEY, passId TEXT NOT NULL, trackKey TEXT NOT NULL, epoch INTEGER NOT NULL,
            listenedMs INTEGER NOT NULL, startedAt INTEGER NOT NULL, lastPlayedAt INTEGER NOT NULL, qualified INTEGER NOT NULL,
            FOREIGN KEY(trackKey) REFERENCES library_tracks(trackKey) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_listening_events_trackKey ON listening_events(trackKey)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS search_history (
            id TEXT NOT NULL PRIMARY KEY, query TEXT NOT NULL, kind TEXT NOT NULL, submittedAt INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS history_policy (
            id INTEGER NOT NULL PRIMARY KEY, privateMode INTEGER NOT NULL, listening INTEGER NOT NULL,
            searches INTEGER NOT NULL, listeningEpoch INTEGER NOT NULL, searchEpoch INTEGER NOT NULL)""")
        db.execSQL("INSERT OR IGNORE INTO history_policy VALUES (1, 0, 1, 1, 0, 0)")
    }
}
