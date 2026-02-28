package com.apvlabs.zonerea.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {
    private val SELECTED_THEME = stringPreferencesKey("selected_theme")

    // Null indica "usar colores del sistema/dinámicos".
    val themeFlow: Flow<AppTheme?> = context.themeDataStore.data.map { prefs ->
        val name = prefs[SELECTED_THEME] ?: return@map null
        runCatching { AppTheme.valueOf(name) }.getOrNull()
    }

    suspend fun setTheme(theme: AppTheme?) {
        context.themeDataStore.edit { prefs ->
            if (theme == null) {
                prefs.remove(SELECTED_THEME)
            } else {
                prefs[SELECTED_THEME] = theme.name
            }
        }
    }
}
