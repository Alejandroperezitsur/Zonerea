package com.example.zonerea

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
        ).fallbackToDestructiveMigration().build()
        songRepository = SongRepository(this, database.songDao())

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Player"
            val descriptionText = "Channel for music player controls"
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Crucial for foreground services
            val channel = NotificationChannel("music_player_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
