package com.apvlabs.zonerea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apvlabs.zonerea.data.SongRepository
import com.apvlabs.zonerea.playback.MusicController
import com.apvlabs.zonerea.ui.theme.ThemePreferences

class ViewModelFactory(
    private val repository: SongRepository,
    private val musicController: MusicController,
    private val themePreferences: ThemePreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, musicController, themePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
