package com.example.zonerea.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

object MotionDefaults {
    const val durationShort = 180
    const val durationMedium = 280
    const val durationLong = 420
    val easing: Easing = FastOutSlowInEasing
}

fun fadeInSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = MotionDefaults.durationMedium, easing = MotionDefaults.easing)
fun fadeOutSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = MotionDefaults.durationShort, easing = MotionDefaults.easing)
fun slideSpec(): FiniteAnimationSpec<IntOffset> = tween(durationMillis = MotionDefaults.durationLong, easing = MotionDefaults.easing)
fun scaleSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = MotionDefaults.durationMedium, easing = MotionDefaults.easing)