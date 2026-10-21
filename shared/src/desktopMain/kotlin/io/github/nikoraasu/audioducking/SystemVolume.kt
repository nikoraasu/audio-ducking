package io.github.nikoraasu.audioducking

import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.PointerByReference

/** Master output volume of the default playback device, 0.0..1.0. */
interface SystemVolume {
    fun get(): Float
    fun set(level: Float)

    companion object {
        fun forCurrentOs(): SystemVolume = when {
            System.getProperty("os.name").startsWith("Windows") -> WindowsVolume()
            PipeWireVolume.available() -> PipeWireVolume()
            else -> PulseVolume()
        }
    }
}

/** PipeWire, via WirePlumber's wpctl. The default on current Linux distros. */
private class PipeWireVolume : SystemVolume {
    override fun get(): Float {
        // "Volume: 0.70" or "Volume: 0.70 [MUTED]"
        val out = exec("wpctl", "get-volume", DEFAULT_SINK)
        return Regex("""Volume:\s*([\d.]+)""").find(out)?.groupValues?.get(1)?.toFloat()
            ?: error("Unexpected wpctl output: $out")
    }

    override fun set(level: Float) {
        exec("wpctl", "set-volume", DEFAULT_SINK, "%.2f".format(java.util.Locale.ROOT, level.coerceAtLeast(0f)))
    }

    companion object {
        private const val DEFAULT_SINK = "@DEFAULT_AUDIO_SINK@"

        fun available(): Boolean = runCatching { exec("wpctl", "get-volume", DEFAULT_SINK) }.isSuccess
    }
}

/** Plain PulseAudio, via pactl. Fallback for systems without PipeWire. */
private class PulseVolume : SystemVolume {
    override fun get(): Float {
        val out = exec("pactl", "get-sink-volume", "@DEFAULT_SINK@")
        val percent = Regex("""(\d+)%""").find(out)?.groupValues?.get(1)
            ?: error("Unexpected pactl output: $out")
        return percent.toInt() / 100f
    }

    override fun set(level: Float) {
        exec("pactl", "set-sink-volume", "@DEFAULT_SINK@", "${(level.coerceAtLeast(0f) * 100).toInt()}%")
    }
}

private fun exec(vararg command: String): String {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    val out = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "${command.joinToString(" ")} failed: $out" }
    return out
}

/** Windows Core Audio IAudioEndpointVolume. */
private class WindowsVolume : SystemVolume {
    override fun get(): Float = withEndpoint(IID_IAUDIO_ENDPOINT_VOLUME) { volume ->
        val level = FloatByReference()
        hr(call(volume, GET_MASTER_VOLUME_LEVEL_SCALAR, level))
        level.value
    }

    override fun set(level: Float) = withEndpoint(IID_IAUDIO_ENDPOINT_VOLUME) { volume ->
        hr(call(volume, SET_MASTER_VOLUME_LEVEL_SCALAR, level.coerceIn(0f, 1f), null))
    }
}

/** Loudest sample currently going to the default playback device, 0.0..1.0 (Windows). */
fun outputPeak(): Float = withEndpoint(IID_IAUDIO_METER_INFORMATION) { meter ->
    val peak = FloatByReference()
    hr(call(meter, GET_PEAK_VALUE, peak))
    peak.value
}

// Windows Core Audio, called through raw COM vtables.
// Looked up per call so a changed default device (e.g. headphones plugged in) is picked up.
private fun <T> withEndpoint(iid: GUID, block: (Pointer) -> T): T {
    Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED) // S_FALSE if already initialised
    val enumerator = PointerByReference()
    hr(Ole32.INSTANCE.CoCreateInstance(CLSID_MM_DEVICE_ENUMERATOR, null, WTypes.CLSCTX_ALL, IID_IMM_DEVICE_ENUMERATOR, enumerator).toInt())
    try {
        val device = PointerByReference()
        hr(call(enumerator.value, GET_DEFAULT_AUDIO_ENDPOINT, E_RENDER, E_MULTIMEDIA, device))
        try {
            val endpoint = PointerByReference()
            hr(call(device.value, ACTIVATE, iid, WTypes.CLSCTX_ALL, null, endpoint))
            try {
                return block(endpoint.value)
            } finally {
                call(endpoint.value, RELEASE)
            }
        } finally {
            call(device.value, RELEASE)
        }
    } finally {
        call(enumerator.value, RELEASE)
    }
}

private fun call(obj: Pointer, vtableIndex: Int, vararg args: Any?): Int {
    val vtable = obj.getPointer(0)
    val fn = Function.getFunction(vtable.getPointer(vtableIndex.toLong() * Native.POINTER_SIZE))
    return fn.invokeInt(arrayOf(obj, *args))
}

private fun hr(result: Int) = check(result >= 0) { "COM call failed: 0x%08X".format(result) }

private val CLSID_MM_DEVICE_ENUMERATOR = GUID("BCDE0395-E52F-467C-8E3D-C4579291692E")
private val IID_IMM_DEVICE_ENUMERATOR = GUID("A95664D2-9614-4F35-A746-DE8DB63617E6")
private val IID_IAUDIO_ENDPOINT_VOLUME = GUID("5CDF2C82-841E-4546-9722-0CF74078229A")
private val IID_IAUDIO_METER_INFORMATION = GUID("C02216F6-8C67-4B5B-9D00-D008E73E0064")

private const val RELEASE = 2
private const val GET_DEFAULT_AUDIO_ENDPOINT = 4 // IMMDeviceEnumerator
private const val ACTIVATE = 3 // IMMDevice
private const val SET_MASTER_VOLUME_LEVEL_SCALAR = 7 // IAudioEndpointVolume
private const val GET_MASTER_VOLUME_LEVEL_SCALAR = 9
private const val GET_PEAK_VALUE = 3 // IAudioMeterInformation

private const val E_RENDER = 0
private const val E_MULTIMEDIA = 1
