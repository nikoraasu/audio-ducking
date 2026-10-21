package io.github.nikoraasu.audioducking

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val canOfferTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private var tileAdded by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)
    private var mediaAccess by mutableStateOf(false)

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkNotificationPermissionAndStart()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkLocationPermissionAndStart()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        startAudioDuckingService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tileAdded = ServiceStateStore.isTileAdded(this)


        if (intent?.getBooleanExtra("request_notification_permission", false) == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AudioDuckingTheme {
                AudioDuckingScreen(
                    settings = Settings(this@MainActivity),
                    onStart = {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasMicPermission) {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            checkNotificationPermissionAndStart()
                        }
                    },
                    onStop = {
                        stopService(Intent(this@MainActivity, AudioDuckingService::class.java))
                        ServiceStateStore.setRunning(this@MainActivity, false)
                    },
                    enrollVoice = { onDone -> enrollAndroidVoice(this@MainActivity, onDone) },
                    onAddTile = if (canOfferTile && !tileAdded) ::requestQuickSettingsTile else null,
                    platformSettings = {
                        CarModeSettings(Settings(this@MainActivity))
                        DisplaySettings(Settings(this@MainActivity), overlayAllowed) {
                            startActivity(
                                Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                            )
                        }
                    },
                    podcastExtra = {
                        MediaAccessRow(mediaAccess) {
                            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                    isPhone = true,
                    platformAdvanced = {
                        MicrophoneSetting(Settings(this@MainActivity))
                    }
                )
            }
        }
    }

    // Permissions granted in system settings show up when the user comes back.
    override fun onResume() {
        super.onResume()
        overlayAllowed = canShowOverlay(this)
        mediaAccess = hasMediaAccess(this)
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.visible.value = true
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.visible.value = false
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        checkLocationPermissionAndStart()
    }

    // Car mode reads the speed from GPS; without the permission only the noise boost works.
    private fun checkLocationPermissionAndStart() {
        val needsLocation = Settings(this).carMode != CarMode.OFF &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED

        if (needsLocation) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }

        startAudioDuckingService()
    }

    private fun startAudioDuckingService() {
        val intent = Intent(this, AudioDuckingService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            statusBarManager?.requestAddTileService(
                ComponentName(this, AudioDuckingTileService::class.java),
                "Audio Ducking",
                Icon.createWithResource(this, R.drawable.ic_notification),
                Executors.newSingleThreadExecutor()
            ) { result ->
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                ) {
                    ServiceStateStore.setTileAdded(this, true)
                    tileAdded = true
                }
            }
        }
    }
}
