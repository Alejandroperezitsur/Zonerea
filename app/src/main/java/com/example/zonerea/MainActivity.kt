package com.example.zonerea

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.Crossfade
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
    private var isInitialized: Boolean = false

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
            // Show a dedicated UI explaining required permissions
            showPermissionsRequiredUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instala splash screen antes de super.onCreate para transición suave
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Habilita edge-to-edge para dibujar detrás de barras del sistema
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !isInitialized }
        requestPermissions()
    }

    private fun setupApplication() {
        // 1. Initialize Controller
        musicController = MusicControllerImpl(this)

        // 2. Initialize ViewModel
        val app = (application as MusicPlayerApp)
        val viewModelFactory = ViewModelFactory(
            app.songRepository,
            musicController!!,
            app.themePreferences
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // 3. Scan for songs
        viewModel.scanForSongs()

        // 4. Set UI content
        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            // Garantiza un estado inicial no nulo para evitar suposiciones peligrosas en arranque
            Crossfade(targetState = selectedTheme ?: com.example.zonerea.ui.theme.AppTheme.Auto) { st ->
                ZonereaTheme(selectedTheme = st) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel)
                    }
                }
            }
        }
        isInitialized = true
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

    private fun showPermissionsRequiredUI() {
        val is13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val storagePermission = if (is13Plus) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageGranted = ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = if (is13Plus) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // Detectar si el usuario marcó "No volver a preguntar" (denegación permanente)
        val storageRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, storagePermission)
        val notifRationale = if (is13Plus) {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
        } else false
        val preferOpenSettings = (!storageGranted && !storageRationale) || (is13Plus && !notificationsGranted && !notifRationale)

        setContent {
            // Use default theme fallbacks while permissions are not granted
            com.example.zonerea.ui.theme.ZonereaTheme() {
                com.example.zonerea.ui.composables.PermissionsRequiredScreen(
                    storageGranted = storageGranted,
                    notificationsGranted = notificationsGranted,
                    isAndroid13Plus = is13Plus,
                    preferOpenSettings = preferOpenSettings,
                    onRequestPermissions = { requestPermissions() },
                    onOpenSettings = { openAppSettings() }
                )
            }
        }
    }

    private fun openAppSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        musicController?.release()
    }
}
