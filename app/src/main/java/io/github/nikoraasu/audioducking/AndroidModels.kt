package io.github.nikoraasu.audioducking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.AudioTagging
import com.k2fsa.sherpa.onnx.AudioTaggingConfig
import com.k2fsa.sherpa.onnx.AudioTaggingModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig

class AndroidModels(private val assets: AssetManager) : SpeechModels {
    private val vad = Vad(
        assets,
        VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(model = "silero_vad.onnx", windowSize = FRAME_SAMPLES),
            sampleRate = SAMPLE_RATE,
            numThreads = 1,
            debug = false
        )
    )

    // Loaded on first use: only needed for registered voices. 
    private var extractor: SpeakerEmbeddingExtractor? = null

    // Loaded on first use: not needed for voice registration.
    private var tagger: AudioTagging? = null

    override fun speechProbability(frame: FloatArray) = vad.compute(frame)

    @Synchronized
    override fun speechClassScore(window: FloatArray): Float {
        val tagger = tagger ?: AudioTagging(
            assets,
            AudioTaggingConfig(
                model = AudioTaggingModelConfig(ced = "ced_base_int8.onnx", numThreads = 1, debug = false),
                labels = "ced_labels.csv",
                topK = 527 // AudioSet; all of them, so "Speech" (index 0) is always in the result
            )
        ).also { tagger = it }
        val stream = tagger.createStream()
        try {
            stream.acceptWaveform(window, SAMPLE_RATE)
            return tagger.compute(stream, 527).firstOrNull { it.index == 0 }?.prob ?: 0f
        } finally {
            stream.release()
        }
    }

    @Synchronized
    override fun embed(samples: FloatArray): FloatArray {
        val extractor = extractor ?: SpeakerEmbeddingExtractor(
            assets,
            SpeakerEmbeddingExtractorConfig(model = VOICE_MODEL_FILE, numThreads = 1, debug = false)
        ).also { extractor = it }
        val stream = extractor.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            stream.inputFinished()
            return extractor.compute(stream)
        } finally {
            stream.release()
        }
    }

    override fun augmentationMusic() = AUGMENT_MUSIC.map { name -> pcm16(assets.open(name).use { it.readBytes() }) }

    @Synchronized
    override fun release() {
        vad.release()
        extractor?.release()
        tagger?.release()
    }
}

/**
 * Microphone setups Android offers. Voice recognition is the default: it is tuned for voices at a
 * distance from any direction (like assistants), and the Android compatibility rules require the same
 * behaviour on every phone (flat response, no noise suppression or gain control). The call microphone
 * (VOICE_COMMUNICATION) is deliberately not offered: it focuses on a mouth right at the phone.
 */
enum class MicSource(val androidSource: Int) {
    VOICE_RECOGNITION(MediaRecorder.AudioSource.VOICE_RECOGNITION),
    MIC(MediaRecorder.AudioSource.MIC),
    CAMCORDER(MediaRecorder.AudioSource.CAMCORDER),
    UNPROCESSED(MediaRecorder.AudioSource.UNPROCESSED)
}

fun unprocessedMicSupported(context: Context) =
    context.getSystemService(AudioManager::class.java)
        .getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"

/** The chosen source, falling back to voice recognition if the phone can't do it. */
fun micSource(context: Context): Int {
    val chosen = MicSource.entries.firstOrNull { it.name == Settings(context).micSource } ?: MicSource.VOICE_RECOGNITION
    val usable = chosen != MicSource.UNPROCESSED || unprocessedMicSupported(context)
    return (if (usable) chosen else MicSource.VOICE_RECOGNITION).androidSource
}

/** Records [ENROLL_SECONDS] from the mic on a background thread and returns the voice's embedding. */
@SuppressLint("MissingPermission") // checked below
fun enrollAndroidVoice(context: Context, onDone: (Result<VoiceSample>) -> Unit) {
    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        onDone(Result.failure(EnrollException(EnrollException.Reason.NO_PERMISSION)))
        return
    }
    Thread({
        val result = runCatching {
            val record = AudioRecord(
                micSource(context), // same microphone as when listening, so the fingerprints match
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(
                    AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
                    FRAME_SAMPLES * 2 * 4
                )
            )
            val models = AndroidModels(context.assets)
            try {
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    throw EnrollException(EnrollException.Reason.MIC_UNAVAILABLE)
                }
                record.startRecording()
                val shorts = ShortArray(FRAME_SAMPLES)
                enrollVoice(models, ENROLL_SECONDS) { frame ->
                    val ok = record.read(shorts, 0, shorts.size) == shorts.size
                    for (i in shorts.indices) frame[i] = shorts[i] / 32768f
                    ok
                }.getOrThrow()
            } finally {
                record.release()
                models.release()
            }
        }
        onDone(result)
    }, "voice-enroll").start()
}
