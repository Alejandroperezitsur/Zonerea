package com.example.zonerea.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.floor

@Composable
fun AlphabetIndex(
    letters: List<Char> = listOf('#') + ('A'..'Z').toList() + listOf('Ñ'),
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val density: Density = LocalDensity.current
    var itemHeightPx by remember { mutableStateOf(0f) }
    var currentIndex by remember { mutableStateOf(-1) }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .onSizeChanged { size ->
                // Derive item height from total height and count of letters
                itemHeightPx = if (letters.isNotEmpty()) size.height.toFloat() / letters.size else 0f
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(onDragStart = { offset ->
                    if (itemHeightPx == 0f) {
                        itemHeightPx = with(density) { 24.dp.toPx() }
                    }
                    currentIndex = floor(offset.y / itemHeightPx).toInt().coerceIn(0, letters.lastIndex)
                    onLetterSelected(letters[currentIndex])
                }) { _, dragAmount ->
                    if (itemHeightPx == 0f) return@detectVerticalDragGestures
                    // Accumulate based on drag amount mapped to index delta
                    // This simple mapping uses local position from event not available here,
                    // so we compute delta steps by dragAmount / itemHeightPx.
                    val delta = (dragAmount / itemHeightPx).toInt()
                    if (delta != 0 && currentIndex != -1) {
                        currentIndex = (currentIndex + delta).coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[currentIndex])
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { ch ->
            Text(
                text = ch.toString(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { onLetterSelected(ch) }
            )
        }
    }
}