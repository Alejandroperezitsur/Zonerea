package com.example.zonerea.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zonerea.model.Playlist

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    ListItem(
        headlineContent = { Text(playlist.name) },
        leadingContent = {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Playlist")
        },
        trailingContent = {
            IconButton(onClick = { onDelete(playlist) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar playlist")
            }
        },
        modifier = Modifier.clickable { onClick(playlist) }
    )
}
