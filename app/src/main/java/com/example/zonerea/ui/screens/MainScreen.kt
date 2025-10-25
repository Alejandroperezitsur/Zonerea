package com.example.zonerea.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.zonerea.ui.composables.SongItem
import com.example.zonerea.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs) { song ->
            SongItem(song = song, onClick = { viewModel.playSong(song) })
        }
    }
}
