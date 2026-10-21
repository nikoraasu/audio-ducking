package io.github.nikoraasu.audioducking

import com.k2fsa.sherpa.onnx.AudioTagging
import com.k2fsa.sherpa.onnx.AudioTaggingConfig
import com.k2fsa.sherpa.onnx.AudioTaggingModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

class DesktopModels : SpeechModels {
    private val vad = Vad(
        VadModelConfig.builder()
            .setSileroVadModelConfig(
                SileroVadModelConfig.builder().setModel(modelPath("silero_vad.onnx")).setWindowSize(FRAME_SAMPLES).build()
            )
            .setSampleRate(SAMPLE_RATE)
            .setNumThreads(1)
            .setDebug(false)
            .build()
    )

    // Loaded on first use: only needed for registered voices. 
    private var extractor: SpeakerEmbeddingExtractor? = null

    // Loaded on first use: not needed for voice registration.
    private var tagger: AudioTagging? = null

    override fun speechProbability(frame: FloatArray) = vad.compute(frame)

    @Synchronized
    override fun speechClassScore(window: FloatArray): Float {
        val tagger = tagger ?: AudioTagging(
            AudioTaggingConfig.builder()
                .setModel(AudioTaggingModelConfig.builder().setCED(modelPath(CED_MODEL)).setNumThreads(1).setDebug(false).build())
                .setLabels(modelPath(CED_LABELS))
                .setTopK(ALL_CLASSES)
                .build()
        ).also { tagger = it }
        val stream = tagger.createStream()
        try {
            stream.acceptWaveform(window, SAMPLE_RATE)
            return tagger.compute(stream).firstOrNull { it.index == SPEECH_CLASS }?.prob ?: 0f
        } finally {
            stream.release()
        }
    }

    @Synchronized
    override fun embed(samples: FloatArray): FloatArray {
        val extractor = extractor ?: SpeakerEmbeddingExtractor(
            SpeakerEmbeddingExtractorConfig.builder()
                .setModel(modelPath(VOICE_MODEL_FILE))
                .setNumThreads(1)
                .setDebug(false)
                .build()
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

    override fun augmentationMusic() = AUGMENT_MUSIC.map { name ->
        pcm16(DesktopModels::class.java.getResourceAsStream("/$name")!!.use { it.readBytes() })
    }

    @Synchronized
    override fun release() {
        vad.release()
        extractor?.release()
        tagger?.release()
    }

    private companion object {
        const val CED_MODEL = "ced_base_int8.onnx"
        const val CED_LABELS = "ced_labels.csv"
        const val ALL_CLASSES = 527 // AudioSet; all of them, so "Speech" is always in the result
        const val SPEECH_CLASS = 0
        private val extracted = mutableMapOf<String, String>()

        // sherpa-onnx needs a real file path, so copy each bundled model out once.
        @Synchronized
        fun modelPath(name: String): String = extracted.getOrPut(name) {
            val file = File.createTempFile(name.substringBefore('.'), "." + name.substringAfter('.')).apply { deleteOnExit() }
            DesktopModels::class.java.getResourceAsStream("/$name")!!.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        }
    }
}

/** Default microphone, 16 kHz mono, read one [FRAME_SAMPLES] frame at a time. */
class DesktopMic : AutoCloseable {
    private val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
    private val line: TargetDataLine = AudioSystem.getTargetDataLine(format).apply {
        open(format, SAMPLE_RATE * 2) // 1 s: the sound classifier briefly holds up the reading thread
        start()
    }
    private val bytes = ByteArray(FRAME_SAMPLES * 2)

    fun read(frame: FloatArray): Boolean {
        if (line.read(bytes, 0, bytes.size) != bytes.size) return false
        for (i in frame.indices) {
            val sample = (bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)
            frame[i] = sample.toShort() / 32768f
        }
        return true
    }

    override fun close() = line.close()
}
