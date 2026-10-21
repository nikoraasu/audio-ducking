package io.github.nikoraasu.audioducking

import kotlin.math.sqrt

/** What speech should do to the volume right now: move it towards [level] the way [mode] says. */
data class SpeechEffect(val mode: Mode, val level: Float)

/**
 * Turns microphone frames into the current [SpeechEffect].
 *
 * Speech detection: the CED sound classifier scores the last second for "Speech" about twice a second,
 * on [classify] (it is too slow for the audio thread; on a slow device it simply runs less often). It
 * tells talking apart from music with vocals, which a voice-activity detector cannot.
 *
 * With voice filtering on, each ~1.5 s of speech is fingerprinted on [identify] and the last
 * [WINDOW_CHUNKS] fingerprints of the conversation are averaged and matched against the registered
 * voices. The first match counts straight away and then sticks until the conversation ends: once the
 * volume is down, the person is easy to hear anyway. Until a registered voice
 * is found the volume stays as it is.
 *
 * Settings are re-read whenever they change ([SettingsVersion]), so edits apply while listening.
 */
class SpeechProcessor(
    private val settings: Settings,
    private val models: SpeechModels,
    private val classify: (() -> Unit) -> Unit,
    private val identify: (() -> Unit) -> Unit
) {
    private var seenVersion = -1
    @Volatile private var threshold = 0f
    private var modeEffect = SpeechEffect(Mode.DUCK, 0f)
    @Volatile private var voices = emptyList<Voice>()
    @Volatile private var withExtras = false
    @Volatile private var matchThreshold = 0f
    private val gate = SpeechGate(0, 0)

    private val lastSecond = FloatArray(FRAMES_PER_SECOND * FRAME_SAMPLES) // ring buffer, ~1 s of whole frames
    private var lastSecondPos = 0
    private var framesSinceClassifyStarted = CLASSIFY_EVERY_FRAMES
    @Volatile private var classifying = false
    @Volatile private var classifierSaysSpeech = false

    private val chunk = FloatArray(CHUNK_SAMPLES)
    private var chunkFill = 0
    @Volatile private var conversation = 0
    @Volatile private var checking = false
    @Volatile private var identified: Identified? = null

    var effect: SpeechEffect? = null
        private set

    var lastFrameWasSpeech = false
        private set

    /** The registered voice heard in the current conversation, if any. */
    val speaker: String?
        get() = current()?.voice?.name

    /** Similarity of the conversation so far to the closest registered voice, for tuning the threshold. */
    val matchScore: Float?
        get() = current()?.score

    private fun current() = identified?.takeIf { it.conversation == conversation }

    private fun reloadIfChanged() {
        if (seenVersion == SettingsVersion.value) return
        seenVersion = SettingsVersion.value
        threshold = settings.speechThreshold
        gate.holdMs = settings.speechHoldMs
        gate.pauseMs = settings.allowedPauseMs
        modeEffect = SpeechEffect(settings.mode, settings.speechLevel)
        matchThreshold = settings.voiceMatchThreshold
        val newVoices = if (settings.voiceFilter) settings.voices else emptyList()
        if (newVoices.map { it.name } != voices.map { it.name }) {
            conversation++ // different voices: start identifying from scratch
            chunkFill = 0
        }
        voices = newVoices
        withExtras = settings.voiceExtraSamples
    }

    /** Feeds one [FRAME_SAMPLES] frame; returns true when [effect] changed. */
    fun onFrame(frame: FloatArray, nowMs: Long): Boolean {
        reloadIfChanged()
        remember(frame)
        maybeClassify()
        val isSpeech = classifierSaysSpeech
        lastFrameWasSpeech = isSpeech
        val wasSpeaking = gate.speaking
        gate.update(isSpeech, nowMs)

        if (voices.isNotEmpty()) {
            if (wasSpeaking && !gate.speaking) {
                conversation++ // forget who was talking
                chunkFill = 0
            } else if (isSpeech) {
                collect(frame)
            }
        }

        val known = current()
        val newEffect = when {
            !gate.active -> null
            voices.isEmpty() -> modeEffect
            known?.voice != null -> modeEffect
            else -> null // unregistered voice, or still finding out who is talking
        }
        val changed = newEffect != effect
        effect = newEffect
        return changed
    }

    private fun remember(frame: FloatArray) {
        frame.copyInto(lastSecond, lastSecondPos)
        lastSecondPos = (lastSecondPos + frame.size) % lastSecond.size
    }

    /** Starts a classification of the last second every ~0.5 s, unless one is still running. */
    private fun maybeClassify() {
        if (++framesSinceClassifyStarted < CLASSIFY_EVERY_FRAMES || classifying) return
        framesSinceClassifyStarted = 0
        val window = FloatArray(lastSecond.size) { lastSecond[(lastSecondPos + it) % lastSecond.size] }
        if (rmsDb(window) < SILENCE_DB) { // nothing to classify; saves battery in a quiet room
            classifierSaysSpeech = false
            return
        }
        classifying = true
        classify {
            try {
                classifierSaysSpeech = models.speechClassScore(window) >= threshold
            } catch (_: Exception) {
                classifierSaysSpeech = false
            } finally {
                classifying = false
            }
        }
    }

    private fun collect(frame: FloatArray) {
        frame.copyInto(chunk, chunkFill)
        chunkFill += frame.size
        if (chunkFill < chunk.size) return
        chunkFill = 0
        if (checking) return // previous check still running; skip this chunk

        checking = true
        val samples = chunk.copyOf()
        val forConversation = conversation
        val candidates = voices
        val extras = withExtras
        identify {
            try {
                val embedding = normalized(models.embed(samples))
                val previous = identified?.takeIf { it.conversation == forConversation }
                val recent = (previous?.recent.orEmpty() + listOf(embedding)).takeLast(WINDOW_CHUNKS)
                val (voice, score) = closestVoice(sum(recent), candidates, extras)!!
                // Sticky: a match made earlier in this conversation stays.
                val matched = previous?.voice ?: voice.takeIf { score >= matchThreshold }
                if (forConversation == conversation) identified = Identified(forConversation, matched, score, recent)
            } catch (_: Exception) {
                // Leave the speaker unknown; the next chunk tries again.
            } finally {
                checking = false
            }
        }
    }

    private class Identified(
        val conversation: Int,
        val voice: Voice?,
        val score: Float,
        val recent: List<FloatArray>
    )

    private companion object {
        const val CHUNK_SAMPLES = 47 * FRAME_SAMPLES // ~1.5 s
        const val FRAMES_PER_SECOND = SAMPLE_RATE / FRAME_SAMPLES
        const val CLASSIFY_EVERY_FRAMES = 16 // ~0.5 s
        const val SILENCE_DB = -60f
        const val WINDOW_CHUNKS = 3 // ~4.5 s of speech

        fun normalized(v: FloatArray): FloatArray {
            val norm = sqrt(v.fold(0f) { acc, x -> acc + x * x }) + 1e-12f
            return FloatArray(v.size) { v[it] / norm }
        }

        fun sum(vectors: List<FloatArray>) = FloatArray(vectors[0].size) { i -> vectors.sumOf { it[i].toDouble() }.toFloat() }
    }
}
