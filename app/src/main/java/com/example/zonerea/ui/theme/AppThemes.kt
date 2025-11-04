package com.example.zonerea.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme(val isDark: Boolean) {
    // Automático (día/noche)
    Auto(false),
    // Claros
    LightAzul(false),
    LightVerde(false),
    LightRojo(false),
    LightNaranja(false),
    LightMorado(false),
    LightTurquesa(false),
    // Oscuros
    DarkAzul(true),
    DarkVerde(true),
    DarkRojo(true),
    DarkNaranja(true),
    DarkMorado(true),
    DarkTurquesa(true)
}

fun appThemeDisplayName(theme: AppTheme): String = when (theme) {
    AppTheme.Auto -> "Automático (día/noche)"
    AppTheme.LightAzul -> "Azul (claro)"
    AppTheme.LightVerde -> "Verde (claro)"
    AppTheme.LightRojo -> "Rojo (claro)"
    AppTheme.LightNaranja -> "Naranja (claro)"
    AppTheme.LightMorado -> "Morado (claro)"
    AppTheme.LightTurquesa -> "Turquesa (claro)"
    AppTheme.DarkAzul -> "Azul (oscuro)"
    AppTheme.DarkVerde -> "Verde (oscuro)"
    AppTheme.DarkRojo -> "Rojo (oscuro)"
    AppTheme.DarkNaranja -> "Naranja (oscuro)"
    AppTheme.DarkMorado -> "Morado (oscuro)"
    AppTheme.DarkTurquesa -> "Turquesa (oscuro)"
}

fun colorSchemeFor(theme: AppTheme): ColorScheme = when (theme) {
    // Auto se gestiona en ZonereaTheme y no debería usarse aquí.
    // Proveemos un esquema por defecto claro para evitar fallos si se invoca accidentalmente.
    AppTheme.Auto -> lightColorScheme(
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF03A9F4),
        tertiary = Color(0xFF00BCD4)
    )
    // Light schemes
    AppTheme.LightAzul -> lightColorScheme(
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF03A9F4),
        tertiary = Color(0xFF00BCD4)
    )
    AppTheme.LightVerde -> lightColorScheme(
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF4CAF50),
        tertiary = Color(0xFF8BC34A)
    )
    AppTheme.LightRojo -> lightColorScheme(
        primary = Color(0xFFC62828),
        secondary = Color(0xFFE53935),
        tertiary = Color(0xFFFF7043)
    )
    AppTheme.LightNaranja -> lightColorScheme(
        primary = Color(0xFFEF6C00),
        secondary = Color(0xFFFF9800),
        tertiary = Color(0xFFFFB74D)
    )
    AppTheme.LightMorado -> lightColorScheme(
        primary = Color(0xFF6A1B9A),
        secondary = Color(0xFF9C27B0),
        tertiary = Color(0xFFBA68C8)
    )
    AppTheme.LightTurquesa -> lightColorScheme(
        primary = Color(0xFF00695C),
        secondary = Color(0xFF009688),
        tertiary = Color(0xFF4DB6AC)
    )
    // Dark schemes
    AppTheme.DarkAzul -> darkColorScheme(
        primary = Color(0xFF64B5F6),
        secondary = Color(0xFF81D4FA),
        tertiary = Color(0xFF4DD0E1)
    )
    AppTheme.DarkVerde -> darkColorScheme(
        primary = Color(0xFF81C784),
        secondary = Color(0xFFA5D6A7),
        tertiary = Color(0xFFB2DFDB)
    )
    AppTheme.DarkRojo -> darkColorScheme(
        primary = Color(0xFFE57373),
        secondary = Color(0xFFEF9A9A),
        tertiary = Color(0xFFFFAB91)
    )
    AppTheme.DarkNaranja -> darkColorScheme(
        primary = Color(0xFFFFB74D),
        secondary = Color(0xFFFFCC80),
        tertiary = Color(0xFFFFE0B2)
    )
    AppTheme.DarkMorado -> darkColorScheme(
        primary = Color(0xFFBA68C8),
        secondary = Color(0xFFCE93D8),
        tertiary = Color(0xFFD1C4E9)
    )
    AppTheme.DarkTurquesa -> darkColorScheme(
        primary = Color(0xFF4DB6AC),
        secondary = Color(0xFF80CBC4),
        tertiary = Color(0xFFB2DFDB)
    )
}