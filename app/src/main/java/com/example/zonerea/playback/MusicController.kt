package com.example.zonerea.playback

import com.example.zonerea.model.Song
import kotlinx.coroutines.flow.StateFlow

interface MusicController {
    val currentlyPlaying: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
    val progress: StateFlow<Float>
    val queue: StateFlow<List<Song>>
    val audioSessionId: StateFlow<Int?>

    fun play(song: Song, playlist: List<Song>)
    fun resume()
    fun pause()
    fun stop()
    fun next()
    fun previous()
    fun seekTo(position: Float)
    fun setShuffleMode(enabled: Boolean)
    fun setRepeatMode(repeatMode: Int)
    fun playAt(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun removeQueueItem(index: Int)
    fun release()

    // Custom command passthrough to service
    fun sendCustomCommand(action: String, args: android.os.Bundle = android.os.Bundle()): android.os.Bundle?
}
