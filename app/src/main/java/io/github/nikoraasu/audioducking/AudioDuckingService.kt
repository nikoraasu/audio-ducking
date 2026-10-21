package io.github.nikoraasu.audioducking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import android.app.PendingIntent

class AudioDuckingService : Service() {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private lateinit var audioManager: AudioManager

    // Volume model: the user's own volume (baseVolume) + car boost, with the speech mode on top.
    private var baseVolume = -1f // 0..1, -1 until the first update
    private var lastSetIndex = -1
    private var appliedCarBoost = 0f
    @Volatile private var speechEffect: SpeechEffect? = null
    @Volatile private var speaker: String? = null
    @Volatile private var matchScore: Float? = null

    @Volatile private var carBoost = 0f // what is applied now: eases towards carBoostTarget() (see Ramp)
    private val carRamp = Ramp(DEFAULT_CAR_TRANSITION_SECONDS)
    private val acceleration = AccelerationTracker()
    @Volatile private var speedKmh = 0f
    @Volatile private var currentSpeedStep = 0
    @Volatile private var noiseDb = QUIET_NOISE_DB

    private val locationListener = LocationListener { location ->
        if (location.hasSpeed()) {
            speedKmh = location.speed * 3.6f
            acceleration.update(speedKmh, SystemClock.elapsedRealtime())
            currentSpeedStep = speedStep(speedKmh, currentSpeedStep, speedSteps.map { it.kmh })
            refreshVolume()
        }
    }

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private lateinit var settings: Settings
    private var carMode = CarMode.OFF
    @Volatile private var speedSteps = emptyList<SpeedStep>()
    @Volatile private var smoothSteps = true
    @Volatile private var accelerationBoostMax = DEFAULT_ACCELERATION_BOOST
    private var noiseBoostMax = 0.2f

    private val pauser by lazy { PodcastPauser(AndroidMediaControl(this)) }
    private var overlay: StatusOverlay? = null
    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val channelId = "audio_ducking_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        loadSettings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadSettings()

        val micPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO)

        if (micPermission != PackageManager.PERMISSION_GRANTED) {
            fail(MonitorError.NO_PERMISSION)
            return START_NOT_STICKY
        }

        startForegroundWithText(strings().notificationListening)
        ServiceStateStore.setRunning(this, true)
        if (!isRecording) Monitor.status.value = MonitorStatus(running = true)

        if (!isRecording) {
            startSpeechMonitoring()
            startCarSensors()
            startOverlay()
        }

        return START_STICKY
    }

    private fun loadSettings() {
        settings = Settings(this)
        carMode = settings.carMode
        loadCarTuning()
    }

    // Car sliders apply while listening too.
    private fun loadCarTuning() {
        speedSteps = settings.speedSteps
        smoothSteps = settings.carSmoothSteps
        accelerationBoostMax = settings.accelerationBoostMax
        carRamp.seconds = settings.carTransitionSeconds
        noiseBoostMax = settings.noiseBoostMaxPercent
    }

    private fun startForegroundWithText(text: String) {
        val notification = buildNotification(text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (carMode != CarMode.OFF && hasLocationPermission()) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(notificationId, notification, types)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Audio Ducking")
            .setContentText(text)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Audio Ducking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = strings().notificationChannelDescription
                setSound(null, null)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun carActive() = carMode == CarMode.ON

    /** Where the car boost is heading: speed (smooth or stepped) + background noise + acceleration. */
    private fun carBoostTarget(nowMs: Long): Float {
        if (!carActive()) return 0f
        acceleration.expire(nowMs)
        val speed = if (smoothSteps) smoothSpeedBoost(speedKmh, speedSteps) else speedBoost(currentSpeedStep, speedSteps)
        return speed + noiseBoost(noiseDb, noiseBoostMax) + accelerationBoost(acceleration.acceleration, accelerationBoostMax)
    }

    /** Moves the car boost a step towards its target and the media volume to where it should be now. */
    @Synchronized
    private fun refreshVolume() {
        carBoost = carRamp.step(carBoostTarget(SystemClock.elapsedRealtime()), SystemClock.elapsedRealtime())

        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Someone else (the volume buttons) moved it: that is the user's new normal.
        if (current != lastSetIndex) {
            baseVolume = (current.toFloat() / max - appliedCarBoost).coerceIn(0f, 1f)
        }

        val target = targetVolume(baseVolume, carBoost, speechEffect)
        var index = (target * max).roundToInt()
        if (target > 0f) index = index.coerceAtLeast(1) // never round down to mute

        if (index != current) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        lastSetIndex = index
        appliedCarBoost = carBoost
    }

    /** Puts the user's own volume back, unless they changed it themselves since our last change. */
    @Synchronized
    private fun restoreUserVolume() {
        if (baseVolume < 0f) return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == lastSetIndex) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (baseVolume * max).roundToInt(), 0)
        }
        baseVolume = -1f
        lastSetIndex = -1
    }

    // Read each time, so a language change shows up even while listening.
    private fun strings() = stringsFor(resolveLanguage(Settings(this).language))

    private fun statusText(): String {
        val strings = strings()
        val effect = speechEffect
        val score = matchScore?.let { "%.2f".format(it) }
        val who = if (score == null) "" else " (${speaker ?: strings.unknownVoice} $score)"
        val speech = when {
            effect == null && score != null -> strings.notificationVoiceHeard(who)
            effect == null -> strings.notificationListening
            effect.mode == Mode.DUCK -> strings.notificationLowered(who)
            effect.mode == Mode.BOOST -> strings.notificationRaised(who)
            else -> strings.notificationPaused(who)
        }
        if (!carActive()) return speech
        return "$speech · " + strings.notificationCar(speedKmh.roundToInt(), noiseDb.roundToInt(), (carBoost * 100).roundToInt())
    }

    // Shown while the app's screen is away, if the user turned it on and allowed drawing over apps.
    private fun startOverlay() {
        overlayScope.launch {
            AppVisibility.visible.collect { visible ->
                if (!visible && settings.floatingStatus) {
                    overlay = overlay ?: StatusOverlay(this@AudioDuckingService)
                    overlay?.show()
                } else {
                    overlay?.hide()
                }
            }
        }
    }

    private fun hasLocationPermission() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startCarSensors() {
        if (carMode == CarMode.OFF) return

        if (!hasLocationPermission()) return // noise boost still works without speed
        try {
            getSystemService(LocationManager::class.java).requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, mainLooper
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) { // no GPS on this device
        }
    }

    private fun stopCarSensors() {
        getSystemService(LocationManager::class.java).removeUpdates(locationListener)
    }

    private fun fail(reason: MonitorError) {
        ServiceStateStore.setRunning(this, false)
        Monitor.status.value = MonitorStatus(error = reason)
        stopSelf()
    }

    private fun startSpeechMonitoring() {
        val hasPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            fail(MonitorError.NO_PERMISSION)
            return
        }

        val frameSamples = FRAME_SAMPLES
        val bytesPerFrame = frameSamples * 2

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            channelConfig,
            audioFormat
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            fail(MonitorError.MIC_UNAVAILABLE)
            return
        }

        // 1 s: the sound classifier briefly holds up the reading thread about twice a second.
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

        try {
            audioRecord = AudioRecord(
                micSource(this),
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                fail(MonitorError.MIC_UNAVAILABLE)
                return
            }

            audioRecord?.startRecording()
            isRecording = true
        } catch (e: SecurityException) {
            fail(MonitorError.MIC_UNAVAILABLE)
            return
        } catch (e: IllegalStateException) {
            fail(MonitorError.MIC_UNAVAILABLE)
            return
        }

        recordingThread = Thread {
            val classifier = Executors.newSingleThreadExecutor()
            val voiceChecks = Executors.newSingleThreadExecutor()
            try {
                val models = AndroidModels(assets)
                try {
                    val processor = SpeechProcessor(settings, models, { classifier.execute(it) }, { voiceChecks.execute(it) })
                    val shorts = ShortArray(frameSamples)
                    val frame = FloatArray(frameSamples)
                    var frames = 0
                    var seenSettings = SettingsVersion.value

                    while (isRecording) {
                        val read = audioRecord?.read(shorts, 0, shorts.size) ?: 0
                        if (read != shorts.size) continue

                        for (i in shorts.indices) frame[i] = shorts[i] / 32768f
                        val speechChanged = processor.onFrame(frame, SystemClock.elapsedRealtime())

                        // Ambient noise is only sampled between words, so talking doesn't count as noise.
                        if (!processor.lastFrameWasSpeech) noiseDb += (rmsDb(frame) - noiseDb) * NOISE_SMOOTHING

                        speechEffect = processor.effect
                        speaker = processor.speaker
                        matchScore = processor.matchScore
                        if (seenSettings != SettingsVersion.value) {
                            seenSettings = SettingsVersion.value
                            loadCarTuning()
                        }
                        if (speechChanged) pauser.update(speechEffect, settings.rewindSeconds * 1000)
                        val newSecond = ++frames % FRAMES_PER_SECOND == 0
                        // ~10 times a second while car mode is on, so its volume changes are gradual.
                        if (speechChanged || newSecond || (carActive() && frames % RAMP_EVERY_FRAMES == 0)) refreshVolume()
                        if (speechChanged || newSecond) {
                            updateNotification(statusText())
                            Monitor.status.value = MonitorStatus(
                                running = true,
                                effect = speechEffect,
                                speaker = speaker,
                                matchScore = matchScore,
                                carBoost = if (carActive()) carBoost else null
                            )
                        }
                    }
                } finally {
                    for (executor in listOf(classifier, voiceChecks)) {
                        executor.shutdown()
                        executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)
                    }
                    models.release()
                }
            } catch (_: Exception) {
                fail(MonitorError.STOPPED_UNEXPECTEDLY)
            }
        }

        recordingThread?.start()
    }

    private fun stopMonitoring() {
        isRecording = false
        stopCarSensors()

        try {
            recordingThread?.join(300)
        } catch (_: InterruptedException) {
        }

        recordingThread = null
        restoreUserVolume()
        pauser.update(null, settings.rewindSeconds * 1000) // stopped mid-conversation: play again
        overlayScope.cancel()
        overlay?.hide()

        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }

        audioRecord?.release()
        audioRecord = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        ServiceStateStore.setRunning(this, false)
        if (Monitor.status.value.error == null) Monitor.status.value = MonitorStatus() // keep a failure reason visible
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val FRAMES_PER_SECOND = 31 // 32 ms Silero frames
        const val RAMP_EVERY_FRAMES = 3 // ~100 ms

        // ~5 s averaging at 32 ms frames, so a passing truck doesn't jerk the volume.
        const val NOISE_SMOOTHING = 0.0064f
    }
}