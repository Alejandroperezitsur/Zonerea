package com.example.zonerea.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
// androidx.media3 Player aliased to avoid name conflicts
import androidx.media3.common.Player as Media3Player
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionCommands
import com.example.zonerea.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
// MediaStyle from androidx.media may not be available; keep simple notification to avoid build issues.

private const val NOTIFICATION_ID = 101
private const val NOTIFICATION_CHANNEL_ID = "music_player_channel"
private const val NOTIFICATION_CHANNEL_NAME = "Music Player"

@UnstableApi
class MusicService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var attachedSessionId: Int = 0
    private lateinit var audioPreferences: AudioPreferences
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            // Intento habilitar crossfade si la versión de Media3 lo soporta
            try {
                val method = this::class.java.getMethod("setCrossFadeEnabled", Boolean::class.javaPrimitiveType)
                method.invoke(this, true)
            } catch (_: Throwable) {
                // Ignorar si no está disponible
            }
            // Habilitar offload cuando sea posible para mejor rendimiento
            try {
                val method = this::class.java.getMethod("setAudioOffloadEnabled", Boolean::class.javaPrimitiveType)
                method.invoke(this, true)
            } catch (_: Throwable) { }
        }
        // Set up MediaSession with callback for custom commands
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val availableSessionCommands = SessionCommands.Builder()
                        .add(SessionCommand("eq:get_bands", Bundle.EMPTY))
                        .add(SessionCommand("eq:set_band_level", Bundle.EMPTY))
                        .add(SessionCommand("eq:reset", Bundle.EMPTY))
                        .add(SessionCommand("bass:set_strength", Bundle.EMPTY))
                        .add(SessionCommand("virt:set_strength", Bundle.EMPTY))
                        .build()

                    val availablePlayerCommands: Media3Player.Commands = session.player.availableCommands
                    return MediaSession.ConnectionResult.accept(availableSessionCommands, availablePlayerCommands)
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    return try {
                        when (customCommand.customAction) {
                            "eq:get_bands" -> {
                                val eq = equalizer
                                val result = Bundle()
                                if (eq != null) {
                                    val bands = eq.numberOfBands.toInt()
                                    result.putInt("bands", bands)
                                    val range = eq.bandLevelRange
                                    result.putShort("min", range[0])
                                    result.putShort("max", range[1])
                                    val current = ShortArray(bands) { i -> eq.getBandLevel(i.toShort()) }
                                    result.putShortArray("levels", current)
                                } else {
                                    result.putInt("bands", 0)
                                }
                                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
                            }
                            "eq:set_band_level" -> {
                                val band = args.getInt("band", -1)
                                val level = args.getShort("level", 0)
                                equalizer?.let { if (band >= 0) it.setBandLevel(band.toShort(), level) }
                                // Persistir los niveles actuales para la canción en reproducción
                                currentSong()?.let { s ->
                                    val eq = equalizer
                                    if (eq != null) {
                                        val bands = eq.numberOfBands.toInt()
                                        val current = ShortArray(bands) { i -> eq.getBandLevel(i.toShort()) }
                                        ioScope.launch { audioPreferences.saveEqPreset(s.id.toString(), current) }
                                    }
                                }
                                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                            }
                            "eq:reset" -> {
                                val eq = equalizer
                                if (eq != null) {
                                    for (i in 0 until eq.numberOfBands) {
                                        eq.setBandLevel(i.toShort(), 0)
                                    }
                                }
                                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                            }
                            "bass:set_strength" -> {
                                val strength = args.getShort("strength", 0)
                                bassBoost?.setStrength(strength)
                                currentSong()?.let { s -> ioScope.launch { audioPreferences.saveBassStrength(s.id.toString(), strength) } }
                                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                            }
                            "virt:set_strength" -> {
                                val strength = args.getShort("strength", 0)
                                virtualizer?.setStrength(strength)
                                currentSong()?.let { s -> ioScope.launch { audioPreferences.saveVirtStrength(s.id.toString(), strength) } }
                                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                            }
                            else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                        }
                    } catch (_: Throwable) {
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                    }
                }
            })
            .build()
        audioPreferences = AudioPreferences(this)

        player.addListener(object : Media3Player.Listener {
            override fun onEvents(player: Media3Player, events: Media3Player.Events) {
                if (events.contains(Media3Player.EVENT_AUDIO_SESSION_ID)) {
                    attachEffects(this@MusicService.player.audioSessionId)
                }
                if (events.contains(Media3Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Media3Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    updateForegroundNotification()
                }
            }
        })

        createNotificationChannel()

        // Construimos una notificación manualmente para evitar los errores
        // Usamos un ícono estándar de Android para evitar un crash si el nuestro no existe
        val notification = buildStyleNotification()

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
        equalizer?.release(); equalizer = null
        bassBoost?.release(); bassBoost = null
        virtualizer?.release(); virtualizer = null
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
    private fun attachEffects(sessionId: Int) {
        if (sessionId == attachedSessionId) return
        // Release previous
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()

        try {
            // Create and enable effects
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            attachedSessionId = sessionId

            // Aplicar preset guardado para la canción actual
            val song = currentSong()
            if (song != null) {
                ioScope.launch {
                    audioPreferences.loadEqPreset(song.id.toString())?.let { preset ->
                        val eq = equalizer
                        if (eq != null && eq.numberOfBands.toInt() == preset.size) {
                            for (i in preset.indices) {
                                try { eq.setBandLevel(i.toShort(), preset[i]) } catch (_: Throwable) { }
                            }
                        }
                    }
                    audioPreferences.loadBassStrength(song.id.toString())?.let { s ->
                        try { bassBoost?.setStrength(s) } catch (_: Throwable) { }
                    }
                    audioPreferences.loadVirtStrength(song.id.toString())?.let { s ->
                        try { virtualizer?.setStrength(s) } catch (_: Throwable) { }
                    }
                }
            }
        } catch (_: Throwable) {
            attachedSessionId = 0
        }
    }


    private fun currentSong(): Song? {
        return try {
            val extras = player.currentMediaItem?.mediaMetadata?.extras
            extras?.getParcelable("song") as? Song
        } catch (_: Throwable) { null }
    }

    private fun buildStyleNotification(): android.app.Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(player.currentMediaItem?.mediaMetadata?.title ?: "Zonerea")
            .setContentText(player.currentMediaItem?.mediaMetadata?.artist ?: "Reproductor")
            .setOngoing(player.playWhenReady)

        // Intentar cargar carátula como ícono grande
        val artUri = player.currentMediaItem?.mediaMetadata?.artworkUri
        if (artUri != null) {
            runCatching {
                contentResolver.openInputStream(Uri.parse(artUri.toString()))?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    builder.setLargeIcon(bmp)
                }
            }
        }
        return builder.build()
    }

    private fun updateForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildStyleNotification())
    }
}
