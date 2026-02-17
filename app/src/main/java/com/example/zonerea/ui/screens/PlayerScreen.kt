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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.zonerea.ui.theme.ZonereaSpacing
import com.example.zonerea.ui.theme.ZonereaMotion
import com.example.zonerea.ui.theme.ZonereaAlpha
import com.example.zonerea.ui.theme.ensureReadableColor
import com.example.zonerea.ui.theme.relativeLuminance

private fun blendTowardsSurface(color: Color, surface: Color, ratio: Float): Color {
    val clamped = ratio.coerceIn(0f, 1f)
    val r = color.red * (1f - clamped) + surface.red * clamped
    val g = color.green * (1f - clamped) + surface.green * clamped
    val b = color.blue * (1f - clamped) + surface.blue * clamped
    return Color(r, g, b, alpha = 1f)
}

private fun overlayColor(base: Color, overlay: Color, alpha: Float): Color {
    val a = alpha.coerceIn(0f, 1f)
    val r = base.red * (1f - a) + overlay.red * a
    val g = base.green * (1f - a) + overlay.green * a
    val b = base.blue * (1f - a) + overlay.blue * a
    return Color(r, g, b, 1f)
}

private fun toHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val s: Float
    val h: Float
    if (d == 0f) {
        s = 0f
        h = 0f
    } else {
        s = d / (1f - kotlin.math.abs(2f * l - 1f))
        h = when (max) {
            r -> ((g - b) / d + if (g < b) 6f else 0f)
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } / 6f
    }
    return floatArrayOf(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun fromHsl(hsl: FloatArray): Color {
    val h = hsl[0].coerceIn(0f, 1f)
    val s = hsl[1].coerceIn(0f, 1f)
    val l = hsl[2].coerceIn(0f, 1f)
    if (s == 0f) {
        return Color(l, l, l, 1f)
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val r = hueToRgb(p, q, h + 1f / 3f)
    val g = hueToRgb(p, q, h)
    val b = hueToRgb(p, q, h - 1f / 3f)
    return Color(r, g, b, 1f)
}

private fun reduceSaturation(color: Color, factor: Float = 0.8f): Color {
    val hsl = toHsl(color)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return fromHsl(hsl)
}

private fun clampLuminance(color: Color, min: Double = 0.12, max: Double = 0.85): Color {
    var current = color
    repeat(6) {
        val l = relativeLuminance(current)
        current = when {
            l < min -> overlayColor(current, Color.White, 0.25f)
            l > max -> overlayColor(current, Color.Black, 0.25f)
            else -> return current
        }
    }
    return current
}

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
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessLow
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
                        targetValue = if (isFavorite) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "favorite_pulse"
                    )
                    val favTint by animateColorAsState(
                        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        animationSpec = tween(
                            durationMillis = ZonereaMotion.durationFast,
                            easing = ZonereaMotion.easingEmphasized
                        ),
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
            animationSpec = tween(
                durationMillis = ZonereaMotion.durationMedium,
                easing = ZonereaMotion.easingStandard
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { song ->
            if (song != null) {
                var verticalDragOffset by remember { mutableFloatStateOf(0f) }
                var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
                val context = LocalContext.current
                val haptics = LocalHapticFeedback.current
                val colorScheme = MaterialTheme.colorScheme
                val defaultColor = colorScheme.primary
                val surfaceVariantColor = colorScheme.surfaceVariant
                var visualizerColor by remember { mutableStateOf(defaultColor) }
                var bgColor by remember { mutableStateOf(surfaceVariantColor) }
                var rotation by remember { mutableFloatStateOf(0f) }
                var showEqDialog by remember { mutableStateOf(false) }
                val playPulse = remember { Animatable(1f) }
                val playPressScale = remember { Animatable(1f) }
                var isScrubbing by remember { mutableStateOf(false) }
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isScrubbing) 0.6f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "slider_glow"
                )
                LaunchedEffect(isPlaying) {
                    try {
                        playPulse.snapTo(0.92f)
                        playPulse.animateTo(
                            1f,
                            spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
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
                            val vibrant = palette.getVibrantColor(
                                palette.getDominantColor(visualizerColor.toArgb())
                            )
                            val dominant = palette.getDominantColor(vibrant)
                            visualizerColor = Color(vibrant)
                            val rawBg = Color(dominant)
                            val desaturated = reduceSaturation(rawBg, factor = 0.8f)
                            val clamped = clampLuminance(desaturated, min = 0.12, max = 0.85)
                            val blended = blendTowardsSurface(clamped, colorScheme.surface, 0.28f)
                            bgColor = blended
                        }
                    } catch (_: Throwable) {
                        // keep primary color on failure
                    }
                }

                LaunchedEffect(isPlaying, song.id) {
                    rotation = 0f
                    if (isPlaying) {
                        val frameDelayMs = (ZonereaMotion.Normal / 60).coerceAtLeast(1)
                        while (isPlaying) {
                            rotation = (rotation + 0.6f) % 360f
                            delay(frameDelayMs.toLong())
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    run {
                                        val l = relativeLuminance(bgColor)
                                        val isLight = l > 0.5
                                        val scrimColor = if (isLight) Color.Black else Color.White
                                        val scrimAlpha = if (isLight) ZonereaAlpha.scrimLight else ZonereaAlpha.scrimDark
                                        overlayColor(bgColor, scrimColor, scrimAlpha)
                                    },
                                    visualizerColor.copy(alpha = ZonereaAlpha.visualizerOverlay),
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
                            val fadeSpec = tween<Float>(
                                durationMillis = ZonereaMotion.durationMedium,
                                easing = ZonereaMotion.easingStandard
                            )
                            val scaleSpec = spring<Float>(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                            (fadeIn(animationSpec = fadeSpec) + scaleIn(
                                initialScale = 0.96f,
                                animationSpec = scaleSpec
                            )) togetherWith
                                    (fadeOut(animationSpec = fadeSpec) + scaleOut(
                                        targetScale = 1.02f,
                                        animationSpec = scaleSpec
                                    ))
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
                                    .clip(RoundedCornerShape(ZonereaSpacing.lg))
                                    .border(BorderStroke(1.dp, visualizerColor.copy(alpha = 0.4f)), RoundedCornerShape(ZonereaSpacing.lg))
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

                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(horizontal = ZonereaSpacing.lg),
                        barColor = visualizerColor
                    )

                    val titleColor = ensureReadableColor(
                        colorScheme.onSurface,
                        bgColor,
                        minRatio = 7.0
                    )
                    val artistBaseColor = colorScheme.onSurfaceVariant
                    val artistColor = ensureReadableColor(
                        artistBaseColor,
                        bgColor,
                        minRatio = 4.5
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = ZonereaSpacing.lg)
                    ) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = titleColor,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = artistColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = ZonereaSpacing.xs)
                                .clickable { onOpenArtist(song.artist) }
                        )
                    }

                    // Acciones rápidas: Favorito y Compartir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ZonereaSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(ZonereaSpacing.md, Alignment.CenterHorizontally)
                    ) {
                        val favFabTint by animateColorAsState(
                            targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(
                                durationMillis = ZonereaMotion.durationShort,
                                easing = ZonereaMotion.easingStandard
                            ),
                            label = "favorite_fab_tint"
                        )
                        val favInteraction = remember { MutableInteractionSource() }
                        val favPressed by favInteraction.collectIsPressedAsState()
                        SmallFloatingActionButton(
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.toggleFavoriteSong(song)
                            },
                            interactionSource = favInteraction,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AnimatedContent(targetState = isFavorite, label = "favorite_fab_toggle") { fav ->
                                val favPressTint by animateColorAsState(
                                    targetValue = if (favPressed) MaterialTheme.colorScheme.secondary else favFabTint,
                                    animationSpec = tween(
                                        durationMillis = ZonereaMotion.durationMicro,
                                        easing = ZonereaMotion.easingEmphasized
                                    ),
                                    label = "favorite_press_tint"
                                )
                                val favScaleAnim by animateFloatAsState(
                                    targetValue = if (favPressed) 0.94f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "favorite_fab_press_scale"
                                )
                                Icon(
                                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (fav) "Quitar de favoritos: ${song.title}" else "Añadir a favoritos: ${song.title}",
                                    tint = favPressTint,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = favScaleAnim
                                        scaleY = favScaleAnim
                                    }
                                )
                            }
                        }
                        val shareInteraction = remember { MutableInteractionSource() }
                        val sharePressed by shareInteraction.collectIsPressedAsState()
                        SmallFloatingActionButton(
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Escucha esta canción")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir canción"))
                            },
                            interactionSource = shareInteraction,
                            modifier = Modifier.size(48.dp)
                        ) {
                            val shareTint by animateColorAsState(
                                targetValue = if (sharePressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "share_press_tint"
                            )
                            val shareScale by animateFloatAsState(
                                targetValue = if (sharePressed) 0.96f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "share_press_scale"
                            )
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir ${song.title} — ${song.artist}",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = shareScale
                                    scaleY = shareScale
                                },
                                tint = shareTint
                            )
                        }
                    }

                    // Progress Slider and Timers
                    Column(modifier = Modifier.padding(horizontal = ZonereaSpacing.lg)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
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
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationShort,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "slider_thumb_press_tint"
                            )
                            val sliderScale by animateFloatAsState(
                                targetValue = if (isScrubbing || sliderPressed) 1.04f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "slider_scale"
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
                                    .graphicsLayer {
                                        scaleX = sliderScale
                                        scaleY = sliderScale
                                    }
                                    .semantics {
                                        stateDescription = "Tiempo ${formatDuration((animatedProgress * song.duration).toLong())} de ${formatDuration(song.duration)}"
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
                                text = formatDuration((animatedProgress * song.duration).toLong()),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatDuration(song.duration),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Player Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ZonereaSpacing.md),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shuffleTint by animateColorAsState(
                            targetValue = if (isShuffling) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(
                                durationMillis = ZonereaMotion.durationShort,
                                easing = ZonereaMotion.easingStandard
                            ),
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
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "shuffle_press_tint"
                            )
                            val shuffleScale by animateFloatAsState(
                                targetValue = if (shufflePressed) 0.94f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "shuffle_press_scale"
                            )
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = shuffleScale
                                    scaleY = shuffleScale
                                },
                                tint = shufflePressTint
                            )
                        }
                        val prevInteraction = remember { MutableInteractionSource() }
                        val prevPressed by prevInteraction.collectIsPressedAsState()
                        IconButton(onClick = { viewModel.previous() }, interactionSource = prevInteraction) {
                            val prevTint by animateColorAsState(
                                targetValue = if (prevPressed) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "prev_press_tint"
                            )
                            val prevScale by animateFloatAsState(
                                targetValue = if (prevPressed) 0.94f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "prev_press_scale"
                            )
                            Icon(
                                Icons.Default.SkipPrevious,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        scaleX = prevScale
                                        scaleY = prevScale
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
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationShort,
                                    easing = ZonereaMotion.easingStandard
                                ),
                                label = "play_pause_tint"
                            )
                            val pressTint by animateColorAsState(
                                targetValue = if (playPressed) MaterialTheme.colorScheme.secondary else playTint,
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "play_press_tint"
                            )
                            LaunchedEffect(playPressed) {
                                try {
                                    if (playPressed) {
                                        playPressScale.animateTo(
                                            0.94f,
                                            animationSpec = tween(
                                                durationMillis = ZonereaMotion.durationMicro
                                            )
                                        )
                                    } else {
                                        playPressScale.animateTo(
                                            1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                } catch (_: Throwable) { }
                            }
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val scale = playPulse.value * playPressScale.value
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                tint = pressTint
                            )
                        }
                        val nextInteraction = remember { MutableInteractionSource() }
                        val nextPressed by nextInteraction.collectIsPressedAsState()
                        IconButton(onClick = { viewModel.next() }, interactionSource = nextInteraction) {
                            val nextTint by animateColorAsState(
                                targetValue = if (nextPressed) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "next_press_tint"
                            )
                            val nextScale by animateFloatAsState(
                                targetValue = if (nextPressed) 0.94f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "next_press_scale"
                            )
                            Icon(
                                Icons.Default.SkipNext,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        scaleX = nextScale
                                        scaleY = nextScale
                                    },
                                contentDescription = "Siguiente",
                                tint = nextTint
                            )
                        }
                        val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
                        val repeatTint by animateColorAsState(
                            targetValue = if (repeatActive) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            animationSpec = tween(
                                durationMillis = ZonereaMotion.durationShort,
                                easing = ZonereaMotion.easingStandard
                            ),
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
                                animationSpec = tween(
                                    durationMillis = ZonereaMotion.durationMicro,
                                    easing = ZonereaMotion.easingEmphasized
                                ),
                                label = "repeat_press_tint"
                            )
                            val repeatScale by animateFloatAsState(
                                targetValue = if (repeatPressed) 0.94f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "repeat_press_scale"
                            )
                            Icon(
                                imageVector = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repetir",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = repeatScale
                                    scaleY = repeatScale
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
