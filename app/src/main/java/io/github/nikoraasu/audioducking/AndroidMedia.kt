package io.github.nikoraasu.audioducking

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

/**
 * Podcast mode on Android. With notification access (needed to see other apps' media sessions), it
 * pauses the playing app and later seeks back precisely. Without it, it falls back to media keys:
 * pause and play only, since a "rewind" key starts continuous rewinding in some players.
 */
class AndroidMediaControl(private val context: Context) : MediaControl {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var controller: MediaController? = null

    override fun pause(): Boolean {
        controller = playingController()
        controller?.let {
            it.transportControls.pause()
            return true
        }
        if (!audioManager.isMusicActive) return false
        pressKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        return true
    }

    override fun resume(rewindMs: Long) {
        val c = controller
        controller = null
        if (c == null) {
            pressKey(KeyEvent.KEYCODE_MEDIA_PLAY)
            return
        }
        val state = c.playbackState
        if (rewindMs > 0 && state != null && state.actions and PlaybackState.ACTION_SEEK_TO != 0L) {
            c.transportControls.seekTo((position(state) - rewindMs).coerceAtLeast(0))
        }
        c.transportControls.play()
    }

    private fun playingController(): MediaController? {
        if (!hasMediaAccess(context)) return null
        return try {
            context.getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(ComponentName(context, MediaAccessService::class.java))
                .firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        } catch (_: SecurityException) {
            null
        }
    }

    // The reported position is from the last state change; add the time played since.
    private fun position(state: PlaybackState): Long {
        if (state.state != PlaybackState.STATE_PLAYING) return state.position
        return state.position + ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
    }

    private fun pressKey(code: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

fun hasMediaAccess(context: Context) =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

/** Never reads notifications: being an enabled listener is what lets the app control other apps' playback. */
class MediaAccessService : NotificationListenerService()
