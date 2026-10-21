package io.github.nikoraasu.audioducking

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings as AndroidSettings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Whether the app's screen is in front; the floating status only shows while it isn't. */
object AppVisibility {
    val visible = MutableStateFlow(false)
}

fun canShowOverlay(context: Context) = AndroidSettings.canDrawOverlays(context)

/** The floating status card (like a navigation app's bubble), drawn over other apps. Main thread only. */
class StatusOverlay(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this).apply { performRestore(null) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedState.savedStateRegistry

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: FrameLayout? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 200
    }

    fun show() {
        if (view != null || !canShowOverlay(context)) return
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        val content = ComposeView(context).apply {
            setContent {
                val status by Monitor.status.collectAsState()
                AudioDuckingTheme {
                    CompositionLocalProvider(LocalStrings provides stringsFor(resolveLanguage(Settings(context).language))) {
                        MiniStatus(status)
                    }
                }
            }
        }
        // Takes every touch before the Compose view does, so a drag moves the card and a tap opens the app.
        view = object : FrameLayout(context) {
            override fun onInterceptTouchEvent(event: MotionEvent) = true
        }.apply {
            setViewTreeLifecycleOwner(this@StatusOverlay)
            setViewTreeSavedStateRegistryOwner(this@StatusOverlay)
            addView(content)
            setOnTouchListener(::onTouch)
        }
        windowManager.addView(view, params)
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        view = null
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    // Raw screen coordinates: the window moves under the finger, so view-relative ones would jitter.
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    @Suppress("ClickableViewAccessibility") // tapping opens the app, which is also in the notification
    private fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (dragging || hypot(dx, dy) > ViewConfiguration.get(context).scaledTouchSlop) {
                    dragging = true
                    params.x = startX + dx.roundToInt()
                    params.y = startY + dy.roundToInt()
                    windowManager.updateViewLayout(view, params)
                }
            }
            MotionEvent.ACTION_UP -> if (!dragging) openApp()
        }
        return true
    }

    private fun openApp() {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }
}
