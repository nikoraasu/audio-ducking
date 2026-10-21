package io.github.nikoraasu.audioducking

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToInt

enum class Language(val code: String, val nativeName: String) {
    EN("en", "English"),
    PL("pl", "Polski"),
    DE("de", "Deutsch"),
    ES("es", "Español")
}

/** Two-letter code of the OS language (e.g. "pl"). */
expect fun systemLanguageCode(): String

/** [setting] is a [Language.code], or empty for "follow the system" (English if unsupported). */
fun resolveLanguage(setting: String): Language {
    val code = setting.ifEmpty { systemLanguageCode() }
    return Language.entries.firstOrNull { it.code == code } ?: Language.EN
}

fun stringsFor(language: Language): Strings = when (language) {
    Language.EN -> EnglishStrings
    Language.PL -> PolishStrings
    Language.DE -> GermanStrings
    Language.ES -> SpanishStrings
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

/** Why listening stopped; the screen words it in the current language. */
enum class MonitorError { NO_PERMISSION, MIC_UNAVAILABLE, STOPPED_UNEXPECTEDLY }

/**
 * Every user-visible text. Each language implements all of it, so a missing translation is a compile
 * error. The app name "Audio Ducking" is a brand and stays untranslated.
 */
interface Strings {
    val decimalSeparator: Char

    // Status card
    val notListening: String
    val notListeningDetail: String
    val listening: String
    val waitingForSpeech: String
    val ducking: String
    val boosting: String
    val paused: String
    fun playbackPaused(who: String): String
    val stopped: String
    fun isTalking(name: String?): String
    fun volumeLowered(who: String): String
    fun volumeRaised(who: String): String
    fun carBoost(percent: Int): String
    fun ignoringVoice(score: String): String
    /** Who is talking plus how well their voice matches, e.g. "Alex is talking (match 0.59)". */
    fun withMatch(who: String, score: String): String
    val startListening: String
    val stopListening: String
    fun error(error: MonitorError): String

    // Mode
    val mode: String
    val duck: String
    val noise: String
    val duckDescription: String
    val noiseDescription: String
    val podcast: String
    val podcastDescription: String
    val rewind: String
    val rewindInfo: String
    val mediaAccessNeeded: String
    val allowMediaAccess: String
    val volumeWhileTalking: String
    val duckLevelInfo: String
    val boostLevelInfo: String

    // Advanced
    val advanced: String
    val expand: String
    val collapse: String
    val speechHold: String
    val speechHoldInfo: String
    val allowedPause: String
    val allowedPauseInfo: String
    val speechThreshold: String
    val speechThresholdInfo: String

    // Voices
    val voices: String
    val voicesInfo: String
    val onlyRegisteredVoices: String
    val extraSamples: String
    /** Also carries the bundled music's licence credit (CC BY 4.0 requires it). */
    val extraSamplesInfo: String
    val matchStrictness: String
    val matchStrictnessInfo: String
    val noVoicesYet: String
    val outdatedVoices: String
    fun remove(name: String): String
    val registerVoice: String
    val name: String
    fun recording(secondsLeft: Int): String
    val processing: String
    fun enrollInstructions(seconds: Int): String
    fun sampleCount(count: Int): String
    val addSample: String
    fun addSampleTitle(name: String): String
    val addSampleHint: String
    /** ~25 s of natural, phonetically varied text to read while registering a voice. */
    val enrollPassage: String
    /** Shown above [enrollPassage]: the text is a help, not a script. */
    val enrollPassageNote: String
    val tooQuiet: String
    val inconsistentRecording: String
    val record: String
    val cancel: String
    val recordingFailed: String
    fun tooLittleSpeech(seconds: Int): String
    val micStopped: String
    val micPermissionNeeded: String

    // Car (Android)
    val car: String
    val carInfo: String
    val off: String
    val on: String
    val carDescription: String
    val speedSteps: String
    val speedStepsInfo: String
    fun speedStepTitle(number: Int): String
    val fromSpeed: String
    val extraVolume: String
    val addSpeedStep: String
    fun removeSpeedStep(number: Int): String
    val smoothSteps: String
    val smoothStepsInfo: String
    val transitionTime: String
    val transitionTimeInfo: String
    val accelerationBoost: String
    val accelerationBoostInfo: String
    val noiseBoostUpTo: String
    val noiseBoostInfo: String
    val carModeAppliesNextStart: String

    // Setup guide
    val setupGuide: String
    val setupGuideIntro: String
    /** Step titles and texts; [phone] adds the Android-only parts (Quick Settings tile, car). */
    fun setupSteps(phone: Boolean): List<Pair<String, String>>
    val useRecommendedValues: String
    val close: String

    // General
    val ok: String
    fun about(topic: String): String
    val language: String
    val microphone: String
    val microphoneInfo: String
    val micAssistant: String
    val micStandard: String
    val micVideo: String
    val micRaw: String
    val systemLanguage: String
    val addTile: String

    val display: String
    val floatingStatus: String
    val floatingStatusInfo: String
    // Android notification
    val notificationListening: String
    fun notificationVoiceHeard(who: String): String
    fun notificationLowered(who: String): String
    fun notificationRaised(who: String): String
    fun notificationPaused(who: String): String
    fun notificationCar(kmh: Int, noiseDb: Int, boostPercent: Int): String
    val notificationChannelDescription: String
    val unknownVoice: String
}

/** 0.5 -> "0.50" / "0,50". */
fun Strings.decimal(value: Float): String {
    val hundredths = (value * 100).roundToInt()
    return "${hundredths / 100}$decimalSeparator${(hundredths % 100).toString().padStart(2, '0')}"
}

/** 1500 ms -> "1.5 s" / "1,5 s". */
fun Strings.seconds(ms: Float): String {
    val tenths = (ms / 100).roundToInt()
    return "${tenths / 10}$decimalSeparator${tenths % 10} s"
}

/** Message for a failed voice recording. */
fun Strings.enrollError(error: Throwable): String = when ((error as? EnrollException)?.reason) {
    EnrollException.Reason.MIC_STOPPED -> micStopped
    EnrollException.Reason.TOO_LITTLE_SPEECH -> tooLittleSpeech((error as EnrollException).heardSeconds)
    EnrollException.Reason.TOO_QUIET -> tooQuiet
    EnrollException.Reason.INCONSISTENT -> inconsistentRecording
    EnrollException.Reason.NO_PERMISSION -> micPermissionNeeded
    EnrollException.Reason.MIC_UNAVAILABLE -> error(MonitorError.MIC_UNAVAILABLE)
    null -> recordingFailed
}

object EnglishStrings : Strings {
    override val decimalSeparator = '.'

    override val notListening = "Not listening"
    override val notListeningDetail = "Start to lower or raise the volume when someone talks."
    override val listening = "Listening"
    override val waitingForSpeech = "Waiting for someone to talk…"
    override val ducking = "Ducking"
    override val boosting = "Boosting"
    override val paused = "Paused"
    override fun playbackPaused(who: String) = "$who, playback paused."
    override val stopped = "Stopped"
    override fun isTalking(name: String?) = if (name != null) "$name is talking" else "Someone is talking"
    override fun volumeLowered(who: String) = "$who, volume lowered."
    override fun volumeRaised(who: String) = "$who, volume raised."
    override fun carBoost(percent: Int) = "Car mode: +$percent% volume"
    override fun ignoringVoice(score: String) = "Ignoring an unregistered voice (match $score)."
    override fun withMatch(who: String, score: String) = "$who (match $score)"
    override val startListening = "Start listening"
    override val stopListening = "Stop listening"
    override fun error(error: MonitorError) = when (error) {
        MonitorError.NO_PERMISSION -> "Microphone permission is missing."
        MonitorError.MIC_UNAVAILABLE -> "Could not open the microphone."
        MonitorError.STOPPED_UNEXPECTEDLY -> "Listening stopped unexpectedly."
    }

    override val mode = "Mode"
    override val duck = "Duck"
    override val noise = "Noise"
    override val duckDescription = "Quieter while someone talks, so you can hear them."
    override val noiseDescription = "Louder while someone talks, so you can keep listening."
    override val podcast = "Podcast"
    override val podcastDescription = "Pauses while someone talks, then plays again a few seconds back, so you miss nothing."
    override val rewind = "Go back after the pause"
    override val rewindInfo =
        "How far back playback resumes when the conversation ends. With speakers, the app can hear the podcast itself and pause for it; " +
            "headphones or \"Only registered voices\" avoid that."
    override val mediaAccessNeeded =
        "To rewind, the app needs access to media playback (notification access). Without it, it only pauses and plays."
    override val allowMediaAccess = "Allow"
    override val volumeWhileTalking = "Volume while talking"
    override val duckLevelInfo = "How low the media volume goes while someone talks. It is never raised by this."
    override val boostLevelInfo =
        "How loud the media volume goes while someone talks. It is never lowered by this, and goes back when they stop."

    override val advanced = "Advanced"
    override val expand = "Expand"
    override val collapse = "Collapse"
    override val speechHold = "Speech hold"
    override val speechHoldInfo =
        "How long someone must talk before the volume changes. Longer ignores short words and noises."
    override val allowedPause = "Allowed pause"
    override val allowedPauseInfo =
        "How long a silence can last before the conversation counts as over and the volume goes back."
    override val speechThreshold = "Speech threshold"
    override val speechThresholdInfo =
        "How sure the detector must be that a sound is speech. Lower catches quiet or distant voices but may react to other noises."

    override val voices = "Voices"
    override val voicesInfo =
        "Record the people you want the app to react to. With \"Only registered voices\" on, everyone else, " +
            "the radio and podcasts are ignored. Recognising who talks takes about 1.5 s of speech."
    override val onlyRegisteredVoices = "Only registered voices"
    override val extraSamples = "Extra samples with music and echo"
    override val extraSamplesInfo =
        "When a voice is registered, the app also makes copies of the recording with music mixed in and with room echo. With this on, voices are compared with those copies too: you're recognised more often over loud music or from further away, but strangers get through a little more often. Applies to voices registered or given a new sample from now on.\n\nMusic: \"Hackbeat\" and \"Volatile Reaction\" by Kevin MacLeod (incompetech.com), licensed under Creative Commons: By Attribution 4.0 (creativecommons.org/licenses/by/4.0)."
    override val matchStrictness = "Match strictness"
    override val matchStrictnessInfo =
        "How similar a voice must sound to a registered one. Higher lets fewer strangers through, " +
            "but may miss a registered person in a noisy car."
    override val noVoicesYet = "No voices registered yet, so it still reacts to everyone."
    override val outdatedVoices = "Voices recorded with the previous recognition model have to be registered again."
    override fun remove(name: String) = "Remove $name"
    override val registerVoice = "Register a voice"
    override val name = "Name"
    override fun recording(secondsLeft: Int) = "Recording, keep talking… $secondsLeft s"
    override val processing = "Checking the recording…"
    override fun enrollInstructions(seconds: Int) =
        "Press Record and keep talking for the whole recording at your normal pace, as if talking to someone. Only this person should talk. Record from where you'll usually be when using the app."
    override fun sampleCount(count: Int) = if (count == 1) "1 sample" else "$count samples"
    override val addSample = "Add a sample"
    override fun addSampleTitle(name: String) = "Add a sample for $name"
    override val addSampleHint =
        "Record in a different situation than before, for example in the car from your seat, with music playing " +
            "as usual. The app compares a voice with the sample that fits best."
    override val enrollPassageNote =
        "The text below is only there so you know what to say. You don't have to read it word for word, and if you finish early, just keep talking."
    override val enrollPassage =
        "Hi, this is my voice. On Saturday morning we drove north along the quiet coast road, past " +
            "shops, fields and a few sleepy villages. The radio was playing jazz, so we turned it down. How " +
            "far is it to the lake? About sixty kilometres, maybe less if the traffic stays this light. " +
            "Let's stop for coffee and fresh bread first, and then we can decide." +
            "\n\n" +
            "Later that afternoon the wind picked up, and dark clouds rolled in from the sea. We packed the " +
            "blankets, the thermos and the old guitar, and hurried back to the car just before the rain " +
            "began. On the way home we talked about everything we had seen, laughed about the burnt toast at " +
            "breakfast, and promised each other that next summer we would stay for a whole week."
    override val tooQuiet =
        "The recording was too quiet. Hold the device closer and talk at your normal volume."
    override val inconsistentRecording =
        "The recording didn't sound like one person all the way through, maybe someone else or the radio " +
            "got in. Try again somewhere quieter, with only this person talking."
    override val record = "Record"
    override val cancel = "Cancel"
    override val recordingFailed = "Recording failed."
    override fun tooLittleSpeech(seconds: Int) = "Heard only $seconds s of speech; keep talking for the whole recording."
    override val micStopped = "The microphone stopped."
    override val micPermissionNeeded = "Microphone permission needed: press Start listening once to grant it."

    override val car = "Car"
    override val carInfo =
        "Raises the media volume as you drive faster and as the car gets noisier. " +
            "Listening still has to be started first."
    override val off = "Off"
    override val on = "On"
    override val carDescription = "Louder as you drive faster, when you accelerate, and as background noise rises."
    override val speedSteps = "Speed steps"
    override val speedStepsInfo =
        "Extra volume from each speed on. Add as many steps as you like. Needs location permission."
    override fun speedStepTitle(number: Int) = "Step $number"
    override val fromSpeed = "From"
    override val extraVolume = "Extra volume"
    override val addSpeedStep = "Add a step"
    override fun removeSpeedStep(number: Int) = "Remove step $number"
    override val smoothSteps = "Smooth between steps"
    override val smoothStepsInfo =
        "Blends the volume between steps, so it rises little by little as you speed up instead of jumping at each step. Off: the volume changes at each step, and a step is only removed again once you are 5 km/h below it."
    override val transitionTime = "Transition time"
    override val transitionTimeInfo =
        "How long car mode takes to move the volume to a new level, so changes feel gradual. Lowering for speech stays instant."
    override val accelerationBoost = "When accelerating, up to"
    override val accelerationBoostInfo =
        "Extra volume while you speed up, more the harder you accelerate (the engine gets louder), easing back once you cruise. 0% turns it off. Needs location permission."
    override val noiseBoostUpTo = "Noise boost, up to"
    override val noiseBoostInfo =
        "Extra volume as the car gets noisier. The limit also keeps the app from hearing the car's own speakers and turning the volume up further and further. The notification shows how noisy it is."
    override val carModeAppliesNextStart = "Turning car mode on or off applies the next time listening starts."

    override val setupGuide = "Setup guide"
    override val setupGuideIntro =
        "Set up in a few minutes. The values below work well for most people; you can fine-tune them later."
    override val useRecommendedValues = "Use recommended values"
    override val close = "Close"
    override fun setupSteps(phone: Boolean) = listOfNotNull(
        "Pick a mode" to "\"Duck\" makes the music quieter while someone talks (35% works well). \"Noise\" makes it louder so you can keep listening (80%). \"Podcast\" pauses, then plays again from 5 s back.",
        "Start listening" to "Press \"Start listening\" and allow the microphone. Say a few sentences and check that the volume changes." + (if (phone) " The Quick Settings tile starts it without opening the app." else ""),
        "Register voices (optional)" to "If the radio, podcasts or other people set it off, register the people who should count and turn on \"Only registered voices\". Record with the phone where it usually is. Then add a second sample in the car with music playing, so you're recognised there too.",
        "Check the match" to "While a registered person talks, the status shows how well the voice matches, for example 0.59. Leave \"Match strictness\" as it is. If you aren't recognised, set it a little below the lowest number you see; if other people get through, raise it a little.",
        ("Car" to "Car mode turns the volume up as you drive faster (from 50, 70 and 90 km/h) and as the car gets noisier (up to +20%). Leave the microphone on \"Assistant\" unless the app doesn't hear you well.").takeIf { phone },
        "Fine-tuning" to "Under \"Advanced\": Speech hold 1.5 s, Allowed pause 1.2 s, Speech threshold 0.25. Reacts to short noises? Raise Speech hold. Volume comes back between sentences? Raise Allowed pause. Misses quiet voices? Lower Speech threshold a little."
    )

    override val ok = "OK"
    override fun about(topic: String) = "About $topic"
    override val language = "Language"
    override val microphone = "Microphone"
    override val microphoneInfo =
        "Which of the phone's microphone settings to listen with. \"Assistant\" picks up voices from a distance and from any direction, like a voice assistant, and suits most people. Try the others if the app doesn't hear you well. Takes effect the next time listening starts. After changing it, register the voices again."
    override val micAssistant = "Assistant"
    override val micStandard = "Standard"
    override val micVideo = "Video"
    override val micRaw = "Raw"
    override val systemLanguage = "System language"
    override val addTile = "Add Quick Settings tile"

    override val display = "Display"
    override val floatingStatus = "Floating status"
    override val floatingStatusInfo =
        "While listening and the app is not open, a small card shows what the app is doing, above other apps such as navigation. " +
            "Drag it to move it, tap it to open the app."
    override val notificationListening = "Listening for speech"
    override fun notificationVoiceHeard(who: String) = "Voice heard$who"
    override fun notificationLowered(who: String) = "Speech detected$who: volume lowered"
    override fun notificationRaised(who: String) = "Speech detected$who: volume raised"
    override fun notificationPaused(who: String) = "Speech detected$who: paused"
    override fun notificationCar(kmh: Int, noiseDb: Int, boostPercent: Int) =
        "Car: $kmh km/h, noise $noiseDb dB, +$boostPercent%"
    override val notificationChannelDescription = "Background microphone monitoring"
    override val unknownVoice = "unknown"
}
