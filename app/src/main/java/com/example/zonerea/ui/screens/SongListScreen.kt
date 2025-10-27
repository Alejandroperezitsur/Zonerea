package com.example.zonerea.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Song
import com.example.zonerea.ui.composables.AddToPlaylistDialog
import com.example.zonerea.ui.composables.SongItem
import com.example.zonerea.ui.viewmodel.FilterType
import com.example.zonerea.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    viewModel: MainViewModel,
    title: String,
    filterType: FilterType,
    onBack: () -> Unit,
) {
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (filterType is FilterType.Playlist) {
                FloatingActionButton(
                    onClick = { /* TODO: Implement add songs to playlist */ },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar canciones")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(songs) { song ->
                val onRemove: ((Song) -> Unit)? = if (filterType is FilterType.Playlist) {
                    {
                        playlists.find { it.id == filterType.playlistId }?.let { playlist ->
                            viewModel.removeSongFromPlaylist(song, playlist)
                        }
                    }
                } else {
                    null
                }

                SongItem(
                    song = song,
                    onClick = { viewModel.playSong(song) },
                    onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onDeleteSong = { viewModel.deleteSong(song) },
                    onRemoveFromPlaylist = onRemove
                )
            }
        }
    }
}
