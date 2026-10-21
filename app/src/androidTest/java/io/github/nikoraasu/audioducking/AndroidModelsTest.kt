package io.github.nikoraasu.audioducking

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sin

/** All bundled models load from the APK's assets and produce sane output on a device. */
@RunWith(AndroidJUnit4::class)
class AndroidModelsTest {
    @Test
    fun modelsLoadFromAssets() {
        val models = AndroidModels(InstrumentationRegistry.getInstrumentation().targetContext.assets)
        try {
            assertTrue(models.speechProbability(FloatArray(FRAME_SAMPLES)) < 0.5f) // silence
            assertTrue(models.speechClassScore(FloatArray(SAMPLE_RATE)) < 0.2f) // classifier loads; silence isn't speech

            val tone = FloatArray(2 * SAMPLE_RATE) { 0.3f * sin(it * 0.05f) }
            val first = models.embed(tone)
            assertEquals(192, first.size) // ERes2Net fingerprint length
            assertEquals(1f, cosine(first, models.embed(tone)), 0.01f) // deterministic
        } finally {
            models.release()
        }
    }
}
