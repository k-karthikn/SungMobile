package com.karthi.sungmobile.core.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    fun open(context: Context): SungDatabase = Room.databaseBuilder(context.applicationContext,
        SungDatabase::class.java, "sung.db").addMigrations(LibraryMigration, DeviceAudioMigration).build()
}
