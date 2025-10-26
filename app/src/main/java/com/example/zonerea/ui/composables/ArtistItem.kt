package com.example.zonerea.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zonerea.model.Artist

@Composable
fun ArtistItem(artist: Artist, onClick: (Artist) -> Unit) {
    ListItem(
        headlineContent = { Text(artist.name) },
        supportingContent = { Text("${artist.songCount} canciones") },
        leadingContent = {
            Icon(Icons.Default.Person, contentDescription = "Artista")
        },
        modifier = Modifier.clickable { onClick(artist) }
    )
}
