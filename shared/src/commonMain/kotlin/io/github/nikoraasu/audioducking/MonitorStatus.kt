package io.github.nikoraasu.audioducking

import kotlinx.coroutines.flow.MutableStateFlow

/** What the listener is doing right now, for the screen. */
data class MonitorStatus(
    val running: Boolean = false,
    /** What speech is currently doing to the volume, null when nobody is talking. */
    val effect: SpeechEffect? = null,
    /** Registered voice currently talking, if any. */
    val speaker: String? = null,
    /** Similarity of the current talker to the closest registered voice (voice filter on). */
    val matchScore: Float? = null,
    /** Volume car mode adds right now; null when car mode isn't active. */
    val carBoost: Float? = null,
    /** Why listening stopped unexpectedly. */
    val error: MonitorError? = null
)

/** One listener per process (the Android service or the desktop monitor) publishes here; the UI watches it. */
object Monitor {
    val status = MutableStateFlow(MonitorStatus())
}
