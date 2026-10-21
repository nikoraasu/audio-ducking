package io.github.nikoraasu.audioducking

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

object ServiceStateStore {
    private const val PREFS = "sound_monitor_prefs"
    private const val KEY_RUNNING = "service_running"
    private const val KEY_TILE_ADDED = "tile_added"

    /** Saves whether the service runs and asks the Quick Settings tile to redraw, whoever started or stopped it. */
    fun setRunning(context: Context, running: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, running)
            .commit() // the tile reads this right away, so it must be written before the request below
        TileService.requestListeningState(context, ComponentName(context, AudioDuckingTileService::class.java))
    }

    fun setTileAdded(context: Context, added: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TILE_ADDED, added)
            .apply()
    }

    fun isTileAdded(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_TILE_ADDED, false)
    }

    fun isRunning(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RUNNING, false)
    }
}