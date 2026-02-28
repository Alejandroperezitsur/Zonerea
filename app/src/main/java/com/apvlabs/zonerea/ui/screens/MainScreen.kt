package com.apvlabs.zonerea.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.apvlabs.zonerea.model.Playlist
import com.apvlabs.zonerea.model.Song
import com.apvlabs.zonerea.ui.composables.*
import com.apvlabs.zonerea.ui.composables.AlphabetIndex
import com.apvlabs.zonerea.ui.composables.SleepTimerDialog
import com.apvlabs.zonerea.ui.viewmodel.FilterType
import com.apvlabs.zonerea.ui.viewmodel.MainViewModel
import com.apvlabs.zonerea.ui.theme.appThemeDisplayName
import com.apvlabs.zonerea.ui.theme.colorSchemeFor
import com.apvlabs.zonerea.ui.theme.fadeInSpec
import com.apvlabs.zonerea.ui.theme.fadeOutSpec
import com.apvlabs.zonerea.ui.theme.slideSpec
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.apvlabs.zonerea.ui.theme.ZonereaSpacing
import com.apvlabs.zonerea.ui.theme.ZonereaMotion

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
    val currentTheme by viewModel.selectedTheme.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var isQueueOpen by rememberSaveable { mutableStateOf(false) }
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var showThemePickerDialog by rememberSaveable { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf<FilterType>(FilterType.None) }
    var songInfoDialogSong by remember { mutableStateOf<Song?>(null) }

    // Launcher para solicitar confirmación de borrado del sistema (Android 10+)
    val pendingDeleteIntent by viewModel.pendingDeleteIntent.collectAsState()
    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onSystemDeleteCompleted(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(pendingDeleteIntent) {
        pendingDeleteIntent?.let { sender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

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
            },
            onOpenPlayer = { isPlayerExpanded = true }
        )
        return
    }

    if (isScanning) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(ZonereaSpacing.md))
                Text("Escaneando tu música...")
            }
        }
        return
    }

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(ZonereaSpacing.md))
                Text(
                    "No se encontraron canciones en tu dispositivo.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(ZonereaSpacing.lg))
                Button(onClick = { viewModel.scanForSongs() }) {
                    Text("Re-escanear biblioteca")
                }
            }
        }
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

    if (showThemePickerDialog) {
        ThemePickerDialog(
            current = currentTheme,
            onSelect = { theme -> viewModel.setTheme(theme) },
            onDismiss = { showThemePickerDialog = false }
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
                songToAddToPlaylist?.let { song ->
                    viewModel.createPlaylistAndAddSong(song, playlistName)
                }
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
        enter = slideInVertically(animationSpec = slideSpec()) { it } + fadeIn(animationSpec = fadeInSpec()),
        exit = slideOutVertically(animationSpec = slideSpec()) { it } + fadeOut(animationSpec = fadeOutSpec())
    ) {
        PlayerScreen(
            viewModel = viewModel,
            onClose = { isPlayerExpanded = false },
            onOpenArtist = { artist ->
                viewModel.filterByArtist(artist)
                currentFilter = FilterType.Artist(artist)
                isPlayerExpanded = false
            }
        )
    }
    AnimatedVisibility(
        visible = isQueueOpen,
        enter = slideInVertically(animationSpec = slideSpec()) { it } + fadeIn(animationSpec = fadeInSpec()),
        exit = slideOutVertically(animationSpec = slideSpec()) { it } + fadeOut(animationSpec = fadeOutSpec())
    ) {
        QueueScreen(
            viewModel = viewModel,
            onClose = { isQueueOpen = false },
            onOpenPlayer = { isPlayerExpanded = true }
        )
    }
    if (!isPlayerExpanded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            var localQuery by rememberSaveable { mutableStateOf(searchQuery) }
                            LaunchedEffect(localQuery) {
                                delay(200)
                                if (localQuery != searchQuery) {
                                    viewModel.onSearchQueryChange(localQuery)
                                }
                            }

                            fun highlight(text: String, q: String): AnnotatedString {
                                if (q.isBlank()) return AnnotatedString(text)
                                val idx = text.indexOf(q, ignoreCase = true)
                                return if (idx >= 0) {
                                    buildAnnotatedString {
                                        append(text.substring(0, idx))
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(text.substring(idx, idx + q.length))
                                        }
                                        append(text.substring(idx + q.length))
                                    }
                                } else AnnotatedString(text)
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                DockedSearchBar(
                                    query = localQuery,
                                    onQueryChange = { localQuery = it },
                                    onSearch = {
                                        // Al confirmar búsqueda, cerramos el modo búsqueda y mantenemos el filtro por query
                                        isSearchActive = false
                                    },
                                    active = isSearchActive,
                                    onActiveChange = { isSearchActive = it },
                                    placeholder = { Text("Buscar canciones, artistas, álbumes, playlists") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            localQuery = ""
                                            viewModel.onSearchQueryChange("")
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val q = localQuery.lowercase()
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
                                                    headlineContent = { Text(highlight(artist.name, q)) },
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
                                                    headlineContent = { Text(highlight(album.name, q)) },
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
                                                    headlineContent = { Text(highlight(playlist.name, q)) },
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
                                                    headlineContent = { Text(highlight(song.title, q)) },
                                                    leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                                    overlineContent = { Text(highlight(song.artist, q)) },
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

                                Row(modifier = Modifier.fillMaxWidth().padding(top = ZonereaSpacing.sm)) {
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
                                    Spacer(modifier = Modifier.width(ZonereaSpacing.sm))
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
                                    Spacer(modifier = Modifier.width(ZonereaSpacing.sm))
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
                            Text(
                                text = "Zonerea",
                                style = MaterialTheme.typography.headlineSmall
                            )
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
                            IconButton(onClick = { isQueueOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Abrir cola")
                            }
                            var showOverflow by rememberSaveable { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showOverflow = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                                }
                                DropdownMenu(
                                    expanded = showOverflow,
                                    onDismissRequest = { showOverflow = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Re-escanear biblioteca") },
                                        onClick = {
                                            showOverflow = false
                                            if (!isScanning) {
                                                viewModel.scanForSongs()
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Temporizador de sueño") },
                                        onClick = {
                                            showOverflow = false
                                            showSleepTimerDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cambiar tema") },
                                        onClick = {
                                            showOverflow = false
                                            showThemePickerDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Palette, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            bottomBar = {
                Column {
                    val haptics = LocalHapticFeedback.current
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 6.dp
                    ) {
                        tabs.take(4).forEachIndexed { index, (title, icon) ->
                            val isSelected = pagerState.currentPage == index
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.12f else 1f,
                                label = "nav_icon_scale"
                            )
                            val tint by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                label = "nav_tint"
                            )
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = title,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                        tint = tint
                                    )
                                },
                                label = { Text(title, color = tint, maxLines = 1) }
                            )
                        }
                    }
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
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    Crossfade(
                        targetState = page,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = ZonereaMotion.durationMedium,
                            easing = ZonereaMotion.easingStandard
                        ),
                        label = "page_crossfade"
                    ) { targetPage ->
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
                                            .padding(end = ZonereaSpacing.xs)
                                    )
                                }
                            }

                            1 -> LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(ZonereaSpacing.sm)
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

                            3 -> Box(modifier = Modifier.fillMaxSize()) {
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
                                if (playlists.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = ZonereaSpacing.lg),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(ZonereaSpacing.md))
                                        Text(
                                            text = "Aún no tienes playlists",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(ZonereaSpacing.lg))
                                        Button(onClick = { showCreatePlaylistDialog = true }) {
                                            Text("Crear playlist")
                                        }
                                    }
                                }
                                FloatingActionButton(
                                    onClick = { showCreatePlaylistDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(ZonereaSpacing.md)
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
