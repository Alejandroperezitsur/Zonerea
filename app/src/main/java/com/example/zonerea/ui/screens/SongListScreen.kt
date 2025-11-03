package com.example.zonerea.ui.screens

// import androidx.compose.animation.core.animateFloatAsState
// import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Delete
// import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Song
import com.example.zonerea.ui.composables.AddToPlaylistDialog
import com.example.zonerea.ui.composables.AddSongsToPlaylistDialog
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
    val allSongs by viewModel.allSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songInfo by remember { mutableStateOf<Song?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    if (songToAddToPlaylist != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAddToPlaylist = null },
            onAddToExistingPlaylist = { playlist ->
                songToAddToPlaylist?.let { song -> viewModel.addSongToPlaylist(song, playlist) }
                songToAddToPlaylist = null
            },
            onCreateNewPlaylist = { playlistName ->
                songToAddToPlaylist?.let { song ->
                    viewModel.createPlaylistAndAddSong(song, playlistName)
                }
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
                actions = {
                    if (filterType is FilterType.Playlist && songs.isNotEmpty()) {
                        IconButton(onClick = {
                            // Reproducir toda la playlist en modo aleatorio
                            viewModel.shufflePlayAll()
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Reproducir toda la playlist en aleatorio")
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = filterType is FilterType.Playlist,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddSongsDialog = true },
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
            // Diálogo para añadir múltiples canciones a la playlist desde el FAB
            if (showAddSongsDialog && filterType is FilterType.Playlist) {
                playlists.find { it.id == filterType.playlistId }?.let { pl ->
                    AddSongsToPlaylistDialog(
                        availableSongs = allSongs,
                        alreadyInPlaylistIds = songs.map { it.id }.toSet(),
                        onDismiss = { showAddSongsDialog = false },
                        onConfirm = { selected ->
                            selected.forEach { s -> viewModel.addSongToPlaylist(s, pl) }
                            showAddSongsDialog = false
                        }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
            items(songs, key = { it.id }) { song ->
                val onRemove: ((Song) -> Unit)? = if (filterType is FilterType.Playlist) {
                    {
                        playlists.find { it.id == filterType.playlistId }?.let { playlist ->
                            viewModel.removeSongFromPlaylist(song, playlist)
                        }
                    }
                } else {
                    null
                }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            if (onRemove != null) {
                                onRemove.invoke(song)
                            } else {
                                viewModel.deleteSong(song)
                            }
                        }
                        true
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // Fondo con color de error y un ícono de eliminar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.align(Alignment.Center)
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
            if (showAddSongsDialog && filterType is FilterType.Playlist) {
                val currentPlaylist = playlists.find { it.id == filterType.playlistId }
                if (currentPlaylist != null) {
                    val alreadyIds = songs.map { it.id }.toSet()
                    AddSongsToPlaylistDialog(
                        availableSongs = allSongs,
                        alreadyInPlaylistIds = alreadyIds,
                        onDismiss = { showAddSongsDialog = false },
                        onConfirm = { selected ->
                            selected.forEach { song -> viewModel.addSongToPlaylist(song, currentPlaylist) }
                            showAddSongsDialog = false
                        }
                    )
                } else {
                    showAddSongsDialog = false
                }
            }
        }
    }
}
