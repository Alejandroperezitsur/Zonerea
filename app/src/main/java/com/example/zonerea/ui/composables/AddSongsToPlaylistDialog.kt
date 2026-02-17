package com.example.zonerea.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zonerea.model.Song

@Composable
fun AddSongsToPlaylistDialog(
    availableSongs: List<Song>,
    alreadyInPlaylistIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Song>) -> Unit
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar canciones a la playlist") },
        text = {
            Column {
                Text(
                    "Selecciona una o varias canciones para añadir",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn {
                    items(availableSongs) { song ->
                        val isDisabled = alreadyInPlaylistIds.contains(song.id)
                        val isChecked = selected.contains(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(vertical = 6.dp)
                                .let { m -> if (!isDisabled) m.clickable {
                                    selected = if (isChecked) selected - song.id else selected + song.id
                                } else m }
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = if (isDisabled) null else { checked ->
                                    selected = if (checked) selected + song.id else selected - song.id
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val selectedSongs = availableSongs.filter { selected.contains(it.id) }
                onConfirm(selectedSongs)
            }) {
                Text("Añadir")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
