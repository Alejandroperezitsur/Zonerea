package com.example.zonerea.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.zonerea.model.Song

@Composable
fun SongItem(
    song: Song,
    onClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onShowInfo: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "song_item_press"
    )

    ListItem(
        headlineContent = {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingContent = {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Carátula del álbum",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onShowInfo(song) },
                loading = {
                    Box(modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                error = {
                    Box(modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Información") },
                        onClick = {
                            onShowInfo(song)
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (song.isFavorite) "Quitar de favoritos" else "Añadir a favoritos") },
                        onClick = {
                            onToggleFavorite(song)
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Añadir a playlist") },
                        onClick = {
                            onAddToPlaylist(song)
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                        }
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Quitar de esta playlist") },
                            onClick = {
                                onRemoveFromPlaylist(song)
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                onDeleteSong(song)
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize()
            .clickable(interactionSource = interactionSource, indication = null) { onClick(song) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
