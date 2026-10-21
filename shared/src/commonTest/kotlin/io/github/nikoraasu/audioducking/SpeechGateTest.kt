package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechGateTest {
    private val gate = SpeechGate(holdMs = 1000, pauseMs = 300)

    private fun feed(isSpeech: Boolean, fromMs: Long, toMs: Long) {
        for (t in fromMs..toMs step 32) gate.update(isSpeech, t)
    }

    @Test
    fun shortSpeechDoesNotTrigger() {
        feed(true, 0, 900)
        feed(false, 932, 2000)
        assertFalse(gate.active)
    }

    @Test
    fun longSpeechTriggersAndShortPauseKeepsItActive() {
        feed(true, 0, 1100)
        assertTrue(gate.active)
        feed(false, 1132, 1300) // pause under 300 ms
        assertTrue(gate.active)
        feed(true, 1332, 1500)
        assertTrue(gate.active)
    }

    @Test
    fun longSilenceReleases() {
        feed(true, 0, 1100)
        feed(false, 1132, 1500)
        assertFalse(gate.active)
    }

    @Test
    fun pauseLongerThanAllowedRestartsTheHold() {
        feed(true, 0, 600)
        feed(false, 632, 1000) // > 300 ms: segment discarded
        feed(true, 1032, 1700) // only ~670 ms of new speech
        assertFalse(gate.active)
    }
}
