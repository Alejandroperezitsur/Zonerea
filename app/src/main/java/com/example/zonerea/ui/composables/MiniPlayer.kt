package com.example.zonerea.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import coil.compose.SubcomposeAsyncImage
import com.example.zonerea.model.Song
import com.example.zonerea.ui.theme.ZonereaSpacing
import com.example.zonerea.ui.theme.ZonereaMotion
import com.example.zonerea.ui.theme.ZonereaAlpha
import com.example.zonerea.ui.theme.ZonereaElevation
import com.example.zonerea.ui.theme.ensureReadableColor

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    // Animación suave para el progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mini_player_progress_animation"
    )
    val haptics = LocalHapticFeedback.current
    val titlePulse = remember { Animatable(1f) }
    val artistFade = remember { Animatable(0.72f) }
    LaunchedEffect(song.id) {
        try {
            titlePulse.snapTo(0.96f)
            titlePulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
            artistFade.snapTo(0.48f)
            artistFade.animateTo(
                0.72f,
                tween(durationMillis = ZonereaMotion.durationFast, easing = ZonereaMotion.easingEmphasized)
            )
        } catch (_: Exception) { }
    }
    val surfaceInteraction = remember { MutableInteractionSource() }
    val surfacePressed by surfaceInteraction.collectIsPressedAsState()
    val miniScale by animateFloatAsState(
        targetValue = if (surfacePressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mini_player_press_scale"
    )
    val baseElevation = ZonereaElevation.level4
    val pressedElevation = baseElevation + 2.dp
    val miniElevation by animateDpAsState(
        targetValue = if (surfacePressed) pressedElevation else baseElevation,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mini_player_elevation"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = miniScale
                scaleY = miniScale
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) { // Swipe up
                        onClick()
                    }
                }
            }
            .clickable(
                interactionSource = surfaceInteraction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shadowElevation = miniElevation,
        tonalElevation = ZonereaElevation.level3,
        color = MaterialTheme.colorScheme.surface.copy(alpha = ZonereaAlpha.playerBackgroundTop)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(
                    horizontal = ZonereaSpacing.md,
                    vertical = ZonereaSpacing.sm
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Crossfade(targetState = song, label = "mini_player_art_title") { currentSong ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        SubcomposeAsyncImage(
                            model = currentSong.albumArtUri,
                            contentDescription = currentSong.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop,
                            loading = { Box(Modifier.background(MaterialTheme.colorScheme.surface)) },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Nota Musical",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = ZonereaSpacing.md)
                                .graphicsLayer {
                                    scaleX = titlePulse.value
                                    scaleY = titlePulse.value
                                }
                        ) {
                            val surface = MaterialTheme.colorScheme.surface
                            val titleColor = ensureReadableColor(
                                MaterialTheme.colorScheme.onSurface,
                                surface,
                                minRatio = 4.5
                            )
                            Text(
                                currentSong.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                color = titleColor
                            )
                            Text(
                                currentSong.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                color = LocalContentColor.current.copy(alpha = artistFade.value),
                                modifier = Modifier.graphicsLayer { alpha = artistFade.value }
                            )
                        }
                    }
                }

                AnimatedContent(targetState = isPlaying, label = "play_pause_button") {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onPlayPause()
                        }) {
                            Icon(
                                imageVector = if (it) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (it) "Pausar" else "Reproducir",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            // Retiramos la barra lineal a favor del indicador circular
        }
    }
}
