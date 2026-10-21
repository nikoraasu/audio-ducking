package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Test audio: every sample is +level for person A, -level for person B, 0 for silence. */
private object SignModels : SpeechModels {
    override fun speechProbability(frame: FloatArray) = if (frame[0] != 0f) 1f else 0f
    override fun speechClassScore(window: FloatArray) = 1f
    override fun embed(samples: FloatArray): FloatArray {
        val mean = samples.average()
        return floatArrayOf(if (mean > 0) 1f else 0f, if (mean < 0) 1f else 0f)
    }
    override fun augmentationMusic() = listOf(FloatArray(SAMPLE_RATE) { 0.02f }, FloatArray(SAMPLE_RATE) { -0.01f })
    override fun release() {}
}

class EnrollTest {
    /** [timeline] gives the sample value for each second of the recording. */
    private fun enroll(timeline: (second: Int) -> Float): Result<VoiceSample> {
        var frameIndex = 0
        return enrollVoice(SignModels, ENROLL_SECONDS) { frame ->
            frame.fill(timeline(frameIndex++ * FRAME_SAMPLES / SAMPLE_RATE))
            true
        }
    }

    private fun reason(result: Result<VoiceSample>) = (result.exceptionOrNull() as EnrollException).reason

    @Test
    fun aGoodRecordingIsAccepted() {
        assertTrue(enroll { 0.1f }.isSuccess) // -20 dBFS, one person
    }

    @Test
    fun tooQuietIsRejected() {
        assertEquals(EnrollException.Reason.TOO_QUIET, reason(enroll { 0.001f })) // -60 dBFS
    }

    @Test
    fun aSecondVoiceIsRejected() {
        val result = enroll { second -> if (second < 40) 0.1f else -0.1f } // last third is someone else
        assertEquals(EnrollException.Reason.INCONSISTENT, reason(result))
    }

    @Test
    fun tooLittleSpeechIsRejected() {
        val result = enroll { second -> if (second < 8) 0.1f else 0f }
        assertEquals(EnrollException.Reason.TOO_LITTLE_SPEECH, reason(result))
        assertEquals(8, (result.exceptionOrNull() as EnrollException).heardSeconds)
    }

    @Test
    fun aRecordingAlsoGetsMusicAndEchoCopies() {
        val sample = enroll { 0.1f }.getOrThrow()
        assertEquals(MAX_EXTRAS_PER_SAMPLE, sample.extras.size) // 2 music + 2 rooms
        assertEquals(listOf(1f, 0f), sample.extras[0].toList()) // still person A with music under it
    }
}
