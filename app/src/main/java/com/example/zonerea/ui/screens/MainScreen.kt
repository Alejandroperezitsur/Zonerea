package com.example.zonerea.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.zonerea.ui.composables.MiniPlayer
import com.example.zonerea.ui.composables.SongItem
import com.example.zonerea.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val lastPlayed by viewModel.lastPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Songs" to Icons.Default.MusicNote,
        "Favorites" to Icons.Default.Favorite,
        "Recent" to Icons.Default.History,
        "Last" to Icons.Default.Schedule,
        "Top" to Icons.AutoMirrored.Filled.TrendingUp
    )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

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
                TabRow(selectedTabIndex = pagerState.currentPage) {
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
                    val listToShow = when (page) {
                        0 -> songs
                        1 -> favorites
                        2 -> recentlyAdded
                        3 -> lastPlayed
                        4 -> mostPlayed
                        else -> emptyList()
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(listToShow) { song ->
                            SongItem(song = song, onClick = { viewModel.playSong(song) })
                        }
                    }
                }
            }
        }
    }
}
