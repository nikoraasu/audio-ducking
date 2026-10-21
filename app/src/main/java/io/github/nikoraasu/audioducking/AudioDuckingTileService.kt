package io.github.nikoraasu.audioducking

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AudioDuckingTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        ServiceStateStore.setTileAdded(this, true)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        ServiceStateStore.setTileAdded(this, false)
    }

    override fun onStartListening() {
        super.onStartListening()
        ServiceStateStore.setTileAdded(this, true) // also covers tiles added before this was tracked
        // The saved flag can be stale (service killed with the process): trust the real state.
        val running = serviceIsRunning()
        if (running != ServiceStateStore.isRunning(this)) ServiceStateStore.setRunning(this, running)
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val isRunning = ServiceStateStore.isRunning(this)

        if (isRunning) {
            stopService(Intent(this, AudioDuckingService::class.java))
            ServiceStateStore.setRunning(this, false)
            updateTile()
            return
        }

        val intent = Intent(this, StartDuckingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    @Suppress("DEPRECATION") // still lists this app's own services
    private fun serviceIsRunning() = getSystemService(ActivityManager::class.java)
        .getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == AudioDuckingService::class.java.name }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = ServiceStateStore.isRunning(this)

        val strings = stringsFor(resolveLanguage(Settings(this).language))
        tile.label = "Audio Ducking"
        tile.subtitle = if (running) strings.on else strings.off
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}