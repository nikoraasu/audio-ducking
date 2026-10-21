package io.github.nikoraasu.audioducking

enum class Mode {
    /** Lower the volume while someone talks, e.g. to hear a person over music. */
    DUCK,

    /** Raise the volume while someone talks, e.g. to keep hearing a podcast over chatter. */
    BOOST,

    /** Pause playback while someone talks, then rewind a little and play again (see [PodcastPauser]). */
    PODCAST
}

/** Volume (0..1) to use while speech is active. Never moves the volume the "wrong" way for the mode. */
fun speechVolume(mode: Mode, current: Float, level: Float): Float = when (mode) {
    Mode.DUCK -> minOf(current, level)
    Mode.BOOST -> maxOf(current, level)
    Mode.PODCAST -> current // playback is paused instead
}

/** Pauses and resumes whatever media is playing on the device. */
interface MediaControl {
    /** Pauses playback; false when nothing was playing (then there is nothing to resume). */
    fun pause(): Boolean

    /** Goes back [rewindMs] where the player allows it, then plays. */
    fun resume(rewindMs: Long)
}

/** Podcast mode: pauses when speech starts, and resumes only what it paused itself. */
class PodcastPauser(private val media: MediaControl) {
    private var paused = false

    @Synchronized
    fun update(effect: SpeechEffect?, rewindMs: Long) {
        val pause = effect?.mode == Mode.PODCAST
        if (pause && !paused) {
            paused = runCatching { media.pause() }.getOrDefault(false)
        } else if (!pause && paused) {
            paused = false
            runCatching { media.resume(rewindMs) }
        }
    }
}
