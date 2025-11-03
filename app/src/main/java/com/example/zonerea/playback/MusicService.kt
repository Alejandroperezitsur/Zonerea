package com.example.zonerea.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

private const val NOTIFICATION_ID = 101
private const val NOTIFICATION_CHANNEL_ID = "music_player_channel"
private const val NOTIFICATION_CHANNEL_NAME = "Music Player"

@UnstableApi
class MusicService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        createNotificationChannel()

        // Construimos una notificación manualmente para evitar los errores
        // Usamos un ícono estándar de Android para evitar un crash si el nuestro no existe
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Ícono de sistema a prueba de fallos
            .setContentTitle("Zonerea")
            .setContentText("Servicio de música en ejecución")
            .build()

        // Forzamos el servicio al primer plano para que no se cierre
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
