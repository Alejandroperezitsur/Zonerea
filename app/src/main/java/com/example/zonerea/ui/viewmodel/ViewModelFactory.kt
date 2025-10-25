package com.example.zonerea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zonerea.data.SongRepository
import com.example.zonerea.playback.MusicController

class ViewModelFactory(
    private val repository: SongRepository,
    private val musicController: MusicController
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, musicController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
