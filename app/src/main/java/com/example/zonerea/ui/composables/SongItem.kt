package com.example.zonerea.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.zonerea.model.Song

@Composable
fun SongItem(song: Song, onClick: (Song) -> Unit) {
    ListItem(
        headlineContent = { 
            Text(
                song.title, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            ) 
        },
        supportingContent = { 
            Text(
                song.artist, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            ) 
        },
        leadingContent = {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
                loading = {
                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    }
                },
                error = {
                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    }
                }
            )
        },
        modifier = Modifier.clickable { onClick(song) }
    )
}
