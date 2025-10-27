package com.example.zonerea.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.zonerea.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicControllerImpl(private val context: Context) : MusicController {

    private var mediaController: MediaController? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentlyPlaying = MutableStateFlow<Song?>(null)
    override val currentlyPlaying = _currentlyPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    override val progress = _progress.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaMetadata?.extras?.getParcelable<Song>("song")?.let {
                            _currentlyPlaying.value = it
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })

                // Progress updates
                coroutineScope.launch {
                    while (true) {
                        mediaController?.let {
                            if (it.isPlaying) {
                                val duration = it.duration.coerceAtLeast(0)
                                val position = it.currentPosition.coerceAtLeast(0)
                                _progress.value = if (duration > 0) position.toFloat() / duration else 0f
                            }
                        }
                        delay(1000)
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun play(song: Song, playlist: List<Song>) {
        mediaController?.let { controller ->
            val mediaItems = playlist.map { s ->
                MediaItem.Builder()
                    .setUri(s.uri.toUri())
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.albumArtUri?.toUri())
                        .setExtras(Bundle().apply { putParcelable("song", s) })
                        .build())
                    .build()
            }
            controller.setMediaItems(mediaItems, playlist.indexOf(song), 0)
            controller.prepare()
            controller.play()
        }
    }

    override fun resume() {
        mediaController?.play()
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun stop() {
        mediaController?.stop()
    }

    override fun next() {
        mediaController?.seekToNextMediaItem()
    }

    override fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    override fun seekTo(position: Float) {
        mediaController?.seekTo((position * (mediaController?.duration ?: 0L)).toLong())
    }

    override fun setShuffleMode(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(repeatMode: Int) {
        mediaController?.repeatMode = repeatMode
    }

    override fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
