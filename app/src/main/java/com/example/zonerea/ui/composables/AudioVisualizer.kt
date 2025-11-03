package com.example.zonerea.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.max

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    var amplitudes by remember { mutableStateOf(FloatArray(barCount) { 0f }) }
    var peaks by remember { mutableStateOf(FloatArray(barCount) { 0f }) }

    LaunchedEffect(isPlaying) {
        val decay = 0.03f
        while (isPlaying) {
            val newAmps = FloatArray(barCount) { i ->
                val base = amplitudes.getOrNull(i) ?: 0f
                val target = Random.nextFloat()
                // Smooth towards target
                base * 0.6f + target * 0.4f
            }
            // Update falling peaks
            val newPeaks = FloatArray(barCount) { i ->
                val currentPeak = peaks.getOrNull(i) ?: 0f
                val amp = newAmps[i]
                if (amp >= currentPeak) amp else max(0f, currentPeak - decay)
            }
            amplitudes = newAmps
            peaks = newPeaks
            delay(50)
        }
        // Reset when paused
        amplitudes = FloatArray(barCount) { 0f }
        peaks = FloatArray(barCount) { 0f }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 1.5f)
        val gap = barWidth * 0.5f
        var x = 0f
        for (i in 0 until barCount) {
            val amp = amplitudes.getOrNull(i) ?: 0f
            val barHeight = height * amp
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
            // Draw peak cap
            val peak = peaks.getOrNull(i) ?: 0f
            val capHeight = height * 0.02f
            val capY = height - height * peak
            drawRect(
                color = barColor.copy(alpha = 0.9f),
                topLeft = androidx.compose.ui.geometry.Offset(x, capY - capHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, capHeight)
            )
            x += barWidth + gap
        }
    }
}