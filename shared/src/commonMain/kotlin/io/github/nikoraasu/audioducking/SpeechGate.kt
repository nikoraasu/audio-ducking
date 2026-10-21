package io.github.nikoraasu.audioducking

/**
 * Turns per-frame speech decisions into "conversation started / stopped".
 * Becomes active once speech lasts [holdMs] (pauses up to [pauseMs] allowed),
 * and inactive again after more than [pauseMs] of silence.
 */
class SpeechGate(var holdMs: Long, var pauseMs: Long) {
    private var segmentStartMs: Long? = null
    private var lastSpeechMs: Long? = null

    var active = false
        private set

    /** Someone is talking or only briefly paused (the current conversation hasn't ended). */
    val speaking: Boolean
        get() = segmentStartMs != null

    /** Feeds one frame; returns true when [active] changed. */
    fun update(isSpeech: Boolean, nowMs: Long): Boolean {
        if (isSpeech) {
            if (segmentStartMs == null) segmentStartMs = nowMs
            lastSpeechMs = nowMs
        } else if (silenceMs(nowMs) > pauseMs) {
            segmentStartMs = null
            lastSpeechMs = null
        }

        val wasActive = active
        val start = segmentStartMs
        if (start != null && nowMs - start >= holdMs) active = true
        if (active && silenceMs(nowMs) > pauseMs) active = false
        return active != wasActive
    }

    private fun silenceMs(nowMs: Long) = lastSpeechMs?.let { nowMs - it } ?: Long.MAX_VALUE
}
