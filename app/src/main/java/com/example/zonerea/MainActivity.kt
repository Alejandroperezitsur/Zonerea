package com.example.zonerea

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.zonerea.playback.MusicController
import com.example.zonerea.ui.screens.MainScreen
import com.example.zonerea.ui.theme.ZonereaTheme
import com.example.zonerea.ui.viewmodel.MainViewModel
import com.example.zonerea.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var musicController: MusicController

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory(
            (application as MusicPlayerApp).songRepository,
            musicController
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_MEDIA_AUDIO] == true || permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            viewModel.scanForSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicController = MusicController(this)
        setContent {
            ZonereaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.scanForSongs()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicController.release()
    }
}
