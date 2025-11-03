package com.example.zonerea.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zonerea.ui.theme.AppTheme
import com.example.zonerea.ui.theme.appThemeDisplayName
import com.example.zonerea.ui.theme.colorSchemeFor

@Composable
fun ThemePickerDialog(
    current: AppTheme?,
    onSelect: (AppTheme?) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = remember { AppTheme.values().toList() }
    val lightThemes = remember { themes.filter { it.name.startsWith("Light") } }
    val darkThemes = remember { themes.filter { it.name.startsWith("Dark") } }
    val selected = remember(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona tema") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Elige entre colores del sistema o 12 temas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected.value = null }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selected.value == null,
                                onClick = { selected.value = null }
                            )
                            Text(text = "Colores del sistema (dinámico)", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    item { Text("Temas claros", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp)) }
                    items(lightThemes) { theme ->
                        val scheme = colorSchemeFor(theme)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected.value = theme }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selected.value == theme,
                                onClick = { selected.value = theme }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = appThemeDisplayName(theme))
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    Box(modifier = Modifier.size(20.dp).background(scheme.primary))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Box(modifier = Modifier.size(20.dp).background(scheme.secondary))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Box(modifier = Modifier.size(20.dp).background(scheme.tertiary))
                                }
                            }
                        }
                    }
                    item { Text("Temas oscuros", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
                    items(darkThemes) { theme ->
                        val scheme = colorSchemeFor(theme)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected.value = theme }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selected.value == theme,
                                onClick = { selected.value = theme }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = appThemeDisplayName(theme))
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    Box(modifier = Modifier.size(20.dp).background(scheme.primary))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Box(modifier = Modifier.size(20.dp).background(scheme.secondary))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Box(modifier = Modifier.size(20.dp).background(scheme.tertiary))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(selected.value)
                onDismiss()
            }) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}