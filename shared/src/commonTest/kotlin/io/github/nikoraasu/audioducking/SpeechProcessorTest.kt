package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapPrefs : Prefs {
    private val map = mutableMapOf<String, Any>()
    override fun getLong(key: String, default: Long) = map[key] as Long? ?: default
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getFloat(key: String, default: Float) = map[key] as Float? ?: default
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun getString(key: String, default: String) = map[key] as String? ?: default
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean) = map[key] as Boolean? ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

/**
 * Test frames carry their meaning: sample 0 = 1 for sound (0 = silence), sample 1 = who (1 = Alex, 0 = someone
 * else), sample 2 = [SINGING] when it is a singer rather than someone talking.
 */
private const val SINGING = -0.5f

private object FakeModels : SpeechModels {
    override fun speechProbability(frame: FloatArray) = frame[0]
    override fun speechClassScore(window: FloatArray) = when {
        window.any { it == SINGING } -> 0.05f
        window.any { it != 0f } -> 0.9f
        else -> 0f
    }
    override fun embed(samples: FloatArray): FloatArray {
        return floatArrayOf(samples[1], 1 - samples[1])
    }
    override fun augmentationMusic() = emptyList<FloatArray>()
    override fun release() {}
}

class SpeechProcessorTest {
    private val alex = Voice("Alex", floatArrayOf(1f, 0f))

    private fun settings(block: Settings.() -> Unit) = Settings(MapPrefs()).apply {
        speechHoldMs = 2000
        allowedPauseMs = 700
        speechThreshold = 0.5f
        mode = Mode.BOOST
        boostPercent = 0.9f
        duckPercent = 0.3f
        block()
    }

    /** Feeds [ms] of frames; background work runs immediately. */
    private fun SpeechProcessor.talk(who: Float, fromMs: Long, ms: Long) {
        var t = fromMs
        while (t < fromMs + ms) {
            onFrame(FloatArray(FRAME_SAMPLES).also { it[0] = 1f; it[1] = who }, t)
            t += 32
        }
    }

    private fun processor(settings: Settings) = SpeechProcessor(settings, FakeModels, { it() }, { it() })

    @Test
    fun singingIsNotTakenForSpeech() {
        val p = processor(settings { })
        var t = 0L
        while (t < 4000) {
            p.onFrame(FloatArray(FRAME_SAMPLES).also { it[0] = 1f; it[2] = SINGING }, t)
            t += 32
        }
        assertNull(p.effect)
        p.talk(who = 0f, fromMs = 4000, ms = 3500) // then someone really talks
        assertEquals(SpeechEffect(Mode.BOOST, 0.9f), p.effect)
    }

    @Test
    fun withoutFilterAnySpeechTriggersTheMode() {
        val p = processor(settings { voices = listOf(alex) })
        p.talk(who = 0f, fromMs = 0, ms = 2500)
        assertEquals(SpeechEffect(Mode.BOOST, 0.9f), p.effect)
    }

    @Test
    fun registeredVoiceTriggersTheMode() {
        val p = processor(settings { voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 1f, fromMs = 0, ms = 3500)
        assertEquals(SpeechEffect(Mode.BOOST, 0.9f), p.effect)
        assertEquals("Alex", p.speaker)
    }

    @Test
    fun unregisteredVoiceIsIgnoredByDefault() {
        val p = processor(settings { voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 0f, fromMs = 0, ms = 2500)
        assertNull(p.effect)
        assertNull(p.speaker)
    }

    @Test
    fun speakerIsForgottenWhenTheConversationEnds() {
        val p = processor(settings { voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 1f, fromMs = 0, ms = 3500)
        assertEquals("Alex", p.speaker)
        repeat(80) { p.onFrame(FloatArray(FRAME_SAMPLES), 3500L + it * 32) } // ~2.5 s silence (the classifier looks 1 s back)
        assertNull(p.effect)
        assertNull(p.speaker)
    }

    @Test
    fun theFirstMatchingSnippetCountsStraightAway() {
        val p = processor(settings { voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 1f, fromMs = 0, ms = 1600) // one ~1.5 s snippet
        assertEquals("Alex", p.speaker)
    }

    @Test
    fun aMatchSticksForTheRestOfTheConversation() {
        val p = processor(settings { voiceFilter = true; voiceMatchThreshold = 0.8f; voices = listOf(alex) })
        p.talk(who = 1f, fromMs = 0, ms = 1504) // Alex starts talking
        p.talk(who = 0f, fromMs = 1504, ms = 3000) // the sound changes (music, car) without a pause
        assertEquals("Alex", p.speaker)
        assertEquals(SpeechEffect(Mode.BOOST, 0.9f), p.effect)
    }

    /** Feeds [ms] of frames (sound for [who], or silence) and returns whether [SpeechProcessor.effect] was ever off. */
    private fun SpeechProcessor.effectDroppedDuring(who: Float?, fromMs: Long, ms: Long): Boolean {
        var dropped = false
        var t = fromMs
        while (t < fromMs + ms) {
            onFrame(FloatArray(FRAME_SAMPLES).also { if (who != null) { it[0] = 1f; it[1] = who } }, t)
            if (effect == null) dropped = true
            t += 32
        }
        return dropped
    }

    @Test
    fun aReplyWithinThePauseKeepsTheVolumeWhereItIs() {
        for (mode in listOf(Mode.DUCK, Mode.BOOST)) {
            val p = processor(settings { this.mode = mode; voiceFilter = true; voices = listOf(alex) })
            p.talk(who = 1f, fromMs = 0, ms = 3500) // Alex talks: volume changes
            assertEquals("Alex", p.speaker)
            // Alex stops, someone else answers before the allowed pause is over, then Alex again.
            assertFalse(p.effectDroppedDuring(who = null, fromMs = 3500, ms = 500))
            assertFalse(p.effectDroppedDuring(who = 0f, fromMs = 4000, ms = 4000))
            assertFalse(p.effectDroppedDuring(who = 1f, fromMs = 8000, ms = 2000))
        }
    }

    @Test
    fun aStrangerAfterTheConversationEndedIsStillIgnored() {
        val p = processor(settings { voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 1f, fromMs = 0, ms = 3500)
        assertTrue(p.effectDroppedDuring(who = null, fromMs = 3500, ms = 2500)) // conversation over
        p.talk(who = 0f, fromMs = 6000, ms = 4000)
        assertNull(p.effect)
    }

    @Test
    fun settingsChangedWhileListeningApplyImmediately() {
        val s = settings { voices = listOf(alex) } // filter off: reacts to anyone
        val p = processor(s)
        p.talk(who = 0f, fromMs = 0, ms = 2500)
        assertEquals(SpeechEffect(Mode.BOOST, 0.9f), p.effect)

        s.voiceFilter = true // switched on mid-conversation: a stranger must stop counting
        p.talk(who = 0f, fromMs = 2500, ms = 3500)
        assertNull(p.effect)

        s.mode = Mode.DUCK
        s.voiceFilter = false
        p.talk(who = 0f, fromMs = 6000, ms = 100)
        assertEquals(SpeechEffect(Mode.DUCK, 0.3f), p.effect)
    }

    @Test
    fun duckModeWaitsForTheVoiceCheckAndIgnoresAStranger() {
        val p = processor(settings { mode = Mode.DUCK; voiceFilter = true; voices = listOf(alex) })
        p.talk(who = 0f, fromMs = 0, ms = 2100) // one check done: no match yet, volume stays
        assertNull(p.effect)
        p.talk(who = 0f, fromMs = 2100, ms = 1500) // still nobody registered
        assertNull(p.effect)
    }

    @Test
    fun voicesSurviveAStoreRoundTrip() {
        val s = settings { voices = listOf(alex, Voice("Bea\nB", floatArrayOf(0.25f, -1f))) }
        val loaded = s.voices
        assertEquals(listOf("Alex", "Bea B"), loaded.map { it.name })
        assertEquals(listOf(0.25f, -1f), loaded[1].samples.single().toList())
    }

    @Test
    fun extraSamplesAreStoredAndMatchedByTheClosestOne() {
        val s = settings { voices = listOf(alex.withSample(VoiceSample(floatArrayOf(0.6f, 0.8f)))) }
        val loaded = s.voices.single()
        assertEquals(2, loaded.samples.size)
        // Heard "in the car": far from the home sample [1, 0], right on the car sample [0.6, 0.8].
        assertEquals(1f, closestVoice(floatArrayOf(0.6f, 0.8f), listOf(loaded))!!.second, 1e-4f)
    }

    @Test
    fun voicesFromAnOlderModelAreDroppedAndFlagged() {
        val prefs = MapPrefs().apply {
            putString("voice_names", "Alex")
            putString("voice_embedding_0", "1.0,0.0") // saved before the model was recorded
        }
        val s = Settings(prefs)
        assertTrue(s.hasOutdatedVoices)
        assertTrue(s.voices.isEmpty())
        s.voices = listOf(alex) // registering again
        assertFalse(s.hasOutdatedVoices)
        assertEquals("Alex", s.voices.single().name)
    }

    @Test
    fun voicesAndThresholdKeepTheirStorageKeys() {
        val prefs = MapPrefs()
        val s = Settings(prefs)
        s.voices = listOf(alex)
        s.voiceMatchThreshold = 0.4f
        assertEquals("Alex", prefs.getString("voice_names", ""))
        assertEquals(0.4f, prefs.getFloat("voice_match_threshold_eres2net", 0f))
        assertEquals("Alex", Settings(prefs).voices.single().name)
    }

    @Test
    fun recommendedValuesResetSpeechSettingsButKeepTheSetup() {
        val s = settings {
            mode = Mode.PODCAST
            voices = listOf(alex)
            voiceMatchThreshold = 0.8f
        }
        s.useRecommendedValues()
        assertEquals(DEFAULT_SPEECH_HOLD_MS, s.speechHoldMs)
        assertEquals(DEFAULT_ALLOWED_PAUSE_MS, s.allowedPauseMs)
        assertEquals(DEFAULT_SPEECH_THRESHOLD, s.speechThreshold)
        assertEquals(DEFAULT_VOICE_MATCH_THRESHOLD, s.voiceMatchThreshold)
        assertEquals(Mode.PODCAST, s.mode)
        assertEquals("Alex", s.voices.single().name)
    }

    @Test
    fun extraFingerprintsAreStoredAndOnlyUsedWhenSwitchedOn() {
        // Alex's music copy sounds like "someone else" in this test's terms.
        val withCopy = Voice("Alex", listOf(VoiceSample(floatArrayOf(1f, 0f), listOf(floatArrayOf(0f, 1f)))))
        val s = settings { voices = listOf(withCopy) }
        val loaded = s.voices.single()
        assertEquals(listOf(0f, 1f), loaded.recordings.single().extras.single().toList())
        assertEquals(0f, closestVoice(floatArrayOf(0f, 1f), listOf(loaded))!!.second, 1e-4f)
        assertEquals(1f, closestVoice(floatArrayOf(0f, 1f), listOf(loaded), withExtras = true)!!.second, 1e-4f)

        s.voiceFilter = true
        val p = processor(s)
        p.talk(who = 0f, fromMs = 0, ms = 3500)
        assertNull(p.speaker) // switch off: the copy doesn't count
        s.voiceExtraSamples = true
        p.talk(who = 0f, fromMs = 3500, ms = 3500)
        assertEquals("Alex", p.speaker)
    }

    @Test
    fun removingAVoiceClearsItsExtraFingerprints() {
        val bea = Voice("Bea", listOf(VoiceSample(floatArrayOf(0f, 1f), listOf(floatArrayOf(0.5f, 0.5f)))))
        val s = settings { voices = listOf(Voice("Alex", listOf(VoiceSample(floatArrayOf(1f, 0f)))), bea) }
        s.voices = listOf(s.voices[1]) // Alex removed: Bea moves to slot 0
        assertEquals("Bea", s.voices.single().name)
        assertEquals(1, s.voices.single().recordings.single().extras.size)
        s.voices = listOf(Voice("Cid", floatArrayOf(1f, 1f)))
        assertTrue(s.voices.single().recordings.single().extras.isEmpty()) // Bea's copy didn't leak to Cid
    }

    @Test
    fun atMostThreeSamplesAreKept() {
        var voice = alex
        repeat(4) { voice = voice.withSample(VoiceSample(floatArrayOf(it.toFloat(), 1f))) }
        assertEquals(MAX_VOICE_SAMPLES, voice.samples.size)
        assertEquals(listOf(3f, 1f), voice.samples.last().toList()) // newest kept
    }

    @Test
    fun closestVoicePicksTheMostSimilar() {
        val bea = Voice("Bea", floatArrayOf(0f, 1f))
        assertEquals("Bea", closestVoice(floatArrayOf(0.2f, 0.9f), listOf(alex, bea))?.first?.name)
        assertNull(closestVoice(floatArrayOf(1f, 0f), emptyList()))
    }

    @Test
    fun weakMatchCountsAsUnregisteredButKeepsItsScore() {
        val p = processor(settings { voiceFilter = true; voiceMatchThreshold = 0.8f; voices = listOf(alex) })
        p.talk(who = 0.5f, fromMs = 0, ms = 2500) // cosine([0.5, 0.5], [1, 0]) ~ 0.71
        assertNull(p.speaker)
        assertEquals(0.71f, p.matchScore!!, 0.01f)
    }
}
