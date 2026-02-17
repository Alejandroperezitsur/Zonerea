package com.example.zonerea.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.example.zonerea.ui.theme.ZonereaSpacing

@Composable
fun PermissionsRequiredScreen(
    storageGranted: Boolean,
    notificationsGranted: Boolean,
    isAndroid13Plus: Boolean,
    preferOpenSettings: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ZonereaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(ZonereaSpacing.md))
        Text(
            text = "Se requieren permisos para continuar",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(ZonereaSpacing.sm))
        Text(
            text = if (isAndroid13Plus) {
                "Concede permiso de música (almacenamiento) y notificaciones para usar Zonerea."
            } else {
                "Concede permiso de música (almacenamiento) para usar Zonerea."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (preferOpenSettings) {
            Spacer(modifier = Modifier.height(ZonereaSpacing.sm))
            Text(
                text = "Has denegado permanentemente alguno de los permisos. Abre Ajustes para concederlos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(ZonereaSpacing.lg))

        PermissionStatusRow(label = "Almacenamiento", granted = storageGranted)
        if (isAndroid13Plus) {
            Spacer(modifier = Modifier.height(ZonereaSpacing.sm))
            PermissionStatusRow(label = "Notificaciones", granted = notificationsGranted)
        }

        Spacer(modifier = Modifier.height(ZonereaSpacing.lg))
        if (preferOpenSettings) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes")
            }
            Spacer(modifier = Modifier.height(ZonereaSpacing.sm))
            TextButton(onClick = onRequestPermissions, enabled = false) {
                Text("Conceder permisos")
            }
        } else {
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Conceder permisos")
            }
            Spacer(modifier = Modifier.height(ZonereaSpacing.sm))
            TextButton(onClick = onOpenSettings) {
                Text("Abrir ajustes")
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (granted) Icons.Default.CheckCircle else Icons.Default.Info
        Icon(imageVector = icon, contentDescription = null, tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: " + if (granted) "Concedido" else "Denegado",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
