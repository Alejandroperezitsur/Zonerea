package com.apvlabs.zonerea.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.audioPrefsDataStore by preferencesDataStore(name = "audio_prefs")

class AudioPreferences(private val context: Context) {
    private fun eqKeyFor(id: String) = stringPreferencesKey("eq_" + id)
    private fun bassKeyFor(id: String) = stringPreferencesKey("bass_" + id)
    private fun virtKeyFor(id: String) = stringPreferencesKey("virt_" + id)

    suspend fun saveEqPreset(songId: String, levels: ShortArray) {
        val data = levels.joinToString(",") { it.toString() }
        context.audioPrefsDataStore.edit { prefs ->
            prefs[eqKeyFor(songId)] = data
        }
    }

    suspend fun loadEqPreset(songId: String): ShortArray? {
        val prefs = context.audioPrefsDataStore.data.first()
        val data = prefs[eqKeyFor(songId)] ?: return null
        return data.split(",").mapNotNull { it.toShortOrNull() }.toShortArray()
    }

    suspend fun saveBassStrength(songId: String, strength: Short) {
        context.audioPrefsDataStore.edit { prefs ->
            prefs[bassKeyFor(songId)] = strength.toString()
        }
    }

    suspend fun loadBassStrength(songId: String): Short? {
        val prefs = context.audioPrefsDataStore.data.first()
        return prefs[bassKeyFor(songId)]?.toShortOrNull()
    }

    suspend fun saveVirtStrength(songId: String, strength: Short) {
        context.audioPrefsDataStore.edit { prefs ->
            prefs[virtKeyFor(songId)] = strength.toString()
        }
    }

    suspend fun loadVirtStrength(songId: String): Short? {
        val prefs = context.audioPrefsDataStore.data.first()
        return prefs[virtKeyFor(songId)]?.toShortOrNull()
    }
}
