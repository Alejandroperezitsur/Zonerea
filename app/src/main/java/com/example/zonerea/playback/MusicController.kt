package com.example.zonerea.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.zonerea.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MusicController(context: Context) {

    private lateinit var mediaController: MediaController
    private val controllerFuture: ListenableFuture<MediaController>

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
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
            mediaController?.play()
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

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
