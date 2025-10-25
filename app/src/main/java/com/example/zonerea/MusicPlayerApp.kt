package com.example.zonerea

import android.app.Application
import androidx.room.Room
import com.example.zonerea.data.SongRepository
import com.example.zonerea.data.local.SongDatabase

class MusicPlayerApp : Application() {

    lateinit var songRepository: SongRepository

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            SongDatabase::class.java, "songs-db"
        ).build()
        songRepository = SongRepository(this, database.songDao())
    }
}
