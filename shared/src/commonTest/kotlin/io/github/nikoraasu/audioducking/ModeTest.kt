package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.test.assertEquals

class ModeTest {
    @Test
    fun duckLowersButNeverRaises() {
        assertEquals(0.35f, speechVolume(Mode.DUCK, current = 0.7f, level = 0.35f))
        assertEquals(0.2f, speechVolume(Mode.DUCK, current = 0.2f, level = 0.35f))
    }

    @Test
    fun boostRaisesButNeverLowers() {
        assertEquals(0.8f, speechVolume(Mode.BOOST, current = 0.5f, level = 0.8f))
        assertEquals(0.9f, speechVolume(Mode.BOOST, current = 0.9f, level = 0.8f))
    }

    @Test
    fun podcastPausesOnceAndResumesOnlyWhatItPaused() {
        val calls = mutableListOf<String>()
        var playing = true
        val pauser = PodcastPauser(object : MediaControl {
            override fun pause() = playing.also { calls += "pause" }
            override fun resume(rewindMs: Long) { calls += "resume $rewindMs" }
        })
        val talking = SpeechEffect(Mode.PODCAST, 1f)

        pauser.update(talking, 5000)
        pauser.update(talking, 5000)
        pauser.update(null, 5000)
        assertEquals(listOf("pause", "resume 5000"), calls)

        playing = false // nothing was playing: don't start playback afterwards
        calls.clear()
        pauser.update(talking, 5000)
        pauser.update(null, 5000)
        assertEquals(listOf("pause"), calls)
    }

    @Test
    fun podcastLeavesTheVolumeAlone() {
        assertEquals(0.6f, speechVolume(Mode.PODCAST, current = 0.6f, level = 1f))
    }
}
