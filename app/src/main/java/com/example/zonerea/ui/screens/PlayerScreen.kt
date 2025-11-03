package com.example.zonerea.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.palette.graphics.Palette
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import com.example.zonerea.playback.Player
import com.example.zonerea.ui.viewmodel.MainViewModel
import com.example.zonerea.ui.composables.AudioVisualizer
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val rawProgress by viewModel.progress.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    
    // Animación suave para el progreso
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progress_animation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo Ahora", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { currentlyPlaying?.let { viewModel.toggleFavoriteSong(it) } }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Crossfade(
            targetState = currentlyPlaying,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "player_content"
        ) { song ->
            if (song != null) {
                var verticalDragOffset by remember { mutableFloatStateOf(0f) }
                var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
                val context = LocalContext.current
                val defaultColor = MaterialTheme.colorScheme.primary
                var visualizerColor by remember { mutableStateOf(defaultColor) }
                LaunchedEffect(song.albumArtUri) {
                    visualizerColor = defaultColor
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .allowHardware(false)
                            .build()
                        val result = ImageLoader(context).execute(request)
                        val drawable = (result as? SuccessResult)?.drawable as? BitmapDrawable
                        val bitmap = drawable?.bitmap
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val colorInt = palette.getVibrantColor(
                                palette.getDominantColor(visualizerColor.toArgb())
                            )
                            visualizerColor = Color(colorInt)
                        }
                    } catch (_: Throwable) {
                        // keep primary color on failure
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (verticalDragOffset > 200) { // Swipe down threshold
                                        onClose()
                                    }
                                    verticalDragOffset = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    verticalDragOffset += dragAmount
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        horizontalDragOffset < -200 -> viewModel.next() // Swipe left
                                        horizontalDragOffset > 200 -> viewModel.previous() // Swipe right
                                    }
                                    horizontalDragOffset = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDragOffset += dragAmount
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    // Album Art with transitions
                    AnimatedContent(
                        targetState = song.albumArtUri,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                                    (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.05f))
                        }, label = "album_art_transition"
                    ) { artUri ->
                        SubcomposeAsyncImage(
                            model = artUri,
                            contentDescription = song.title,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .animateContentSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Nota Musical",
                                        modifier = Modifier.size(96.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }

                    // Visualizador (animado)
                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        barColor = visualizerColor
                    )

                    // Song Info
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(song.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onOpenArtist(song.artist) }
                        )
                    }

                    // Progress Slider and Timers
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Slider(
                            value = animatedProgress,
                            onValueChange = { viewModel.seek(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration((animatedProgress * (song.duration ?: 0L)).toLong()),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatDuration(song.duration ?: 0L),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Player Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = if (isShuffling) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { viewModel.previous() }) {
                            Icon(Icons.Default.SkipPrevious, modifier = Modifier.size(48.dp), contentDescription = "Anterior")
                        }
                        IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.next() }) {
                            Icon(Icons.Default.SkipNext, modifier = Modifier.size(48.dp), contentDescription = "Siguiente")
                        }
                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repetir",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }
    }
}
