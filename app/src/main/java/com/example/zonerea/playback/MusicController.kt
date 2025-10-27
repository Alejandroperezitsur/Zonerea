package com.example.zonerea.playback

import com.example.zonerea.model.Song
import kotlinx.coroutines.flow.StateFlow

interface MusicController {
    val currentlyPlaying: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
    val progress: StateFlow<Float>

    fun play(song: Song, playlist: List<Song>)
    fun resume()
    fun pause()
    fun stop()
    fun next()
    fun previous()
    fun seekTo(position: Float)
    fun setShuffleMode(enabled: Boolean)
    fun setRepeatMode(repeatMode: Int)
    fun release()
}
