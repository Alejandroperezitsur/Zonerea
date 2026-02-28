package com.apvlabs.zonerea.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    currentMinutes: Int?,
    onSet: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    var customMinutesText = remember(currentMinutes) { mutableStateOf(currentMinutes?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Temporizador de sueño") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isActive) {
                        val m = currentMinutes ?: 0
                        "Temporizador activo (${m} min)."
                    } else {
                        "Establece un tiempo para detener la música."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onSet(15); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("15 min") }
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { onSet(45); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("45 min") }
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onSet(30); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("30 min") }
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { onSet(60); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("1 hora") }
                    }
                }
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = customMinutesText.value,
                    onValueChange = { customMinutesText.value = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Minutos personalizados") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = customMinutesText.value.toIntOrNull()
                if (m != null && m > 0) {
                    onSet(m)
                    onDismiss()
                }
            }) { Text("Establecer") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isActive) onCancel()
                onDismiss()
            }) { Text(if (isActive) "Cancelar temporizador" else "Cerrar") }
        }
    )
}
