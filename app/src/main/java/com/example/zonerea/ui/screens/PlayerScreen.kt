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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.zonerea.playback.Player
import com.example.zonerea.ui.viewmodel.MainViewModel
import com.example.zonerea.ui.composables.AudioVisualizer
import com.example.zonerea.ui.composables.EqualizerDialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.delay

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
                    val favScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.12f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "favorite_pulse"
                    )
                    val favTint by animateColorAsState(
                        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                        label = "favorite_tint"
                    )
                    IconButton(onClick = { currentlyPlaying?.let { viewModel.toggleFavoriteSong(it) } }) {
                        AnimatedContent(targetState = isFavorite, label = "favorite_toggle") { fav ->
                            Icon(
                                imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorito",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = favScale
                                    scaleY = favScale
                                },
                                tint = favTint
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Crossfade(
            targetState = currentlyPlaying,
            animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { song ->
            if (song != null) {
                var verticalDragOffset by remember { mutableFloatStateOf(0f) }
                var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
                val context = LocalContext.current
                val haptics = LocalHapticFeedback.current
                val defaultColor = MaterialTheme.colorScheme.primary
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                var visualizerColor by remember { mutableStateOf(defaultColor) }
                var bgColor by remember { mutableStateOf(surfaceVariantColor) }
                var rotation by remember { mutableFloatStateOf(0f) }
                var showEqDialog by remember { mutableStateOf(false) }
                val playPulse = remember { Animatable(1f) }
                var isScrubbing by remember { mutableStateOf(false) }
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isScrubbing) 0.6f else 0f,
                    label = "slider_glow"
                )
                LaunchedEffect(isPlaying) {
                    try {
                        playPulse.snapTo(0.92f)
                        playPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                    } catch (_: Throwable) { }
                }
                LaunchedEffect(song.albumArtUri, surfaceVariantColor) {
                    visualizerColor = defaultColor
                    bgColor = surfaceVariantColor
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
                            bgColor = Color(palette.getDominantColor(colorInt))
                        }
                    } catch (_: Throwable) {
                        // keep primary color on failure
                    }
                }

                LaunchedEffect(isPlaying, song.id) {
                    rotation = 0f
                    if (isPlaying) {
                        while (isPlaying) {
                            rotation = (rotation + 0.6f) % 360f
                            delay(16)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    bgColor.copy(alpha = 0.80f),
                                    visualizerColor.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
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
                                        horizontalDragOffset < -200 -> {
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.next() // Swipe left
                                        }
                                        horizontalDragOffset > 200 -> {
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.previous() // Swipe right
                                        }
                                    }
                                    horizontalDragOffset = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDragOffset += dragAmount
                                    rotation = (dragAmount / 8f).coerceIn(-12f, 12f)
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
                        }
                    ) { artUri ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            // Aura radial sutil detrás de la carátula
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .aspectRatio(1f)
                                    .graphicsLayer { alpha = 0.9f }
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                visualizerColor.copy(alpha = 0.18f),
                                                visualizerColor.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                            SubcomposeAsyncImage(
                                model = artUri,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(BorderStroke(1.dp, visualizerColor.copy(alpha = 0.4f)), RoundedCornerShape(24.dp))
                                    .graphicsLayer {
                                        rotationZ = rotation
                                        shadowElevation = 12f
                                    }
                                    .pointerInput(song.id) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleFavoriteSong(song)
                                            },
                                            onLongPress = { showEqDialog = true }
                                        )
                                    }
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

                    // Acciones rápidas: Favorito y Compartir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        val favFabTint by animateColorAsState(
                            targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                            label = "favorite_fab_tint"
                        )
                        val favInteraction = remember { MutableInteractionSource() }
                        val favPressed by favInteraction.collectIsPressedAsState()
                        SmallFloatingActionButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleFavoriteSong(song)
                        }, interactionSource = favInteraction) {
                            AnimatedContent(targetState = isFavorite, label = "favorite_fab_toggle") { fav ->
                                val favPressTint by animateColorAsState(
                                    targetValue = if (favPressed) MaterialTheme.colorScheme.secondary else favFabTint,
                                    animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                    label = "favorite_press_tint"
                                )
                                Icon(
                                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (fav) "Quitar de favoritos: ${song.title}" else "Añadir a favoritos: ${song.title}",
                                    tint = favPressTint,
                                    modifier = Modifier.graphicsLayer {
                                        val scale = if (favPressed) 0.94f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                )
                            }
                        }
                        val shareInteraction = remember { MutableInteractionSource() }
                        val sharePressed by shareInteraction.collectIsPressedAsState()
                        SmallFloatingActionButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Escucha esta canción")
                                putExtra(android.content.Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir canción"))
                        }, interactionSource = shareInteraction) {
                            val shareTint by animateColorAsState(
                                targetValue = if (sharePressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "share_press_tint"
                            )
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir ${song.title} — ${song.artist}",
                                modifier = Modifier.graphicsLayer {
                                    val scale = if (sharePressed) 0.94f else 1f
                                    scaleX = scale
                                    scaleY = scale
                                },
                                tint = shareTint
                            )
                        }
                    }

                    // Progress Slider and Timers
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Halo sutil detrás del slider cuando se está haciendo scrubbing
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = glowAlpha }
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                visualizerColor.copy(alpha = 0f),
                                                visualizerColor.copy(alpha = 0.35f),
                                                visualizerColor.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                            val sliderInteraction = remember { MutableInteractionSource() }
                            val sliderPressed by sliderInteraction.collectIsPressedAsState()
                            val thumbTint by animateColorAsState(
                                targetValue = if (isScrubbing || sliderPressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "slider_thumb_press_tint"
                            )
                            Slider(
                                value = animatedProgress,
                                onValueChange = {
                                    if (!isScrubbing) {
                                        isScrubbing = true
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    }
                                    viewModel.seek(it)
                                },
                                onValueChangeFinished = {
                                    isScrubbing = false
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        stateDescription = "Tiempo ${formatDuration((animatedProgress * (song.duration ?: 0L)).toLong())} de ${formatDuration(song.duration ?: 0L)}"
                                    },
                                interactionSource = sliderInteraction,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = visualizerColor,
                                    inactiveTrackColor = visualizerColor.copy(alpha = 0.25f),
                                    thumbColor = thumbTint
                                )
                            )
                        }
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
                        val shuffleTint by animateColorAsState(
                            targetValue = if (isShuffling) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                            label = "shuffle_tint"
                        )
                        val shuffleInteraction = remember { MutableInteractionSource() }
                        val shufflePressed by shuffleInteraction.collectIsPressedAsState()
                        IconButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleShuffle()
                        }, interactionSource = shuffleInteraction, modifier = Modifier.semantics {
                            selected = isShuffling
                            stateDescription = if (isShuffling) "Aleatorio activado" else "Aleatorio desactivado"
                        }) {
                            val shufflePressTint by animateColorAsState(
                                targetValue = if (shufflePressed) MaterialTheme.colorScheme.secondary else shuffleTint,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "shuffle_press_tint"
                            )
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                modifier = Modifier.graphicsLayer {
                                    val scale = if (shufflePressed) 0.94f else 1f
                                    scaleX = scale
                                    scaleY = scale
                                },
                                tint = shufflePressTint
                            )
                        }
                        val prevInteraction = remember { MutableInteractionSource() }
                        val prevPressed by prevInteraction.collectIsPressedAsState()
                        IconButton(onClick = { viewModel.previous() }, interactionSource = prevInteraction) {
                            val prevTint by animateColorAsState(
                                targetValue = if (prevPressed) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "prev_press_tint"
                            )
                            Icon(
                                Icons.Default.SkipPrevious,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        val scale = if (prevPressed) 0.94f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                contentDescription = "Anterior",
                                tint = prevTint
                            )
                        }
                        val playInteraction = remember { MutableInteractionSource() }
                        val playPressed by playInteraction.collectIsPressedAsState()
                        IconButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.togglePlayPause()
                        }, modifier = Modifier.size(72.dp), interactionSource = playInteraction) {
                            val playTint by animateColorAsState(
                                targetValue = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "play_pause_tint"
                            )
                            val pressTint by animateColorAsState(
                                targetValue = if (playPressed) MaterialTheme.colorScheme.secondary else playTint,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "play_press_tint"
                            )
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = playPulse.value
                                        scaleY = playPulse.value
                                    },
                                tint = pressTint
                            )
                        }
                        val nextInteraction = remember { MutableInteractionSource() }
                        val nextPressed by nextInteraction.collectIsPressedAsState()
                        IconButton(onClick = { viewModel.next() }, interactionSource = nextInteraction) {
                            val nextTint by animateColorAsState(
                                targetValue = if (nextPressed) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "next_press_tint"
                            )
                            Icon(
                                Icons.Default.SkipNext,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        val scale = if (nextPressed) 0.94f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                contentDescription = "Siguiente",
                                tint = nextTint
                            )
                        }
                        val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
                        val repeatTint by animateColorAsState(
                            targetValue = if (repeatActive) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                            label = "repeat_tint"
                        )
                        val repeatInteraction = remember { MutableInteractionSource() }
                        val repeatPressed by repeatInteraction.collectIsPressedAsState()
                        IconButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleRepeat()
                        }, interactionSource = repeatInteraction, modifier = Modifier.semantics {
                            selected = repeatActive
                            stateDescription = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> "Repetir pista activado"
                                Player.REPEAT_MODE_OFF -> "Repetición desactivada"
                                else -> "Repetir todo activado"
                            }
                        }) {
                            val repeatPressTint by animateColorAsState(
                                targetValue = if (repeatPressed) MaterialTheme.colorScheme.secondary else repeatTint,
                                animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
                                label = "repeat_press_tint"
                            )
                            Icon(
                                imageVector = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repetir",
                                modifier = Modifier.graphicsLayer {
                                    val scale = if (repeatPressed) 0.94f else 1f
                                    scaleX = scale
                                    scaleY = scale
                                },
                                tint = repeatPressTint
                            )
                        }
                    }
                    if (showEqDialog) {
                        EqualizerDialog(
                            sendCommand = { cmd, params -> viewModel.sendPlaybackCommand(cmd, params) },
                            onDismiss = { showEqDialog = false }
                        )
                    }
                }
            }
        }
    }
}
