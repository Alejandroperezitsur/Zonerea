package com.example.zonerea.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Song
import com.example.zonerea.ui.composables.AddToPlaylistDialog
import com.example.zonerea.ui.composables.AlphabetIndex
import com.example.zonerea.ui.composables.SongItem
import com.example.zonerea.ui.composables.SongInfoDialog
import com.example.zonerea.ui.viewmodel.FilterType
import com.example.zonerea.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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
    var songInfo by remember { mutableStateOf<Song?>(null) }

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

    if (songInfo != null) {
        SongInfoDialog(
            song = songInfo!!,
            onDismiss = { songInfo = null }
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
                },
                windowInsets = TopAppBarDefaults.windowInsets
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
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
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
                val dismissState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> {
                                if (onRemove != null) {
                                    onRemove(song)
                                } else {
                                    viewModel.deleteSong(song)
                                }
                                true
                            }
                            else -> false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val targetColor = if (onRemove != null) Color(0xFF7C4DFF) else Color(0xFFD32F2F)
                        val alpha by animateFloatAsState(
                            targetValue = if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) 1f else 0f,
                            label = "dismiss_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(targetColor.copy(alpha = alpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (onRemove != null) "Quitar de playlist" else "Eliminar",
                                tint = Color.White
                            )
                        }
                    },
                    content = {
                        SongItem(
                            song = song,
                            onClick = { viewModel.playSong(song) },
                            onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onDeleteSong = { viewModel.deleteSong(song) },
                            onShowInfo = { songInfo = it },
                            onRemoveFromPlaylist = onRemove
                        )
                    }
                )
            }
            }

            AlphabetIndex(
                onLetterSelected = { ch ->
                    fun normalizedTitle(t: String): String {
                        val tt = t.trimStart()
                        val lowered = tt.lowercase()
                        val articles = listOf("el ", "la ", "los ", "las ", "the ")
                        val match = articles.firstOrNull { lowered.startsWith(it) }
                        return if (match != null) tt.drop(match.length) else tt
                    }
                    val idx = songs.indexOfFirst { s ->
                        val nt = normalizedTitle(s.title)
                        val c = nt.firstOrNull()?.uppercaseChar()
                        if (ch == '#') c != null && (c !in 'A'..'Z') else c == ch
                    }
                    if (idx >= 0) {
                        scope.launch { listState.animateScrollToItem(idx) }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }
    }
}
