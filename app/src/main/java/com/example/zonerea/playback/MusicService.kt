package com.example.zonerea.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.example.zonerea.MainActivity
import com.example.zonerea.model.Song
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// androidx.media3 Player aliased to avoid name conflicts
import androidx.media3.common.Player as Media3Player
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
    @Suppress("DEPRECATION")
    private var virtualizer: Virtualizer? = null
    private var attachedSessionId: Int = 0
    private lateinit var audioPreferences: AudioPreferences
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var consecutiveErrorSkips: Int = 0
    private var isForeground = false
    private val artworkCache = object : LruCache<String, Bitmap>(20) {}

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            manager.createNotificationChannel(channel)
        }

        player = ExoPlayer.Builder(this).build().apply {
            try {
                val method = this::class.java.getMethod("setCrossFadeEnabled", Boolean::class.javaPrimitiveType)
                method.invoke(this, true)
            } catch (_: Throwable) {
            }
            try {
                val method = this::class.java.getMethod("setAudioOffloadEnabled", Boolean::class.javaPrimitiveType)
                method.invoke(this, true)
            } catch (_: Throwable) {
            }
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        player.setHandleAudioBecomingNoisy(true)
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

                @Suppress("DEPRECATION")
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
                if (events.contains(Media3Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    if (player.playbackState == Media3Player.STATE_READY) {
                        consecutiveErrorSkips = 0
                    }
                }
                if (events.contains(Media3Player.EVENT_MEDIA_METADATA_CHANGED)) {
                    val artUri = this@MusicService.player.currentMediaItem?.mediaMetadata?.artworkUri
                    if (artUri != null) {
                        preloadArtwork(artUri)
                    }
                }
                if (events.contains(Media3Player.EVENT_MEDIA_METADATA_CHANGED) ||
                    events.contains(Media3Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                    events.contains(Media3Player.EVENT_PLAY_WHEN_READY_CHANGED)
                ) {
                    updateForegroundState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("MusicService", "Playback error: ${error.errorCode}", error)
                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Archivo no encontrado, saltando al siguiente."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Error al reproducir el archivo, saltando al siguiente."
                    else -> "Error al reproducir, saltando al siguiente."
                }
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@MusicService, message, Toast.LENGTH_SHORT).show()
                }
                val itemCount = player.mediaItemCount
                if (itemCount == 0) {
                    consecutiveErrorSkips = 0
                    player.stop()
                    updateForegroundState()
                    return
                }
                if (consecutiveErrorSkips >= itemCount) {
                    consecutiveErrorSkips = 0
                    player.stop()
                    updateForegroundState()
                    return
                }
                consecutiveErrorSkips++
                val hasNext = player.currentMediaItemIndex < itemCount - 1
                if (hasNext) {
                    player.seekToNext()
                    player.playWhenReady = true
                    updateForegroundState()
                } else {
                    player.stop()
                    updateForegroundState()
                }
            }
        })
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

    @Suppress("DEPRECATION")
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
            extras?.let { BundleCompat.getParcelable(it, "song", Song::class.java) }
        } catch (_: Throwable) { null }
    }

    private fun buildStyleNotification(): android.app.Notification {
        val currentItem = player.currentMediaItem
        val title = currentItem?.mediaMetadata?.title ?: "Zonerea"
        val artist = currentItem?.mediaMetadata?.artist ?: "Reproductor"

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val isPlaying = player.isPlaying
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val previousAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Anterior",
            mediaSession.sessionActivity
        )
        val playPauseAction = NotificationCompat.Action(
            playPauseIcon,
            if (isPlaying) "Pausar" else "Reproducir",
            mediaSession.sessionActivity
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Siguiente",
            mediaSession.sessionActivity
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(1)
            )

        val artUri = currentItem?.mediaMetadata?.artworkUri
        if (artUri != null) {
            artworkCache.get(artUri.toString())?.let { builder.setLargeIcon(it) }
        }
        return builder.build()
    }

    private fun updateForegroundState() {
        val notification = buildStyleNotification()
        val playing = player.playWhenReady &&
                (player.playbackState == Media3Player.STATE_READY || player.playbackState == Media3Player.STATE_BUFFERING)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (playing) {
            if (!isForeground) {
                startForeground(NOTIFICATION_ID, notification)
                isForeground = true
            } else {
                manager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            manager.notify(NOTIFICATION_ID, notification)
            if (isForeground) {
                stopForeground(android.app.Service.STOP_FOREGROUND_DETACH)
                isForeground = false
            }
        }
    }

    private fun preloadArtwork(uri: Uri) {
        val key = uri.toString()
        if (artworkCache.get(key) != null) return
        ioScope.launch {
            val bitmap = runCatching {
                contentResolver.openInputStream(uri)?.use { first ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(first, null, bounds)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(bounds, 512, 512)
                    }
                    contentResolver.openInputStream(uri)?.use { second ->
                        BitmapFactory.decodeStream(second, null, options)
                    }
                }
            }.getOrNull()
            bitmap?.let {
                artworkCache.put(key, it)
                Handler(Looper.getMainLooper()).post { updateForegroundState() }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
                halfHeight /= 2
                halfWidth /= 2
            }
        }
        return inSampleSize
    }
}
