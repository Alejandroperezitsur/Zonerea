package com.example.zonerea

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.zonerea.playback.MusicController
import com.example.zonerea.playback.MusicControllerImpl
import com.example.zonerea.ui.screens.MainScreen
import com.example.zonerea.ui.theme.ZonereaTheme
import com.example.zonerea.ui.viewmodel.MainViewModel
import com.example.zonerea.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private var musicController: MusicController? = null
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val storagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        // Notification permission is critical on Android 13+
        val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true // Not required for older versions
        }

        if (storagePermissionGranted && notificationPermissionGranted) {
            setupApplication()
        } else {
            // Handle permission denial by closing the app
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    private fun setupApplication() {
        // 1. Initialize Controller
        musicController = MusicControllerImpl(this)

        // 2. Initialize ViewModel
        val viewModelFactory = ViewModelFactory(
            (application as MusicPlayerApp).songRepository,
            musicController!!
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // 3. Scan for songs
        viewModel.scanForSongs()

        // 4. Set UI content
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
            // Permissions are already granted
            setupApplication()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicController?.release()
    }
}
