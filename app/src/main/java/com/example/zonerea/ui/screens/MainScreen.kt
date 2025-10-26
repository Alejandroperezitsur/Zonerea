package com.example.zonerea.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Song
import com.example.zonerea.ui.composables.AlbumItem
import com.example.zonerea.ui.composables.AddToPlaylistDialog
import com.example.zonerea.ui.composables.ArtistItem
import com.example.zonerea.ui.composables.ConfirmDeleteDialog
import com.example.zonerea.ui.composables.MiniPlayer
import com.example.zonerea.ui.composables.PlaylistItem
import com.example.zonerea.ui.composables.SongItem
import com.example.zonerea.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val favorites by viewModel.favorites.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val lastPlayed by viewModel.lastPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val tabs = listOf(
        "Canciones" to Icons.Default.MusicNote,
        "Álbumes" to Icons.Default.Album,
        "Artistas" to Icons.Default.Person,
        "Playlists" to Icons.Default.QueueMusic,
        "Favoritos" to Icons.Default.Favorite,
        "Recientes" to Icons.Default.History,
        "Últimas" to Icons.Default.Schedule,
        "Más Escuchadas" to Icons.AutoMirrored.Filled.TrendingUp
    )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    if (songToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAddToPlaylist = null },
            onAddToExistingPlaylist = { playlist ->
                songToAddToPlaylist?.let { song -> viewModel.addSongToPlaylist(song, playlist) }
                songToAddToPlaylist = null
            },
            onCreateNewPlaylist = { playlistName ->
                viewModel.createPlaylist(playlistName)
                songToAddToPlaylist = null
            }
        )
    }

    if (songToDelete != null) {
        ConfirmDeleteDialog(
            song = songToDelete!!,
            onDismiss = { songToDelete = null },
            onConfirm = {
                songToDelete?.let { viewModel.deleteSong(it) }
                songToDelete = null
            }
        )
    }

    if (isPlayerExpanded) {
        PlayerScreen(
            viewModel = viewModel,
            onClose = { isPlayerExpanded = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Zonerea") })
            },
            bottomBar = {
                currentlyPlaying?.let {
                    MiniPlayer(
                        song = it,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { isPlayerExpanded = true }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            text = { Text(title) },
                            icon = { Icon(icon, contentDescription = title) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    Crossfade(targetState = page, label = "page_crossfade") { targetPage ->
                        when (targetPage) {
                            0 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(songs) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = { viewModel.playSong(song) },
                                        onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onDeleteSong = { songToDelete = it }
                                    )
                                }
                            }
                            1 -> LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(albums) { album ->
                                    AlbumItem(album = album, onClick = { /* TODO */ })
                                }
                            }
                            2 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(artists) { artist ->
                                    ArtistItem(artist = artist, onClick = { /* TODO */ })
                                }
                            }
                            3 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(playlists) { playlist ->
                                    PlaylistItem(playlist = playlist, onClick = { /* TODO */ })
                                }
                            }
                            4 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(favorites) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = { viewModel.playSong(song) },
                                        onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onDeleteSong = { songToDelete = it }
                                    )
                                }
                            }
                            5 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(recentlyAdded) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = { viewModel.playSong(song) },
                                        onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onDeleteSong = { songToDelete = it }
                                    )
                                }
                            }
                            6 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(lastPlayed) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = { viewModel.playSong(song) },
                                        onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onDeleteSong = { songToDelete = it }
                                    )
                                }
                            }
                            7 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(mostPlayed) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = { viewModel.playSong(song) },
                                        onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                        onAddToPlaylist = { songToAddToPlaylist = it },
                                        onDeleteSong = { songToDelete = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
