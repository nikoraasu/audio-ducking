package io.github.nikoraasu.audioducking

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.util.prefs.Preferences
import org.jetbrains.skia.Image

private class JavaPrefs : Prefs {
    private val node = Preferences.userRoot().node("io/github/nikoraasu/audioducking").also { migrateOldNode(it) }

    // Settings and voices lived under the app's earlier package name; copy them over once.
    private fun migrateOldNode(new: Preferences) {
        val oldPath = listOf("com", "example", "soundmonitor").joinToString("/")
        if (new.keys().isNotEmpty() || !Preferences.userRoot().nodeExists(oldPath)) return
        val old = Preferences.userRoot().node(oldPath)
        old.keys().forEach { key -> new.put(key, old.get(key, "")) }
        new.flush()
    }

    override fun getLong(key: String, default: Long) = node.getLong(key, default)
    override fun putLong(key: String, value: Long) = node.putLong(key, value)
    override fun getFloat(key: String, default: Float) = node.getFloat(key, default)
    override fun putFloat(key: String, value: Float) = node.putFloat(key, value)
    override fun getString(key: String, default: String): String = node.get(key, default)
    override fun putString(key: String, value: String) = node.put(key, value)
    override fun getBoolean(key: String, default: Boolean) = node.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = node.putBoolean(key, value)
}

fun main() {
    val settings = Settings(JavaPrefs())
    val monitor = DesktopMonitor(settings, SystemVolume.forCurrentOs())
    val icon = BitmapPainter(
        Image.makeFromEncoded(Settings::class.java.getResourceAsStream("/duck.png")!!.readBytes()).toComposeImageBitmap()
    )
    application {
        Window(
            onCloseRequest = {
                monitor.stop() // puts the volume back if speech had changed it
                exitApplication()
            },
            title = "Audio Ducking",
            icon = icon,
            state = rememberWindowState(size = DpSize(480.dp, 860.dp))
        ) {
            AudioDuckingTheme {
                AudioDuckingScreen(
                    settings = settings,
                    onStart = monitor::start,
                    onStop = monitor::stop,
                    enrollVoice = ::enrollDesktopVoice
                )
            }
        }
    }
}
