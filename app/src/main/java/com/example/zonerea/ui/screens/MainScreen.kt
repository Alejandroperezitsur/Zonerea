package com.example.zonerea.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Playlist
import com.example.zonerea.model.Song
import com.example.zonerea.ui.composables.*
import com.example.zonerea.ui.composables.AlphabetIndex
import com.example.zonerea.ui.composables.SleepTimerDialog
import com.example.zonerea.ui.viewmodel.FilterType
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSleepTimerActive by viewModel.isSleepTimerActive.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var isQueueOpen by rememberSaveable { mutableStateOf(false) }
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf<FilterType>(FilterType.None) }
    var songInfoDialogSong by remember { mutableStateOf<Song?>(null) }

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }
    BackHandler(enabled = isQueueOpen) {
        isQueueOpen = false
    }

    if (currentFilter !is FilterType.None) {
        val title = when (val filter = currentFilter) {
            is FilterType.Album -> filter.album
            is FilterType.Artist -> filter.artist
            is FilterType.Playlist -> playlists.find { it.id == filter.playlistId }?.name ?: ""
            else -> ""
        }
        SongListScreen(
            viewModel = viewModel,
            title = title,
            filterType = currentFilter,
            onBack = {
                viewModel.clearFilter()
                currentFilter = FilterType.None
            }
        )
        return
    }


    val tabs = listOf(
        "Canciones" to Icons.Default.MusicNote,
        "Álbumes" to Icons.Default.Album,
        "Artistas" to Icons.Default.Person,
        "Playlists" to Icons.AutoMirrored.Filled.QueueMusic,
        "Favoritos" to Icons.Default.Favorite,
        "Recientes" to Icons.Default.History,
        "Últimas" to Icons.Default.Schedule,
        "Más Escuchadas" to Icons.AutoMirrored.Filled.TrendingUp
    )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = {
                viewModel.createPlaylist(it)
                showCreatePlaylistDialog = false
            }
        )
    }

    songInfoDialogSong?.let { s ->
        SongInfoDialog(song = s, onDismiss = { songInfoDialogSong = null })
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            isActive = isSleepTimerActive,
            currentMinutes = sleepTimerMinutes,
            onSet = { m -> viewModel.startSleepTimer(m) },
            onCancel = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (playlistToDelete != null) {
        ConfirmDeleteDialog(
            playlist = playlistToDelete,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                playlistToDelete?.let { viewModel.deletePlaylist(it) }
                playlistToDelete = null
            }
        )
    }

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

    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        PlayerScreen(
            viewModel = viewModel,
            onClose = { isPlayerExpanded = false }
        )
    }
    AnimatedVisibility(
        visible = isQueueOpen,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        QueueScreen(
            viewModel = viewModel,
            onClose = { isQueueOpen = false }
        )
    }
    if (!isPlayerExpanded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                DockedSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                                    onSearch = { /* No-op, usamos sugerencias */ },
                                    active = true,
                                    onActiveChange = { /* mantiene activo mientras isSearchActive */ },
                                    placeholder = { Text("Buscar canciones, artistas, álbumes, playlists") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                        }
                                    }
                                ) {
                                    val q = searchQuery.lowercase()
                                    val artistSuggestions = artists.filter { it.name.lowercase().contains(q) }.take(5)
                                    val albumSuggestions = albums.filter { it.name.lowercase().contains(q) }.take(5)
                                    val playlistSuggestions = playlists.filter { it.name.lowercase().contains(q) }.take(5)
                                    val songSuggestions = songs.filter { it.title.lowercase().contains(q) }.take(5)

                                    LazyColumn {
                                        if (artistSuggestions.isNotEmpty()) {
                                            item { Text("Artistas", style = MaterialTheme.typography.titleSmall) }
                                            items(artistSuggestions.size) { idx ->
                                                val artist = artistSuggestions[idx]
                                                ListItem(
                                                    headlineContent = { Text(artist.name) },
                                                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                                                    overlineContent = { Text("Filtrar por artista") },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            currentFilter = FilterType.Artist(artist.name)
                                                            isSearchActive = false
                                                        }
                                                )
                                            }
                                        }
                                        if (albumSuggestions.isNotEmpty()) {
                                            item { Text("Álbumes", style = MaterialTheme.typography.titleSmall) }
                                            items(albumSuggestions.size) { idx ->
                                                val album = albumSuggestions[idx]
                                                ListItem(
                                                    headlineContent = { Text(album.name) },
                                                    leadingContent = { Icon(Icons.Default.Album, contentDescription = null) },
                                                    overlineContent = { Text("Filtrar por álbum") },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            currentFilter = FilterType.Album(album.name)
                                                            isSearchActive = false
                                                        }
                                                )
                                            }
                                        }
                                        if (playlistSuggestions.isNotEmpty()) {
                                            item { Text("Playlists", style = MaterialTheme.typography.titleSmall) }
                                            items(playlistSuggestions.size) { idx ->
                                                val playlist = playlistSuggestions[idx]
                                                ListItem(
                                                    headlineContent = { Text(playlist.name) },
                                                    leadingContent = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                                    overlineContent = { Text("Filtrar por playlist") },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            currentFilter = FilterType.Playlist(playlist.id)
                                                            isSearchActive = false
                                                        }
                                                )
                                            }
                                        }
                                        if (songSuggestions.isNotEmpty()) {
                                            item { Text("Canciones", style = MaterialTheme.typography.titleSmall) }
                                            items(songSuggestions.size) { idx ->
                                                val song = songSuggestions[idx]
                                                ListItem(
                                                    headlineContent = { Text(song.title) },
                                                    leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                                    overlineContent = { Text(song.artist) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.playSong(song)
                                                            isSearchActive = false
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    val hasQuery = searchQuery.isNotBlank()
                                    FilterChip(
                                        selected = currentFilter is FilterType.Artist,
                                        onClick = {
                                            if (hasQuery) {
                                                val match = artists.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                                                if (match != null) {
                                                    currentFilter = FilterType.Artist(match.name)
                                                    isSearchActive = false
                                                }
                                            }
                                        },
                                        label = { Text("Artista") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                                    FilterChip(
                                        selected = currentFilter is FilterType.Album,
                                        onClick = {
                                            if (hasQuery) {
                                                val match = albums.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                                                if (match != null) {
                                                    currentFilter = FilterType.Album(match.name)
                                                    isSearchActive = false
                                                }
                                            }
                                        },
                                        label = { Text("Álbum") },
                                        leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) }
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                                    FilterChip(
                                        selected = currentFilter is FilterType.Playlist,
                                        onClick = {
                                            if (hasQuery) {
                                                val match = playlists.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                                                if (match != null) {
                                                    currentFilter = FilterType.Playlist(match.id)
                                                    isSearchActive = false
                                                }
                                            }
                                        },
                                        label = { Text("Playlist") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) }
                                    )
                                }
                            }
                        } else {
                            Text("Zonerea")
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = { isSearchActive = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Temporizador",
                                    tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { isQueueOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Abrir cola")
                            }
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentlyPlaying != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
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
                            0 -> {
                                val listState = rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = listState
                                    ) {
                                        items(songs) { song ->
                                            SongItem(
                                                song = song,
                                                onClick = { viewModel.playSong(song) },
                                                onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onDeleteSong = { songToDelete = it },
                                                onShowInfo = { songInfoDialogSong = it }
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

                            1 -> LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(albums) { album ->
                                    AlbumItem(album = album, onClick = {
                                        viewModel.filterByAlbum(album.name)
                                        currentFilter = FilterType.Album(album.name)
                                    })
                                }
                            }

                            2 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(artists) { artist ->
                                    ArtistItem(artist = artist, onClick = {
                                        viewModel.filterByArtist(artist.name)
                                        currentFilter = FilterType.Artist(artist.name)
                                    })
                                }
                            }

                            3 -> Box(modifier = Modifier.fillMaxSize()){
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(playlists) { playlist ->
                                        PlaylistItem(
                                            playlist = playlist,
                                            onClick = {
                                                viewModel.filterByPlaylist(playlist.id)
                                                currentFilter = FilterType.Playlist(playlist.id)
                                            },
                                            onDelete = { playlistToDelete = it }
                                        )
                                    }
                                }
                                FloatingActionButton(
                                    onClick = { showCreatePlaylistDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Crear playlist")
                                }
                            }

                            4 -> {
                                val listState = rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                                        items(favorites) { song ->
                                            SongItem(
                                                song = song,
                                                onClick = { viewModel.playSong(song) },
                                                onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onDeleteSong = { songToDelete = it },
                                                onShowInfo = { songInfoDialogSong = it }
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
                                            val idx = favorites.indexOfFirst { s ->
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

                            5 -> {
                                val listState = rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                                        items(recentlyAdded) { song ->
                                            SongItem(
                                                song = song,
                                                onClick = { viewModel.playSong(song) },
                                                onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onDeleteSong = { songToDelete = it },
                                                onShowInfo = { songInfoDialogSong = it }
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
                                            val idx = recentlyAdded.indexOfFirst { s ->
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

                            6 -> {
                                val listState = rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                                        items(lastPlayed) { song ->
                                            SongItem(
                                                song = song,
                                                onClick = { viewModel.playSong(song) },
                                                onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onDeleteSong = { songToDelete = it },
                                                onShowInfo = { songInfoDialogSong = it }
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
                                            val idx = lastPlayed.indexOfFirst { s ->
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

                            7 -> {
                                val listState = rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                                        items(mostPlayed) { song ->
                                            SongItem(
                                                song = song,
                                                onClick = { viewModel.playSong(song) },
                                                onToggleFavorite = { viewModel.toggleFavoriteSong(song) },
                                                onAddToPlaylist = { songToAddToPlaylist = it },
                                                onDeleteSong = { songToDelete = it },
                                                onShowInfo = { songInfoDialogSong = it }
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
                                            val idx = mostPlayed.indexOfFirst { s ->
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
                    }
                }
            }
        }
    }
}
