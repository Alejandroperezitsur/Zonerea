package com.example.zonerea.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.unit.dp

object ZonereaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object ZonereaMotion {
    const val durationMicro = 90
    const val durationShort = 150
    const val durationMedium = 250
    const val durationLong = 400

    const val durationFast = durationShort
    const val durationSlow = durationLong

    val easingStandard: Easing = FastOutSlowInEasing
    val easingEmphasized: Easing = LinearOutSlowInEasing

    const val Fast: Int = durationFast
    const val Normal: Int = durationMedium
    const val Slow: Int = durationSlow
}

object ZonereaAlpha {
    const val scrimLight = 0.10f
    const val scrimDark = 0.18f
    const val playerBackgroundTop = 0.88f
    const val visualizerOverlay = 0.35f
    const val auraInner = 0.18f
}

object ZonereaElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 2.dp
    val level3 = 4.dp
    val level4 = 8.dp
}

