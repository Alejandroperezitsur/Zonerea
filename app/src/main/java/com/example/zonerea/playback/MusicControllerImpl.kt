package com.example.zonerea.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.session.SessionCommand
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
    private val scopeJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + scopeJob)

    private val _currentlyPlaying = MutableStateFlow<Song?>(null)
    override val currentlyPlaying = _currentlyPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    override val progress = _progress.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    override val queue = _queue.asStateFlow()

    private val _audioSessionId = MutableStateFlow<Int?>(null)
    override val audioSessionId = _audioSessionId.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaMetadata?.extras?.let { extras ->
                            BundleCompat.getParcelable(extras, "song", Song::class.java)?.let {
                                _currentlyPlaying.value = it
                            }
                        }
                        // Refresh queue when current item changes
                        refreshQueue()
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        // Timeline changed (items added/removed/moved)
                        refreshQueue()
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
                        delay(200)
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun play(song: Song, playlist: List<Song>) {
        mediaController?.let { controller ->
            if (playlist.isEmpty()) return
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
            val startIndex = playlist.indexOf(song).let { if (it >= 0) it else 0 }
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
            // Update queue state
            _queue.value = playlist
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
        mediaController?.let { controller ->
            val duration = controller.duration
            val clamped = position.coerceIn(0f, 1f)
            if (controller.isCurrentMediaItemSeekable && duration != C.TIME_UNSET && duration > 0) {
                controller.seekTo((clamped * duration).toLong())
            }
        }
    }

    override fun setShuffleMode(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(repeatMode: Int) {
        mediaController?.repeatMode = repeatMode
    }

    override fun playAt(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.seekToDefaultPosition(index)
                controller.play()
            }
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        mediaController?.let { controller ->
            val count = controller.mediaItemCount
            if (fromIndex in 0 until count && toIndex in 0 until count) {
                controller.moveMediaItem(fromIndex, toIndex)
                refreshQueue()
            }
        }
    }

    override fun removeQueueItem(index: Int) {
        mediaController?.let { controller ->
            val count = controller.mediaItemCount
            if (index in 0 until count) {
                controller.removeMediaItem(index)
                refreshQueue()
            }
        }
    }

    override fun release() {
        scopeJob.cancel()
        MediaController.releaseFuture(controllerFuture)
        mediaController = null
    }

    override fun sendCustomCommand(action: String, args: Bundle): Bundle? {
        val controller = mediaController ?: return null
        return try {
            val result = controller.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
            result.get().extras
        } catch (_: Throwable) {
            null
        }
    }

    private fun refreshQueue() {
        mediaController?.let { controller ->
            val items = mutableListOf<Song>()
            val count = controller.mediaItemCount
            for (i in 0 until count) {
                val item = controller.getMediaItemAt(i)
                val extras = item.mediaMetadata.extras
                val song = extras?.let { BundleCompat.getParcelable(it, "song", Song::class.java) }
                if (song != null) items.add(song)
            }
            _queue.value = items
        }
    }
}
