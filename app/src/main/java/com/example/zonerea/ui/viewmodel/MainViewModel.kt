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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val songRepository: SongRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _allSongs = songRepository.songs
    // Expose all songs for UI selections (e.g., add to playlist from playlist view)
    val allSongs: StateFlow<List<Song>> = _allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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
    val queue: StateFlow<List<Song>> = musicController.queue
    val audioSessionId: StateFlow<Int?> = musicController.audioSessionId

    val isFavorite: StateFlow<Boolean> = combine(currentlyPlaying, _allSongs) { playing, allSongs ->
        playing?.let { currentSong ->
            allSongs.find { it.id == currentSong.id }?.isFavorite ?: false
        } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Sleep timer
    private val _isSleepTimerActive = MutableStateFlow(false)
    val isSleepTimerActive: StateFlow<Boolean> = _isSleepTimerActive.asStateFlow()
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: Job? = null

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

    // Shuffle and play the entire current list (playlist, artist, album, or all)
    fun shufflePlayAll() {
        val current = songs.value
        if (current.isNotEmpty()) {
            _isShuffling.value = true
            musicController.setShuffleMode(true)
            val shuffled = current.shuffled()
            val first = shuffled.first()
            musicController.play(first, shuffled)
            viewModelScope.launch {
                songRepository.updatePlayStatistics(first.id)
            }
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

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        _isSleepTimerActive.value = true
        sleepTimerJob = viewModelScope.launch {
            try {
                delay(minutes * 60_000L)
                musicController.pause()
            } finally {
                _isSleepTimerActive.value = false
                _sleepTimerMinutes.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _isSleepTimerActive.value = false
        _sleepTimerMinutes.value = null
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

    // Create a playlist and add the provided song to it
    fun createPlaylistAndAddSong(song: Song, name: String) {
        viewModelScope.launch {
            val newId = songRepository.createPlaylist(name)
            songRepository.addSongToPlaylist(song.id, newId)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            songRepository.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(song: Song, playlist: Playlist) {
        viewModelScope.launch {
            songRepository.addSongToPlaylist(song.id, playlist.id)
        }
    }

    fun removeSongFromPlaylist(song: Song, playlist: Playlist) {
        viewModelScope.launch {
            songRepository.removeSongFromPlaylist(song.id, playlist.id)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songRepository.deleteSong(song)
        }
    }

    fun playAt(index: Int) {
        musicController.playAt(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicController.moveQueueItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        musicController.removeQueueItem(index)
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        musicController.release()
    }
}

sealed class FilterType {
    object None : FilterType()
    data class Artist(val artist: String) : FilterType()
    data class Album(val album: String) : FilterType()
    data class Playlist(val playlistId: Long) : FilterType()
}
