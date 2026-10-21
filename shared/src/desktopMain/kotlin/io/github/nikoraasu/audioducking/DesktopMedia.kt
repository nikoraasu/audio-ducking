package io.github.nikoraasu.audioducking

import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

fun desktopMediaControl(): MediaControl =
    if (System.getProperty("os.name").startsWith("Windows")) WindowsMedia() else LinuxMedia()

/** MPRIS over D-Bus: every Linux player that shows in the desktop's media controls (Spotify, browsers, VLC...). */
private class LinuxMedia : MediaControl {
    private var paused = emptyList<String>()

    override fun pause(): Boolean {
        paused = players().filter { status(it) == "Playing" }
        paused.forEach { player(it, "Pause") }
        return paused.isNotEmpty()
    }

    override fun resume(rewindMs: Long) {
        for (p in paused) {
            if (rewindMs > 0) player(p, "Seek", "int64:${-rewindMs * 1000}") // microseconds, relative
            player(p, "Play")
        }
        paused = emptyList()
    }

    private fun players() = Regex("""string "(org\.mpris\.MediaPlayer2\.[^"]+)"""")
        .findAll(dbus("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus.ListNames"))
        .map { it.groupValues[1] }.toList()

    private fun status(player: String) = Regex("""string "(\w+)"""").find(
        dbus(player, PATH, "org.freedesktop.DBus.Properties.Get", "string:org.mpris.MediaPlayer2.Player", "string:PlaybackStatus")
    )?.groupValues?.get(1)

    private fun player(player: String, method: String, vararg args: String) =
        dbus(player, PATH, "org.mpris.MediaPlayer2.Player.$method", *args)

    private fun dbus(dest: String, path: String, method: String, vararg args: String): String {
        val process = ProcessBuilder("dbus-send", "--session", "--print-reply", "--dest=$dest", path, method, *args)
            .redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return out
    }

    private companion object {
        const val PATH = "/org/mpris/MediaPlayer2"
    }
}

/**
 * Windows: the play/pause media key, which the focused media session (Spotify, browsers...) handles.
 * It toggles, so it is only pressed when sound is actually playing. No rewind: that needs WinRT.
 */
private class WindowsMedia : MediaControl {
    override fun pause(): Boolean {
        if (outputPeak() < 0.001f) return false
        pressPlayPause()
        return true
    }

    override fun resume(rewindMs: Long) = pressPlayPause()

    private fun pressPlayPause() {
        for (flags in intArrayOf(0, KEYEVENTF_KEYUP)) {
            val input = WinUser.INPUT().apply {
                type = WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD.toLong())
                input.setType("ki")
                input.ki.wVk = WinDef.WORD(VK_MEDIA_PLAY_PAUSE.toLong())
                input.ki.dwFlags = WinDef.DWORD(flags.toLong())
                input.ki.dwExtraInfo = BaseTSD.ULONG_PTR(0)
            }
            User32.INSTANCE.SendInput(WinDef.DWORD(1), arrayOf(input), input.size())
        }
    }

    private companion object {
        const val VK_MEDIA_PLAY_PAUSE = 0xB3
        const val KEYEVENTF_KEYUP = 2
    }
}
