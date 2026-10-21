package io.github.nikoraasu.audioducking

import kotlin.math.exp
import kotlin.math.log10

enum class CarMode {
    OFF,
    ON
}

/** From [kmh] on, car mode adds [boost] (the total extra volume at that speed, not on top of lower steps). */
data class SpeedStep(val kmh: Float, val boost: Float)

val DEFAULT_SPEED_STEPS_KMH = listOf(50f, 70f, 90f)
private const val SPEED_HYSTERESIS_KMH = 5f

/**
 * Number of speed thresholds passed. A step is only dropped once the speed is
 * [SPEED_HYSTERESIS_KMH] below its threshold, so cruising at ~50 km/h doesn't flap the volume.
 */
fun speedStep(kmh: Float, previousStep: Int, thresholds: List<Float> = DEFAULT_SPEED_STEPS_KMH): Int {
    val up = thresholds.count { kmh >= it }
    if (up >= previousStep) return up
    return minOf(previousStep, thresholds.count { kmh >= it - SPEED_HYSTERESIS_KMH })
}

/** Extra volume for having passed [step] of the (speed-sorted) [steps]. */
fun speedBoost(step: Int, steps: List<SpeedStep>): Float = if (step == 0) 0f else steps[step - 1].boost

const val MAX_SPEED_STEPS = 8

// Below the first step the smooth curve starts this far under it, so the first step is eased into too.
private const val SMOOTH_LEAD_IN_KMH = 20f

/**
 * Extra volume at [kmh] with the volume blended between the (speed-sorted) [steps]: 0 up to
 * [SMOOTH_LEAD_IN_KMH] below the first step, straight lines from step to step, flat after the last.
 * No hysteresis needed: a small speed change only moves the volume a little.
 */
fun smoothSpeedBoost(kmh: Float, steps: List<SpeedStep>): Float {
    if (steps.isEmpty()) return 0f
    val points = listOf(SpeedStep(steps[0].kmh - SMOOTH_LEAD_IN_KMH, 0f)) + steps
    if (kmh <= points.first().kmh) return 0f
    if (kmh >= points.last().kmh) return points.last().boost
    val upper = points.indexOfFirst { it.kmh > kmh }
    val a = points[upper - 1]
    val b = points[upper]
    return a.boost + (b.boost - a.boost) * (kmh - a.kmh) / (b.kmh - a.kmh)
}

/**
 * Acceleration (m/s², forward only) from successive GPS speeds, smoothed over a few seconds so
 * single noisy readings don't count. Readings more than [STALE_MS] apart (tunnel, GPS lost) restart it.
 */
class AccelerationTracker {
    private var lastKmh = 0f
    private var lastMs = -1L

    var acceleration = 0f
        private set

    fun update(kmh: Float, nowMs: Long): Float {
        val dtMs = nowMs - lastMs
        if (lastMs < 0 || dtMs > STALE_MS || dtMs <= 0) {
            acceleration = 0f
        } else {
            val raw = (kmh - lastKmh) / 3.6f / (dtMs / 1000f)
            acceleration += (raw - acceleration) * (1 - exp(-dtMs / SMOOTHING_MS))
        }
        lastKmh = kmh
        lastMs = nowMs
        return acceleration
    }

    /** No reading for a while (GPS lost): don't keep boosting for an old acceleration. */
    fun expire(nowMs: Long) {
        if (lastMs >= 0 && nowMs - lastMs > STALE_MS) acceleration = 0f
    }

    private companion object {
        const val STALE_MS = 3000L
        const val SMOOTHING_MS = 2000f
    }
}

// shortcut: 0.5 m/s² ignores GPS wobble and gentle speeding up; 2.5 m/s² (0-100 km/h in ~11 s) counts as
// hard. Rough values for an ordinary car; tune with real drives.
const val GENTLE_ACCELERATION = 0.5f
const val HARD_ACCELERATION = 2.5f

/** Extra volume (0..[maxBoost]) while speeding up, more the harder the acceleration (the engine gets louder). */
fun accelerationBoost(acceleration: Float, maxBoost: Float): Float =
    ((acceleration - GENTLE_ACCELERATION) / (HARD_ACCELERATION - GENTLE_ACCELERATION)).coerceIn(0f, 1f) * maxBoost

/**
 * Eases a value towards its target instead of jumping: after [seconds] it has covered ~95% of a change
 * (exponential, so big changes move faster at first). 0 seconds = instant.
 */
class Ramp(var seconds: Float) {
    var value = 0f
        private set
    private var lastMs = -1L

    fun step(target: Float, nowMs: Long): Float {
        val dt = if (lastMs < 0) 0f else (nowMs - lastMs) / 1000f // starts at 0 and eases in
        lastMs = nowMs
        value = if (seconds <= 0f || dt >= seconds * 3) target else value + (target - value) * (1 - exp(-3 * dt / seconds))
        if (kotlin.math.abs(target - value) < 0.01f) value = target // within 1%: settle instead of creeping forever
        return value
    }

    /** True while still moving towards [target]. */
    fun moving(target: Float) = value != target
}

// shortcut: fixed mic range from rough phone-mic estimates; calibrate with real car readings
// (the notification shows the live dB value) and turn into settings if one range doesn't fit all phones.
const val QUIET_NOISE_DB = -50f
const val LOUD_NOISE_DB = -25f

/** Extra volume (0..[maxBoost]) for the given ambient noise level in dBFS. */
fun noiseBoost(noiseDb: Float, maxBoost: Float): Float =
    ((noiseDb - QUIET_NOISE_DB) / (LOUD_NOISE_DB - QUIET_NOISE_DB)).coerceIn(0f, 1f) * maxBoost

/** Volume (0..1) to play at: the user's [base] plus the car boost, then the speech effect on top. */
fun targetVolume(base: Float, carBoost: Float, effect: SpeechEffect?): Float {
    val normal = (base + carBoost).coerceIn(0f, 1f)
    return if (effect == null) normal else speechVolume(effect.mode, normal, effect.level)
}

/** Loudness of a frame in dBFS (0 = full scale; silence is very negative). */
fun rmsDb(samples: FloatArray): Float {
    var sum = 0.0
    for (s in samples) sum += s * s
    return (10 * log10(sum / samples.size + 1e-10)).toFloat()
}
