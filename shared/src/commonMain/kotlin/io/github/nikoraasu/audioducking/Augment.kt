package io.github.nikoraasu.audioducking

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Extra fingerprints made at registration from copies of the recording with music mixed in and with
 * simulated room echo, so a voice heard over music or from further away still finds a close sample.
 *
 * Measured with ERes2Net on real voices (Mini LibriSpeech, 25 speakers, 1.5 s checks, test songs and
 * rooms different from these): in a simulated car (echo + rumble + music 5 dB under the voice) the
 * registered person was missed 36.5% -> ~25% of the time, strangers got through 0.8% -> ~2%. Car noise
 * copies were also tried and added almost nothing, so they are left out.
 */
const val AUGMENT_PART_SECONDS = 15 // 15 s copies worked as well as full 60 s ones at a quarter of the work

/**
 * Bundled clips: 20 s each of "Hackbeat" and "Volatile Reaction" by Kevin MacLeod (incompetech.com),
 * CC BY 4.0, as raw 16-bit little-endian mono PCM at [SAMPLE_RATE]. The credit is in [Strings.extraSamplesInfo].
 */
val AUGMENT_MUSIC = listOf("augment_music_1.pcm", "augment_music_2.pcm")

/** Raw 16-bit little-endian PCM to samples in -1..1. */
fun pcm16(bytes: ByteArray) = FloatArray(bytes.size / 2) {
    ((bytes[2 * it].toInt() and 0xFF) or (bytes[2 * it + 1].toInt() shl 8)).toShort() / 32768f
}

/** Music copies (each bundled clip at its own level under the voice), then the two rooms. */
fun augmentedFingerprints(
    models: SpeechModels,
    speech: FloatArray,
    music: List<FloatArray>
): List<FloatArray> {
    val partLength = AUGMENT_PART_SECONDS * SAMPLE_RATE
    fun part(index: Int): FloatArray {
        val start = minOf(index * partLength, (speech.size - partLength).coerceAtLeast(0))
        return speech.copyOfRange(start, minOf(start + partLength, speech.size))
    }
    val musicCopies = music.take(MUSIC_LEVELS_DB.size).mapIndexed { i, clip -> mixUnder(part(i), clip, MUSIC_LEVELS_DB[i]) }
    val echoCopies = ROOMS.mapIndexed { i, room -> withEcho(part(i + 1), room) }
    return (musicCopies + echoCopies).map { models.embed(it) }
}

// How far under the voice each music clip is mixed (dB).
private val MUSIC_LEVELS_DB = floatArrayOf(10f, 5f)

private class Room(val reverbSeconds: Float, val directGain: Float, val seed: Int)

// A small, fairly dead room (like a car) and a larger, more echoing one, both a little away from the mic.
private val ROOMS = listOf(Room(0.15f, 0.5f, 11), Room(0.45f, 0.3f, 12))

/** [speech] with [clip] (looped) mixed in [dbUnder] dB quieter than the speech. */
internal fun mixUnder(speech: FloatArray, clip: FloatArray, dbUnder: Float): FloatArray {
    if (clip.isEmpty()) return speech.copyOf()
    val looped = FloatArray(speech.size) { clip[it % clip.size] }
    val gain = rms(speech) / rms(looped) / 10f.pow(dbUnder / 20f)
    return FloatArray(speech.size) { speech[it] + gain * looped[it] }
}

/** [speech] as heard in [room]: direct sound plus exponentially fading random reflections, same loudness. */
private fun withEcho(speech: FloatArray, room: Room): FloatArray {
    val random = Random(room.seed)
    val length = (room.reverbSeconds * SAMPLE_RATE).toInt()
    val response = FloatArray(length) { i ->
        when {
            i == 0 -> room.directGain
            i < 40 -> 0f // ~2.5 ms before the first reflections arrive
            else -> (gaussian(random) * exp(-6.9 * i / length) * 0.12).toFloat() // -60 dB over the reverb time
        }
    }
    val echoed = convolve(speech, response)
    val gain = rms(speech) / rms(echoed)
    return FloatArray(echoed.size) { echoed[it] * gain }
}

private fun rms(x: FloatArray): Float {
    var sum = 0.0
    for (v in x) sum += v * v
    return sqrt(sum / x.size + 1e-12).toFloat()
}

private fun gaussian(random: Random): Double { // Box-Muller
    val u = random.nextDouble().coerceAtLeast(1e-12)
    return sqrt(-2 * ln(u)) * cos(2 * PI * random.nextDouble())
}

/** First [x].size samples of [x] convolved with [h], via FFT (direct convolution would take seconds). */
private fun convolve(x: FloatArray, h: FloatArray): FloatArray {
    var n = 1
    while (n < x.size + h.size) n = n shl 1
    val ar = DoubleArray(n)
    val ai = DoubleArray(n)
    val br = DoubleArray(n)
    val bi = DoubleArray(n)
    for (i in x.indices) ar[i] = x[i].toDouble()
    for (i in h.indices) br[i] = h[i].toDouble()
    fft(ar, ai, inverse = false)
    fft(br, bi, inverse = false)
    for (i in 0 until n) {
        val re = ar[i] * br[i] - ai[i] * bi[i]
        val im = ar[i] * bi[i] + ai[i] * br[i]
        ar[i] = re
        ai[i] = im
    }
    fft(ar, ai, inverse = true)
    return FloatArray(x.size) { (ar[it] / n).toFloat() }
}

/** In-place radix-2 FFT; [re].size must be a power of two. */
private fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            re[i] = re[j].also { re[j] = re[i] }
            im[i] = im[j].also { im[j] = im[i] }
        }
    }
    var len = 2
    while (len <= n) {
        val angle = 2 * PI / len * (if (inverse) 1 else -1)
        val wr = cos(angle)
        val wi = sin(angle)
        for (start in 0 until n step len) {
            var cr = 1.0
            var ci = 0.0
            for (k in 0 until len / 2) {
                val a = start + k
                val b = a + len / 2
                val xr = re[b] * cr - im[b] * ci
                val xi = re[b] * ci + im[b] * cr
                re[b] = re[a] - xr
                im[b] = im[a] - xi
                re[a] += xr
                im[a] += xi
                val next = cr * wr - ci * wi
                ci = cr * wi + ci * wr
                cr = next
            }
        }
        len = len shl 1
    }
}
