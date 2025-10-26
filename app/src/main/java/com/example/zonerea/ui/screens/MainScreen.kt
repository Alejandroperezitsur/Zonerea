package com.example.zonerea.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var searchQuery by remember { mutableStateOf("") }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val tabs = listOf("All Songs", "Favorites", "Recently Added", "Last Played", "Most Played")

    if (isPlayerExpanded) {
        PlayerScreen(
            viewModel = viewModel,
            onClose = { isPlayerExpanded = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                currentlyPlaying?.let {
                    MiniPlayer(
                        song = it,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSeek = { position -> viewModel.seek(position) },
                        onClick = { isPlayerExpanded = true }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
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
                    pageCount = tabs.size,
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> { // All Songs
                            Column {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        viewModel.search(it)
                                    },
                                    label = { Text("Search") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(songs) { song ->
                                        SongItem(song = song, onClick = { viewModel.playSong(song) })
                                    }
                                }
                            }
                        }
                        1 -> { // Favorites
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(favorites) { song ->
                                    SongItem(song = song, onClick = { viewModel.playSong(song) })
                                }
                            }
                        }
                        2 -> { // Recently Added
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(recentlyAdded) { song ->
                                    SongItem(song = song, onClick = { viewModel.playSong(song) })
                                }
                            }
                        }
                        3 -> { // Last Played
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(lastPlayed) { song ->
                                    SongItem(song = song, onClick = { viewModel.playSong(song) })
                                }
                            }
                        }
                        4 -> { // Most Played
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(mostPlayed) { song ->
                                    SongItem(song = song, onClick = { viewModel.playSong(song) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
