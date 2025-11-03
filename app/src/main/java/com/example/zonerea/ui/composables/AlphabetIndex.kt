package com.example.zonerea.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var overlayLetter by remember { mutableStateOf<Char?>(null) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .onSizeChanged { size ->
                itemHeightPx = if (letters.isNotEmpty()) size.height.toFloat() / letters.size else 0f
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (itemHeightPx == 0f) {
                            itemHeightPx = with(density) { 24.dp.toPx() }
                        }
                        currentIndex = floor(offset.y / itemHeightPx).toInt().coerceIn(0, letters.lastIndex)
                        overlayLetter = letters[currentIndex]
                        onLetterSelected(letters[currentIndex])
                    },
                    onDragEnd = {
                        overlayLetter = null
                    }
                ) { _, dragAmount ->
                    if (itemHeightPx == 0f) return@detectVerticalDragGestures
                    val delta = (dragAmount / itemHeightPx).toInt()
                    if (delta != 0 && currentIndex != -1) {
                        currentIndex = (currentIndex + delta).coerceIn(0, letters.lastIndex)
                        overlayLetter = letters[currentIndex]
                        onLetterSelected(letters[currentIndex])
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            letters.forEach { ch ->
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable {
                            overlayLetter = ch
                            onLetterSelected(ch)
                            scope.launch {
                                delay(700)
                                overlayLetter = null
                            }
                        }
                )
            }
        }
        overlayLetter?.let { ch ->
            Text(
                text = ch.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}