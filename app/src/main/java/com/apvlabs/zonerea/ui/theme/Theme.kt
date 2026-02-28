package com.apvlabs.zonerea.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.apvlabs.zonerea.ui.theme.AppTheme
import com.apvlabs.zonerea.ui.theme.colorSchemeFor
import java.util.Calendar

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ZonereaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    selectedTheme: AppTheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (selectedTheme == AppTheme.Auto) {
        // Modo automático por hora del día (noche: 19-6)
        val isNight = remember {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 19 || hour < 7
        }
        val context = LocalContext.current
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isNight) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            isNight -> DefaultDarkColorScheme
            else -> DefaultLightColorScheme
        }
    } else if (selectedTheme != null) {
        colorSchemeFor(selectedTheme)
    } else {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DefaultDarkColorScheme
            else -> DefaultLightColorScheme
        }
    }
    // Edge-to-edge se gestiona desde MainActivity con enableEdgeToEdge.
    // Evitamos manipular directamente statusBarColor para no usar APIs deprecadas.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
