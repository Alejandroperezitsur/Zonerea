package com.apvlabs.zonerea.ui.composables

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EqualizerDialog(
    sendCommand: (String, Bundle) -> Bundle?,
    onDismiss: () -> Unit,
) {
    var bandCount by remember { mutableStateOf(0) }
    var minLevel by remember { mutableStateOf<Short>(-1500) }
    var maxLevel by remember { mutableStateOf<Short>(1500) }
    var levels by remember { mutableStateOf<ShortArray?>(null) }
    var bassStrength by remember { mutableStateOf(0f) } // 0..1000
    var virtStrength by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val res = sendCommand("eq:get_bands", Bundle())
        res?.let {
            bandCount = it.getInt("bands", 0)
            minLevel = it.getShort("min", -1500)
            maxLevel = it.getShort("max", 1500)
            levels = it.getShortArray("levels")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ecualizador", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val lvls = levels ?: ShortArray(bandCount) { 0 }
                for (i in 0 until bandCount) {
                    val min = minLevel.toFloat()
                    val max = maxLevel.toFloat()
                    var sliderVal by remember(i) { mutableStateOf(lvls[i].toFloat()) }
                    Text(text = "Banda ${i + 1}")
                    Slider(
                        value = sliderVal,
                        onValueChange = {
                            sliderVal = it
                            val b = Bundle().apply {
                                putInt("band", i)
                                putShort("level", it.roundToInt().toShort())
                            }
                            sendCommand("eq:set_band_level", b)
                        },
                        valueRange = min..max,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(text = "Bass Boost")
                Slider(
                    value = bassStrength,
                    onValueChange = {
                        bassStrength = it
                        val b = Bundle().apply { putShort("strength", it.roundToInt().toShort()) }
                        sendCommand("bass:set_strength", b)
                    },
                    valueRange = 0f..1000f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(text = "Virtualizer")
                Slider(
                    value = virtStrength,
                    onValueChange = {
                        virtStrength = it
                        val b = Bundle().apply { putShort("strength", it.roundToInt().toShort()) }
                        sendCommand("virt:set_strength", b)
                    },
                    valueRange = 0f..1000f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        },
        dismissButton = {
            Button(onClick = {
                sendCommand("eq:reset", Bundle())
            }) { Text("Reset") }
        }
    )
}
