package com.example.zonerea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonerea.data.SongRepository
import com.example.zonerea.model.Song
import com.example.zonerea.playback.MusicController
import com.example.zonerea.playback.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val songRepository: SongRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _songs = songRepository.songs

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow<FilterType>(FilterType.None)

    val songs: StateFlow<List<Song>> = combine(
        _songs,
        _searchQuery,
        _filterType
    ) { songs, query, filter ->
        val filteredSongs = if (query.isNotBlank()) {
            songs.filter { it.title.contains(query, ignoreCase = true) }
        } else {
            songs
        }

        when (filter) {
            is FilterType.Artist -> filteredSongs.filter { it.artist == filter.artist }
            is FilterType.Album -> filteredSongs.filter { it.album == filter.album }
            else -> filteredSongs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = _songs.map { songs -> songs.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = _songs.map { songs -> songs.sortedByDescending { it.dateAdded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayed: StateFlow<List<Song>> = _songs.map { songs -> songs.sortedByDescending { it.lastPlayed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = _songs.map { songs -> songs.sortedByDescending { it.playCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentlyPlaying = MutableStateFlow<Song?>(null)
    val currentlyPlaying: StateFlow<Song?> = _currentlyPlaying.asStateFlow()

    val isFavorite: StateFlow<Boolean> = currentlyPlaying.map { it?.isFavorite ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    val progress: StateFlow<Float> = musicController.progress

    fun scanForSongs() {
        viewModelScope.launch {
            songRepository.scanForSongs()
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun filterByArtist(artist: String) {
        _filterType.value = FilterType.Artist(artist)
    }

    fun filterByAlbum(album: String) {
        _filterType.value = FilterType.Album(album)
    }

    fun playSong(song: Song) {
        musicController.play(song, songs.value)
        _currentlyPlaying.value = song
        _isPlaying.value = true
        viewModelScope.launch {
            songRepository.updatePlayStatistics(song.id)
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            musicController.pause()
        } else {
            musicController.resume()
        }
        _isPlaying.value = !isPlaying.value
    }

    fun seek(position: Float) {
        musicController.seekTo(position)
    }

    fun next() {
        musicController.next()
    }

    fun previous() {
        musicController.previous()
    }

    fun toggleShuffle() {
        _isShuffling.value = !isShuffling.value
        musicController.setShuffleMode(isShuffling.value)
    }

    fun toggleRepeat() {
        _repeatMode.value = when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        musicController.setRepeatMode(repeatMode.value)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            currentlyPlaying.value?.let { song ->
                songRepository.setFavorite(song.id, !song.isFavorite)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}

sealed class FilterType {
    object None : FilterType()
    data class Artist(val artist: String) : FilterType()
    data class Album(val album: String) : FilterType()
}
