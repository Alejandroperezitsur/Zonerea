package com.apvlabs.zonerea.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.apvlabs.zonerea.model.Playlist
import com.apvlabs.zonerea.model.Song

@Composable
fun ConfirmDeleteDialog(
    song: Song? = null,
    playlist: Playlist? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = if (song != null) "Eliminar canción" else "Eliminar playlist"
    val text = if (song != null) {
        "¿Estás seguro de que quieres eliminar '${song.title}'?"
    } else {
        "¿Estás seguro de que quieres eliminar la playlist '${playlist?.name}'?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
