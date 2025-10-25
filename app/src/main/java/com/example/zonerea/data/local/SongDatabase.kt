package com.example.zonerea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zonerea.model.Song

@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
