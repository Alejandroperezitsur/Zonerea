package com.example.zonerea.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zonerea.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val queue by viewModel.queue.collectAsState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulated by remember { mutableStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cola de reproducción") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            state = listState
        ) {
            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                AnimatedContent(targetState = index, label = "queue_item_transition") { _ ->
                var menuExpanded by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text(song.title) },
                    overlineContent = { Text(song.artist, style = MaterialTheme.typography.bodySmall) },
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
                                    text = { Text("Reproducir") },
                                    onClick = {
                                        viewModel.playAt(index)
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    }
                                )
                                if (index > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Mover arriba") },
                                        onClick = {
                                            viewModel.moveQueueItem(index, index - 1)
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = null)
                                        }
                                    )
                                }
                                if (index < queue.size - 1) {
                                    DropdownMenuItem(
                                        text = { Text("Mover abajo") },
                                        onClick = {
                                            viewModel.moveQueueItem(index, index + 1)
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Eliminar de cola") },
                                    onClick = {
                                        viewModel.removeQueueItem(index)
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                            }
                        }
                        // Drag handle
                        IconButton(
                            onClick = {},
                            modifier = Modifier.pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragAccumulated = 0f
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragAccumulated = 0f
                                    }
                                ) { _, dragAmount ->
                                    val current = draggingIndex ?: return@detectDragGestures
                                    dragAccumulated += dragAmount.y
                                    val steps = (dragAccumulated / itemHeightPx).toInt()
                                    if (steps != 0) {
                                        val target = (current + steps).coerceIn(0, queue.lastIndex)
                                        if (target != current) {
                                            viewModel.moveQueueItem(current, target)
                                            draggingIndex = target
                                            // reduce accumulated by full steps
                                            dragAccumulated -= steps * itemHeightPx
                                        }
                                    }
                                    // Auto-scroll near edges
                                    val first = listState.firstVisibleItemIndex
                                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
                                    if (current <= first + 1 && dragAmount.y < 0) {
                                        // scroll up by one item
                                        val target = (first - 1).coerceAtLeast(0)
                                        scope.launch { listState.animateScrollToItem(target) }
                                    } else if (current >= last - 1 && dragAmount.y > 0) {
                                        // scroll down by one item
                                        val target = (last + 1).coerceAtMost(queue.lastIndex)
                                        scope.launch { listState.animateScrollToItem(target) }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Arrastrar")
                        }
                    }
                )
                }
            }
        }
    }
}