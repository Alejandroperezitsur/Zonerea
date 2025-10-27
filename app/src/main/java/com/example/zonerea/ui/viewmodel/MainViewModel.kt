package com.example.zonerea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zonerea.data.SongRepository
import com.example.zonerea.model.Album
import com.example.zonerea.model.Artist
import com.example.zonerea.model.Playlist
import com.example.zonerea.model.Song
import com.example.zonerea.playback.MusicController
import com.example.zonerea.playback.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val songRepository: SongRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _allSongs = songRepository.songs

    val playlists = songRepository.playlists

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<FilterType>(FilterType.None)

    val songs: StateFlow<List<Song>> = _filterType.flatMapLatest { filter ->
        when (filter) {
            is FilterType.Album -> _allSongs.map { songs -> songs.filter { it.album == filter.album } }
            is FilterType.Artist -> _allSongs.map { songs -> songs.filter { it.artist == filter.artist } }
            is FilterType.Playlist -> songRepository.getPlaylistWithSongs(filter.playlistId).map { it?.songs ?: emptyList() }
            else -> _allSongs
        }
    }.combine(_searchQuery) { songs, query ->
        if (query.isNotBlank()) {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        } else {
            songs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = _allSongs.map { songs ->
        songs.groupBy { it.album }
            .map { (albumName, songsInAlbum) ->
                Album(
                    name = albumName,
                    artist = songsInAlbum.first().artist,
                    albumArtUri = songsInAlbum.first().albumArtUri,
                    songCount = songsInAlbum.size
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = _allSongs.map { songs ->
        songs.groupBy { it.artist }
            .map { (artistName, songsByArtist) ->
                Artist(
                    name = artistName,
                    songCount = songsByArtist.size
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = _allSongs.map { songs -> songs.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = _allSongs.map { songs -> songs.sortedByDescending { it.dateAdded } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayed: StateFlow<List<Song>> = _allSongs.map { songs -> songs.sortedByDescending { it.lastPlayed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = _allSongs.map { songs -> songs.sortedByDescending { it.playCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentlyPlaying: StateFlow<Song?> = musicController.currentlyPlaying
    val isPlaying: StateFlow<Boolean> = musicController.isPlaying
    val progress: StateFlow<Float> = musicController.progress

    val isFavorite: StateFlow<Boolean> = currentlyPlaying.map { it?.isFavorite ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    fun scanForSongs() {
        viewModelScope.launch {
            songRepository.scanForSongs()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun filterByArtist(artist: String) {
        _filterType.value = FilterType.Artist(artist)
    }

    fun filterByAlbum(album: String) {
        _filterType.value = FilterType.Album(album)
    }

    fun filterByPlaylist(playlistId: Long) {
        _filterType.value = FilterType.Playlist(playlistId)
    }

    fun clearFilter() {
        _filterType.value = FilterType.None
    }

    fun playSong(song: Song) {
        musicController.play(song, songs.value)
        viewModelScope.launch {
            songRepository.updatePlayStatistics(song.id)
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            musicController.pause()
        } else {
            currentlyPlaying.value?.let { musicController.resume() }
        }
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
        _isShuffling.value = !_isShuffling.value
        musicController.setShuffleMode(_isShuffling.value)
    }

    fun toggleRepeat() {
        val nextRepeatMode = when (repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextRepeatMode
        musicController.setRepeatMode(nextRepeatMode)
    }

    fun toggleFavoriteSong(song: Song) {
        viewModelScope.launch {
            songRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            songRepository.createPlaylist(name)
        }
    }

    fun addSongToPlaylist(song: Song, playlist: Playlist) {
        viewModelScope.launch {
            songRepository.addSongToPlaylist(song.id, playlist.id)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songRepository.deleteSong(song)
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
    data class Playlist(val playlistId: Long) : FilterType()
}
