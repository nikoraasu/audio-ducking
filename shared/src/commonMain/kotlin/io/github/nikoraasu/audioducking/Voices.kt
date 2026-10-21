package io.github.nikoraasu.audioducking

import kotlin.math.sqrt

const val SAMPLE_RATE = 16000
const val FRAME_SAMPLES = 512 // Silero's window at 16 kHz (32 ms)

const val MAX_VOICE_SAMPLES = 3

/** The speaker model: 3D-Speaker ERes2Net (26 MB). Chosen on real voices; see [Settings.voiceMatchThreshold]. */
const val VOICE_MODEL_ID = "eres2net"
const val VOICE_MODEL_FILE = "eres2net_sv_en_voxceleb_16k.onnx"
const val DEFAULT_VOICE_MATCH_THRESHOLD = 0.35f

/**
 * One registration recording: its fingerprint, plus [extras] made from copies with music and room echo
 * (see [augmentedFingerprints]; empty for recordings made before those existed).
 */
class VoiceSample(val main: FloatArray, val extras: List<FloatArray> = emptyList())

/**
 * A registered person. Each sample is the speaker model's fingerprint of one recording, ideally made
 * in a different situation (at home, in the car, with music), so a voice is compared with the
 * recording that best matches where it is heard now.
 */
class Voice(name: String, val recordings: List<VoiceSample>) {
    constructor(name: String, embedding: FloatArray) : this(name, listOf(VoiceSample(embedding)))

    val name = name.replace('\n', ' ').trim()

    val samples: List<FloatArray> get() = recordings.map { it.main }

    fun withSample(sample: VoiceSample) = Voice(name, (recordings + sample).takeLast(MAX_VOICE_SAMPLES))

    /** The fingerprints to compare with: the recordings, and their music/echo copies if [withExtras]. */
    fun fingerprints(withExtras: Boolean) = if (withExtras) recordings.flatMap { listOf(it.main) + it.extras } else samples
}

/** The on-device models, wrapped per platform (sherpa-onnx has separate Android and JVM APIs). */
interface SpeechModels {
    /** Silero speech probability (0..1) for one [FRAME_SAMPLES] frame at [SAMPLE_RATE]. */
    fun speechProbability(frame: FloatArray): Float

    /**
     * Probability (0..1) that ~1 s of audio contains people talking, from the CED sound classifier
     * (AudioSet "Speech" class). Unlike [speechProbability] it scores singing and music low.
     */
    fun speechClassScore(window: FloatArray): Float

    /** Speaker embedding of mono speech at [SAMPLE_RATE]. */
    fun embed(samples: FloatArray): FloatArray

    /** The bundled music clips (mono, [SAMPLE_RATE]) mixed into registration copies; see [augmentedFingerprints]. */
    fun augmentationMusic(): List<FloatArray>

    fun release()
}

fun cosine(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    var na = 0f
    var nb = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]
        na += a[i] * a[i]
        nb += b[i] * b[i]
    }
    return dot / sqrt(na * nb + 1e-12f)
}

/**
 * The registered voice most similar to [embedding] (via its closest fingerprint), with that similarity.
 * [withExtras] also compares with the music/echo copies.
 */
fun closestVoice(embedding: FloatArray, voices: List<Voice>, withExtras: Boolean = false): Pair<Voice, Float>? =
    voices.map { voice -> voice to voice.fingerprints(withExtras).maxOf { cosine(embedding, it) } }.maxByOrNull { it.second }

/** Why recording a voice failed; the screen words it (see [enrollError]). */
class EnrollException(val reason: Reason, val heardSeconds: Int = 0) : Exception(reason.name) {
    enum class Reason { MIC_STOPPED, TOO_LITTLE_SPEECH, TOO_QUIET, INCONSISTENT, NO_PERMISSION, MIC_UNAVAILABLE }
}

// ~60 s fits the two-paragraph reading text at a normal pace; a longer sample gives a steadier fingerprint.
const val ENROLL_SECONDS = 60
private const val MIN_ENROLL_SPEECH_SECONDS = 30

// shortcut: the level is an estimate for a phone/laptop mic at arm's length; tune if good samples get rejected.
private const val MIN_SPEECH_LEVEL_DB = -45f

// Measured with ERes2Net on real voices (Mini LibriSpeech, 20 s parts): the same person scored >= 0.90,
// two different people <= 0.41.
private const val MIN_PART_SIMILARITY = 0.6f

/**
 * Listens for [seconds] via [readFrame] (fills a [FRAME_SAMPLES] frame, false when the mic fails),
 * keeps only the speech and returns its embedding, plus the music/echo copies' ones. Rejects recordings that
 * are too short, too quiet, or whose thirds don't sound like the same person (someone else or the radio got in).
 */
fun enrollVoice(models: SpeechModels, seconds: Int, readFrame: (FloatArray) -> Boolean): Result<VoiceSample> {
    val frames = seconds * SAMPLE_RATE / FRAME_SAMPLES
    val speech = FloatArray(frames * FRAME_SAMPLES)
    var speechSamples = 0
    var levelDbSum = 0f
    val frame = FloatArray(FRAME_SAMPLES)

    repeat(frames) {
        if (!readFrame(frame)) return Result.failure(EnrollException(EnrollException.Reason.MIC_STOPPED))
        if (models.speechProbability(frame) >= 0.5f) {
            frame.copyInto(speech, speechSamples)
            speechSamples += FRAME_SAMPLES
            levelDbSum += rmsDb(frame)
        }
    }

    val heardSeconds = speechSamples / SAMPLE_RATE
    if (heardSeconds < MIN_ENROLL_SPEECH_SECONDS) {
        return Result.failure(EnrollException(EnrollException.Reason.TOO_LITTLE_SPEECH, heardSeconds))
    }
    if (levelDbSum / (speechSamples / FRAME_SAMPLES) < MIN_SPEECH_LEVEL_DB) {
        return Result.failure(EnrollException(EnrollException.Reason.TOO_QUIET))
    }

    val third = speechSamples / 3
    val parts = (0 until 3).map { models.embed(speech.copyOfRange(it * third, (it + 1) * third)) }
    val consistent = parts.indices.all { i -> parts.indices.all { j -> cosine(parts[i], parts[j]) >= MIN_PART_SIMILARITY } }
    if (!consistent) return Result.failure(EnrollException(EnrollException.Reason.INCONSISTENT))

    val heard = speech.copyOf(speechSamples)
    // Always made, so the "music and echo" switch works for this recording whenever it is turned on.
    return Result.success(VoiceSample(models.embed(heard), augmentedFingerprints(models, heard, models.augmentationMusic())))
}
