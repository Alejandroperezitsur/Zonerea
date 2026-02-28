package com.apvlabs.zonerea.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

private fun channelToLinear(c: Float): Double {
    val v = c.toDouble()
    return if (v <= 0.03928) {
        v / 12.92
    } else {
        ((v + 0.055) / 1.055).pow(2.4)
    }
}

fun relativeLuminance(color: Color): Double {
    val r = channelToLinear(color.red)
    val g = channelToLinear(color.green)
    val b = channelToLinear(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

fun contrastRatio(c1: Color, c2: Color): Double {
    val l1 = relativeLuminance(c1)
    val l2 = relativeLuminance(c2)
    val light = maxOf(l1, l2)
    val dark = minOf(l1, l2)
    return (light + 0.05) / (dark + 0.05)
}

fun ensureReadableColor(
    foreground: Color,
    background: Color,
    minRatio: Double = 4.5
): Color {
    val ratio = contrastRatio(foreground, background)
    if (ratio >= minRatio) return foreground
    val blackRatio = contrastRatio(Color.Black, background)
    val whiteRatio = contrastRatio(Color.White, background)
    return if (blackRatio >= whiteRatio) Color.Black else Color.White
}
