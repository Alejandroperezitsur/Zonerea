package com.example.zonerea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zonerea.model.Playlist
import com.example.zonerea.model.PlaylistSongCrossRef
import com.example.zonerea.model.Song

@Database(
    entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class],
    version = 2,
    exportSchema = false
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
