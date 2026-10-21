package io.github.nikoraasu.audioducking

/** Platform key-value storage: SharedPreferences on Android, java.util.prefs on desktop. */
interface Prefs {
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** Bumped on every settings write, so a running listener can pick up changes immediately. */
object SettingsVersion {
    @Volatile
    var value = 0
        private set

    fun bump() {
        value++
    }
}

private class TrackingPrefs(private val prefs: Prefs) : Prefs by prefs {
    override fun putLong(key: String, value: Long) = prefs.putLong(key, value).also { SettingsVersion.bump() }
    override fun putFloat(key: String, value: Float) = prefs.putFloat(key, value).also { SettingsVersion.bump() }
    override fun putString(key: String, value: String) = prefs.putString(key, value).also { SettingsVersion.bump() }
    override fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value).also { SettingsVersion.bump() }
}

class Settings(prefs: Prefs) {
    private val prefs: Prefs = TrackingPrefs(prefs)

    var speechHoldMs: Long
        get() = prefs.getLong("speech_hold_ms", DEFAULT_SPEECH_HOLD_MS)
        set(value) = prefs.putLong("speech_hold_ms", value)

    // Long enough for someone to answer: with registered voices, a reply within the pause continues the
    // conversation instead of letting the volume come back between speakers.
    var allowedPauseMs: Long
        get() = prefs.getLong("allowed_pause_ms", DEFAULT_ALLOWED_PAUSE_MS)
        set(value) = prefs.putLong("allowed_pause_ms", value)

    /** Minimum CED "Speech" score. New key: the old one held a Silero threshold on a different scale. */
    // shortcut: 0.25 from end-to-end tests with CED-base, synthetic speech and a real song: the song with
    // vocals never triggered at 0.2-0.4, voice over equally loud music was caught 52% of the time at 0.25.
    // Retune with real voices.
    var speechThreshold: Float
        get() = prefs.getFloat("speech_class_threshold", DEFAULT_SPEECH_THRESHOLD)
        set(value) = prefs.putFloat("speech_class_threshold", value)

    var duckPercent: Float
        get() = prefs.getFloat("duck_percent", DEFAULT_DUCK_PERCENT)
        set(value) = prefs.putFloat("duck_percent", value)

    var boostPercent: Float
        get() = prefs.getFloat("boost_percent", DEFAULT_BOOST_PERCENT)
        set(value) = prefs.putFloat("boost_percent", value)

    /** Podcast mode: how far back playback resumes after a conversation. */
    var rewindSeconds: Long
        get() = prefs.getLong("rewind_seconds", DEFAULT_REWIND_SECONDS)
        set(value) = prefs.putLong("rewind_seconds", value)

    /** Android: show a small floating status card while the app is in the background. */
    var floatingStatus: Boolean
        get() = prefs.getBoolean("floating_status", false)
        set(value) = prefs.putBoolean("floating_status", value)

    var mode: Mode
        get() = Mode.entries.firstOrNull { it.name == prefs.getString("mode", "") } ?: Mode.DUCK
        set(value) = prefs.putString("mode", value.name)

    var carMode: CarMode
        get() = CarMode.entries.firstOrNull { it.name == prefs.getString("car_mode", "") } ?: CarMode.OFF
        set(value) = prefs.putString("car_mode", value.name)

    /** Car mode's speed steps (1..[MAX_SPEED_STEPS]), sorted by speed. Defaults to 50/70/90 km/h. */
    var speedSteps: List<SpeedStep>
        get() {
            // Before per-step settings there was one "volume per step"; derive the defaults from it.
            val legacyPerStep = prefs.getFloat("speed_boost_percent", 0.1f)
            val count = prefs.getLong("car_step_count", DEFAULT_SPEED_STEPS_KMH.size.toLong()).toInt().coerceIn(1, MAX_SPEED_STEPS)
            return (0 until count).map { i ->
                SpeedStep(
                    kmh = prefs.getFloat("car_step_${i}_kmh", DEFAULT_SPEED_STEPS_KMH.getOrElse(i) { 90f + 20f * (it - 2) }),
                    boost = prefs.getFloat("car_step_${i}_boost", (legacyPerStep * (i + 1)).coerceAtMost(0.5f))
                )
            }.sortedBy { it.kmh }
        }
        set(value) {
            value.forEachIndexed { i, step ->
                prefs.putFloat("car_step_${i}_kmh", step.kmh)
                prefs.putFloat("car_step_${i}_boost", step.boost)
            }
            prefs.putLong("car_step_count", value.size.toLong())
        }

    /** Blend the volume between speed steps instead of jumping at each one. */
    var carSmoothSteps: Boolean
        get() = prefs.getBoolean("car_smooth_steps", true)
        set(value) = prefs.putBoolean("car_smooth_steps", value)

    /** How long car mode takes to move the volume to a new level (seconds; 0 = at once). */
    var carTransitionSeconds: Float
        get() = prefs.getFloat("car_transition_seconds", DEFAULT_CAR_TRANSITION_SECONDS)
        set(value) = prefs.putFloat("car_transition_seconds", value)

    /** Most volume accelerating can add (0 = off). */
    var accelerationBoostMax: Float
        get() = prefs.getFloat("acceleration_boost_max", DEFAULT_ACCELERATION_BOOST)
        set(value) = prefs.putFloat("acceleration_boost_max", value)

    /** Most volume ambient noise can add. Also caps the loop of the mic hearing the car speakers. */
    var noiseBoostMaxPercent: Float
        get() = prefs.getFloat("noise_boost_max_percent", 0.2f)
        set(value) = prefs.putFloat("noise_boost_max_percent", value)

    /** Android microphone source name (see MicSource in the app); voice recognition by default. */
    var micSource: String
        get() = prefs.getString("mic_source", "VOICE_RECOGNITION")
        set(value) = prefs.putString("mic_source", value)

    /** A [Language.code], or empty to follow the system language. */
    var language: String
        get() = prefs.getString("language", "")
        set(value) = prefs.putString("language", value)

    /** React only to registered voices (if any are registered). */
    var voiceFilter: Boolean
        get() = prefs.getBoolean("voice_filter", false)
        set(value) = prefs.putBoolean("voice_filter", value)

    /**
     * Minimum similarity (cosine, -1..1) for speech to count as a registered voice. ERes2Net's 0.35: its equal-error point on real voices
     * (Mini LibriSpeech) was 0.38 for a clean 1.5 s snippet and lower with music; leaning lower favours
     * recognising the registered person.
     */
    var voiceMatchThreshold: Float
        get() = prefs.getFloat("voice_match_threshold_eres2net", DEFAULT_VOICE_MATCH_THRESHOLD)
        set(value) = prefs.putFloat("voice_match_threshold_eres2net", value)

    /** Voices are saved but were recorded with an older speaker model, so they must be recorded again. */
    val hasOutdatedVoices: Boolean
        get() = prefs.getString("voice_names", "").isNotBlank() && prefs.getString("voice_model", "") != VOICE_MODEL_ID

    /** Registered voices. */
    var voices: List<Voice>
        get() = if (hasOutdatedVoices) emptyList() else loadVoices()
        set(value) = saveVoices(value)

    // One key per sample: java.util.prefs caps a value at 8 KB, about two embeddings as text.
    // Sample 0 keeps the original "voice_embedding_<i>" key, so voices saved before samples existed still load.
    // Music/echo copies of sample j: "voice_extra_<i>_<j>_<k>".
    private fun loadVoices(): List<Voice> {
        return prefs.getString("voice_names", "").lines().filter { it.isNotEmpty() }.mapIndexedNotNull { i, name ->
            val recordings = (0 until MAX_VOICE_SAMPLES).mapNotNull { j ->
                floats(sampleKey(i, j))?.let { main ->
                    VoiceSample(main, (0 until MAX_EXTRAS_PER_SAMPLE).mapNotNull { k -> floats(extraKey(i, j, k)) })
                }
            }
            if (recordings.isEmpty()) null else Voice(name, recordings)
        }
    }

    private fun saveVoices(value: List<Voice>) {
        value.forEachIndexed { i, voice ->
            for (j in 0 until MAX_VOICE_SAMPLES) {
                val recording = voice.recordings.getOrNull(j)
                prefs.putString(sampleKey(i, j), recording?.main?.joinToString(",") ?: "")
                for (k in 0 until MAX_EXTRAS_PER_SAMPLE) {
                    prefs.putString(extraKey(i, j, k), recording?.extras?.getOrNull(k)?.joinToString(",") ?: "")
                }
            }
        }
        prefs.putString("voice_names", value.joinToString("\n") { it.name })
        prefs.putString("voice_model", VOICE_MODEL_ID)
    }

    private fun sampleKey(voice: Int, sample: Int) =
        if (sample == 0) "voice_embedding_$voice" else "voice_embedding_${voice}_$sample"

    private fun extraKey(voice: Int, sample: Int, extra: Int) = "voice_extra_${voice}_${sample}_$extra"

    private fun floats(key: String) =
        prefs.getString(key, "").split(',').mapNotNull { it.toFloatOrNull() }.takeIf { it.isNotEmpty() }?.toFloatArray()

    /** Also compare voices with their music/echo copies (see [augmentedFingerprints]). Off until tried in the car. */
    var voiceExtraSamples: Boolean
        get() = prefs.getBoolean("voice_extra_samples", false)
        set(value) = prefs.putBoolean("voice_extra_samples", value)

    /**
     * Puts the speech and voice settings back to values that work for most people (the setup guide's
     * "use recommended values"). Leaves the mode, registered voices, car and microphone alone.
     */
    fun useRecommendedValues() {
        speechHoldMs = DEFAULT_SPEECH_HOLD_MS
        allowedPauseMs = DEFAULT_ALLOWED_PAUSE_MS
        speechThreshold = DEFAULT_SPEECH_THRESHOLD
        duckPercent = DEFAULT_DUCK_PERCENT
        boostPercent = DEFAULT_BOOST_PERCENT
        rewindSeconds = DEFAULT_REWIND_SECONDS
        voiceMatchThreshold = DEFAULT_VOICE_MATCH_THRESHOLD
    }

    /** Set once the setup guide has been shown, so it opens by itself only on the first start. */
    var setupGuideSeen: Boolean
        get() = prefs.getBoolean("setup_guide_seen", false)
        set(value) = prefs.putBoolean("setup_guide_seen", value)

    /** The level the current mode moves the volume towards while someone speaks. */
    val speechLevel: Float
        get() = when (mode) {
            Mode.DUCK -> duckPercent
            Mode.BOOST -> boostPercent
            Mode.PODCAST -> 1f // unused: playback pauses instead
        }
}

const val MAX_EXTRAS_PER_SAMPLE = 4

const val DEFAULT_CAR_TRANSITION_SECONDS = 3f
const val DEFAULT_ACCELERATION_BOOST = 0.1f

const val DEFAULT_SPEECH_HOLD_MS = 1500L
const val DEFAULT_ALLOWED_PAUSE_MS = 1200L
const val DEFAULT_SPEECH_THRESHOLD = 0.25f
const val DEFAULT_DUCK_PERCENT = 0.35f
const val DEFAULT_BOOST_PERCENT = 0.8f
const val DEFAULT_REWIND_SECONDS = 5L
