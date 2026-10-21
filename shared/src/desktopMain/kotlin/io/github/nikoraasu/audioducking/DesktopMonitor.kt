package io.github.nikoraasu.audioducking

import java.util.concurrent.Executors

/** Listens to the default microphone and changes the system volume while someone talks (see [SpeechProcessor]). */
class DesktopMonitor(private val settings: Settings, private val volume: SystemVolume) {
    @Volatile private var running = false
    private var thread: Thread? = null
    private var originalVolume: Float? = null
    private val pauser = PodcastPauser(desktopMediaControl())

    fun start() {
        if (running) return
        running = true
        Monitor.status.value = MonitorStatus(running = true)
        thread = Thread(::listen, "speech-monitor").apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        thread?.join(1000)
        thread = null
        restore()
        Monitor.status.value = MonitorStatus()
    }

    private fun listen() {
        val classifier = Executors.newSingleThreadExecutor { Thread(it, "speech-classifier").apply { isDaemon = true } }
        val voiceChecks = Executors.newSingleThreadExecutor { Thread(it, "voice-check").apply { isDaemon = true } }
        val models = DesktopModels()
        try {
            DesktopMic().use { mic ->
                val processor = SpeechProcessor(settings, models, { classifier.execute(it) }, { voiceChecks.execute(it) })
                val frame = FloatArray(FRAME_SAMPLES)
                while (running) {
                    if (!mic.read(frame)) continue
                    if (processor.onFrame(frame, System.currentTimeMillis())) {
                        val effect = processor.effect
                        if (effect != null) apply(effect) else restore()
                        pauser.update(effect, settings.rewindSeconds * 1000)
                    }
                    val status = MonitorStatus(
                        running = true,
                        effect = processor.effect,
                        speaker = processor.speaker,
                        matchScore = processor.matchScore
                    )
                    if (status != Monitor.status.value) Monitor.status.value = status
                }
            }
        } catch (e: Exception) {
            System.err.println("Speech monitor stopped: $e")
            running = false
            Monitor.status.value = MonitorStatus(error = MonitorError.STOPPED_UNEXPECTEDLY)
        } finally {
            for (executor in listOf(classifier, voiceChecks)) {
                executor.shutdown()
                executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)
            }
            models.release()
            restore()
            pauser.update(null, settings.rewindSeconds * 1000) // stopped mid-conversation: play again
        }
    }

    // Podcast mode leaves the volume as it is (see speechVolume). Volume changes go through here only when the effect changes, so the same original is reused.
    @Synchronized private fun apply(effect: SpeechEffect) {
        val original = originalVolume ?: volume.get().also { originalVolume = it }
        volume.set(speechVolume(effect.mode, original, effect.level))
    }

    @Synchronized private fun restore() {
        val original = originalVolume ?: return
        volume.set(original)
        originalVolume = null
    }
}

/** Records [ENROLL_SECONDS] from the default microphone and returns the voice's embedding. */
fun enrollDesktopVoice(onDone: (Result<VoiceSample>) -> Unit) {
    Thread({
        val result = runCatching {
            val models = DesktopModels()
            try {
                DesktopMic().use { mic -> enrollVoice(models, ENROLL_SECONDS, mic::read).getOrThrow() }
            } finally {
                models.release()
            }
        }
        onDone(result)
    }, "voice-enroll").apply { isDaemon = true; start() }
}
