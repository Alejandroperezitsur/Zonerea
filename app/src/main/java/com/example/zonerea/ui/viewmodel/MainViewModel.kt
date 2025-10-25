package com.example.zonerea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonerea.data.SongRepository
import com.example.zonerea.model.Song
import com.example.zonerea.playback.MusicController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val songRepository: SongRepository,
    private val musicController: MusicController
) : ViewModel() {

    val songs: StateFlow<List<Song>> = songRepository.songs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun scanForSongs() {
        viewModelScope.launch {
            songRepository.scanForSongs()
        }
    }

    fun playSong(song: Song) {
        musicController.play(song, songs.value)
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
