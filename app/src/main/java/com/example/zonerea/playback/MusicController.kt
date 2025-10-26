package com.example.zonerea.playback

import android.content.ComponentName
import android.content.Context
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicController(context: Context) {

    private lateinit var mediaController: MediaController
    private val controllerFuture: ListenableFuture<MediaController>

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private var progressJob: Job? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            startProgressUpdates()
                        } else {
                            stopProgressUpdates()
                        }
                    }
                })
            },
            MoreExecutors.directExecutor()
        )
    }

    fun play(song: Song, songs: List<Song>) {
        if (!::mediaController.isInitialized) return

        mediaController.stop()
        mediaController.clearMediaItems()

        val mediaItems = songs.map {
            MediaItem.Builder()
                .setUri(it.uri.toUri())
                .setMediaId(it.id.toString())
                .build()
        }
        mediaController.setMediaItems(mediaItems)

        val startIndex = songs.indexOf(song)
        if (startIndex != -1) {
            mediaController.seekTo(startIndex, 0)
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun pause() {
        if (!::mediaController.isInitialized) return
        mediaController.pause()
    }

    fun resume() {
        if (!::mediaController.isInitialized) return
        mediaController.play()
    }

    fun seekTo(position: Float) {
        if (!::mediaController.isInitialized) return
        mediaController.seekTo((mediaController.duration * position).toLong())
    }

    fun next() {
        if (!::mediaController.isInitialized) return
        mediaController.seekToNext()
    }

    fun previous() {
        if (!::mediaController.isInitialized) return
        mediaController.seekToPrevious()
    }

    fun setShuffleMode(isShuffling: Boolean) {
        if (!::mediaController.isInitialized) return
        mediaController.shuffleModeEnabled = isShuffling
    }

    fun setRepeatMode(repeatMode: Int) {
        if (!::mediaController.isInitialized) return
        mediaController.repeatMode = when (repeatMode) {
            com.example.zonerea.playback.Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            com.example.zonerea.playback.Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (mediaController.duration > 0) {
                    _progress.value = mediaController.currentPosition.toFloat() / mediaController.duration
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressUpdates()
        MediaController.releaseFuture(controllerFuture)
    }
}
