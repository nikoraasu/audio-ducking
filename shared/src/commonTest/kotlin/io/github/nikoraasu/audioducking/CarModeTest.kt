package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarModeTest {
    @Test
    fun speedStepsUpAtEachThreshold() {
        assertEquals(0, speedStep(30f, 0))
        assertEquals(1, speedStep(50f, 0))
        assertEquals(2, speedStep(75f, 1))
        assertEquals(3, speedStep(120f, 0))
    }

    @Test
    fun speedStepHasHysteresisOnTheWayDown() {
        assertEquals(1, speedStep(47f, 1)) // within 5 km/h of 50: keep the step
        assertEquals(0, speedStep(44f, 1))
        assertEquals(2, speedStep(66f, 2))
        assertEquals(1, speedStep(60f, 2))
        assertEquals(0, speedStep(10f, 3))
    }

    @Test
    fun customStepsAndTheirBoosts() {
        val steps = listOf(SpeedStep(40f, 0.05f), SpeedStep(100f, 0.3f))
        val step = speedStep(110f, 0, steps.map { it.kmh })
        assertEquals(2, step)
        assertEquals(0.3f, speedBoost(step, steps))
        assertEquals(0f, speedBoost(0, steps))
    }

    @Test
    fun oldVolumePerStepBecomesTheDefaultBoosts() {
        val prefs = MapPrefs().apply { putFloat("speed_boost_percent", 0.05f) }
        val steps = Settings(prefs).speedSteps
        assertEquals(listOf(0.05f, 0.1f, 0.15f), steps.map { (it.boost * 100).roundToInt() / 100f })
    }

    @Test
    fun noiseBoostScalesBetweenQuietAndLoud() {
        assertEquals(0f, noiseBoost(-70f, 0.2f))
        assertEquals(0.1f, noiseBoost((QUIET_NOISE_DB + LOUD_NOISE_DB) / 2, 0.2f), 1e-6f)
        assertEquals(0.2f, noiseBoost(-10f, 0.2f))
    }

    @Test
    fun rmsDbOfFullScaleSineIsAboutMinus3() {
        val sine = FloatArray(512) { kotlin.math.sin(it * 0.3).toFloat() }
        assertEquals(-3f, rmsDb(sine), 0.2f)
        assertEquals(-100f, rmsDb(FloatArray(512)), 0.1f)
    }

    @Test
    fun speechModesApplyOnTopOfCarBoost() {
        assertEquals(0.7f, targetVolume(0.5f, 0.2f, null), 1e-6f)
        assertEquals(0.35f, targetVolume(0.5f, 0.2f, SpeechEffect(Mode.DUCK, 0.35f)), 1e-6f)
        assertEquals(0.9f, targetVolume(0.5f, 0.2f, SpeechEffect(Mode.BOOST, 0.9f)), 1e-6f)
        assertEquals(1f, targetVolume(0.9f, 0.3f, null))
    }

    private val steps = listOf(SpeedStep(50f, 0.1f), SpeedStep(70f, 0.2f), SpeedStep(90f, 0.3f))

    @Test
    fun smoothCurveBlendsBetweenSteps() {
        assertEquals(0f, smoothSpeedBoost(20f, steps))
        assertEquals(0.05f, smoothSpeedBoost(40f, steps), 1e-6f) // eases into the first step from 30 km/h
        assertEquals(0.1f, smoothSpeedBoost(50f, steps), 1e-6f)
        assertEquals(0.15f, smoothSpeedBoost(60f, steps), 1e-6f) // halfway between 50 and 70
        assertEquals(0.3f, smoothSpeedBoost(140f, steps), 1e-6f) // flat after the last step
        assertEquals(0f, smoothSpeedBoost(100f, emptyList()))
    }

    @Test
    fun accelerationIsSmoothedAndForgottenWhenGpsIsLost() {
        val tracker = AccelerationTracker()
        var t = 0L
        var kmh = 0f
        tracker.update(kmh, t)
        repeat(10) { // 0-100 km/h in ~10 s: 2.8 m/s², one GPS reading a second
            t += 1000; kmh += 10f
            tracker.update(kmh, t)
        }
        assertEquals(2.78f, tracker.acceleration, 0.1f)
        assertEquals(0.1f, accelerationBoost(tracker.acceleration, 0.1f), 1e-6f)

        tracker.update(kmh + 15f, t + 1000) // one jumpy reading only moves it part of the way
        assertTrue(tracker.acceleration < 4.17f * 0.5f + 2.78f * 0.5f + 0.1f)

        tracker.expire(t + 10_000) // tunnel: no readings
        assertEquals(0f, tracker.acceleration)
    }

    @Test
    fun gentleSpeedingUpAndBrakingAddNothing() {
        assertEquals(0f, accelerationBoost(0.4f, 0.1f))
        assertEquals(0f, accelerationBoost(-3f, 0.1f))
        assertEquals(0.05f, accelerationBoost(1.5f, 0.1f), 1e-6f)
    }

    @Test
    fun rampEasesInsteadOfJumping() {
        val ramp = Ramp(3f)
        ramp.step(0.3f, 0)
        assertEquals(0f, ramp.value) // starts where it is, no jump
        val afterOne = ramp.step(0.3f, 1000)
        assertTrue(afterOne > 0.1f && afterOne < 0.25f, "after 1 of 3 s: $afterOne")
        ramp.step(0.3f, 3000)
        assertEquals(0.3f, ramp.value, 0.02f) // ~95% after the transition time
        ramp.step(0.3f, 4000)
        assertFalse(ramp.moving(0.3f)) // settles exactly
        assertEquals(0.1f, Ramp(0f).step(0.1f, 0)) // 0 s = instant
    }

    @Test
    fun anyNumberOfStepsIsStored() {
        val s = Settings(MapPrefs())
        assertEquals(DEFAULT_SPEED_STEPS_KMH, s.speedSteps.map { it.kmh })
        s.speedSteps = listOf(SpeedStep(30f, 0.05f), SpeedStep(60f, 0.1f), SpeedStep(90f, 0.15f), SpeedStep(120f, 0.2f), SpeedStep(140f, 0.3f))
        assertEquals(listOf(30f, 60f, 90f, 120f, 140f), s.speedSteps.map { it.kmh })
        s.speedSteps = listOf(SpeedStep(80f, 0.2f))
        assertEquals(listOf(SpeedStep(80f, 0.2f)), s.speedSteps) // removed steps don't come back
    }
}
