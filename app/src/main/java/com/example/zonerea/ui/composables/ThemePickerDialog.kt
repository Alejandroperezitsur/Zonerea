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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
                val currentLabel = current?.let { appThemeDisplayName(it) } ?: "Sistema"
                Text(
                    text = "Tema actual: " + currentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Elige colores del sistema, Automático o 12 temas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    item {
                        Card(
                            onClick = { selected.value = null },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (selected.value == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                RadioButton(
                                    selected = selected.value == null,
                                    onClick = { selected.value = null }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Row {
                                        Icon(imageVector = Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(text = "Colores del sistema (dinámico)")
                                    }
                                    Text(
                                        text = "Usa Material You si está disponible",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            onClick = { selected.value = AppTheme.Auto },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (selected.value == AppTheme.Auto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                RadioButton(
                                    selected = selected.value == AppTheme.Auto,
                                    onClick = { selected.value = AppTheme.Auto }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(text = appThemeDisplayName(AppTheme.Auto), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Cambia claro/oscuro según la hora",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item { Text("Temas claros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
                    items(lightThemes) { theme ->
                        val scheme = colorSchemeFor(theme)
                        Card(
                            onClick = { selected.value = theme },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (selected.value == theme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                RadioButton(
                                    selected = selected.value == theme,
                                    onClick = { selected.value = theme }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(text = appThemeDisplayName(theme), style = MaterialTheme.typography.bodyLarge)
                                    Row(modifier = Modifier.padding(top = 6.dp)) {
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.primary))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.secondary))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.tertiary))
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Temas oscuros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, start = 4.dp)) }
                    items(darkThemes) { theme ->
                        val scheme = colorSchemeFor(theme)
                        Card(
                            onClick = { selected.value = theme },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (selected.value == theme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                RadioButton(
                                    selected = selected.value == theme,
                                    onClick = { selected.value = theme }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(text = appThemeDisplayName(theme), style = MaterialTheme.typography.bodyLarge)
                                    Row(modifier = Modifier.padding(top = 6.dp)) {
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.primary))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.secondary))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(scheme.tertiary))
                                    }
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