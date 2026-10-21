package io.github.nikoraasu.audioducking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Records [ENROLL_SECONDS] from the mic and returns the embedding (on any thread). */
typealias VoiceEnroller = (onDone: (Result<VoiceSample>) -> Unit) -> Unit

@Composable
fun VoiceSettings(settings: Settings, enroll: VoiceEnroller) {
    var voices by remember { mutableStateOf(settings.voices) }
    var filter by remember { mutableStateOf(settings.voiceFilter) }
    var extraSamples by remember { mutableStateOf(settings.voiceExtraSamples) }
    var matchThreshold by remember { mutableStateOf(settings.voiceMatchThreshold) }
    var showEnroll by remember { mutableStateOf(false) }
    var addingTo by remember { mutableStateOf<Voice?>(null) } // null = registering a new person
    val strings = LocalStrings.current

    SectionCard(title = strings.voices, info = strings.voicesInfo) {
        if (settings.hasOutdatedVoices) {
            Text(strings.outdatedVoices, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        SwitchRow(strings.onlyRegisteredVoices, filter) {
            filter = it
            settings.voiceFilter = it
        }

        if (filter) {
            SwitchRow(strings.extraSamples, extraSamples, info = strings.extraSamplesInfo) {
                extraSamples = it
                settings.voiceExtraSamples = it
            }
            SettingSlider(
                label = strings.matchStrictness,
                valueText = strings.decimal(matchThreshold),
                value = matchThreshold,
                range = 0.2f..0.9f,
                info = strings.matchStrictnessInfo
            ) {
                matchThreshold = ((it * 100f).roundToInt() / 100f).coerceIn(0.2f, 0.9f)
                settings.voiceMatchThreshold = matchThreshold
            }
            if (voices.isEmpty()) {
                Text(
                    strings.noVoicesYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        for (voice in voices) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(voice.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        strings.sampleCount(voice.samples.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (voice.samples.size < MAX_VOICE_SAMPLES) {
                    IconButton(onClick = {
                        addingTo = voice
                        showEnroll = true
                    }) { Icon(Icons.Filled.Add, contentDescription = strings.addSample) }
                }
                IconButton(onClick = {
                    voices = voices - voice
                    settings.voices = voices
                }) { Icon(Icons.Filled.Close, contentDescription = strings.remove(voice.name)) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { addingTo = null; showEnroll = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.registerVoice)
        }
    }

    if (showEnroll) {
        val target = addingTo
        EnrollDialog(
            enroll = { onDone -> enroll(onDone) },
            addingTo = target,
            onRecorded = { name, sample ->
                voices = if (target != null) {
                    voices.map { if (it.name == target.name) it.withSample(sample) else it }
                } else {
                    val voice = Voice(name, listOf(sample))
                    voices.filter { it.name != voice.name } + voice // same name = re-record
                }
                settings.voices = voices
            },
            onDismiss = { showEnroll = false }
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, info: String? = null, onChange: (Boolean) -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (info != null) InfoButton(label) { showInfo = true }
        Switch(checked = checked, onCheckedChange = onChange, colors = duckSwitchColors())
    }
    if (showInfo && info != null) InfoDialog(label, info) { showInfo = false }
}

@Composable
private fun EnrollDialog(
    enroll: (onDone: (Result<VoiceSample>) -> Unit) -> Unit,
    addingTo: Voice?,
    onRecorded: (name: String, sample: VoiceSample) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(addingTo?.name ?: "") }
    var recording by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(ENROLL_SECONDS) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    LaunchedEffect(recording) {
        while (recording && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    AlertDialog(
        onDismissRequest = { if (!recording) onDismiss() },
        title = { Text(if (addingTo != null) strings.addSampleTitle(addingTo.name) else strings.registerVoice) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (addingTo != null) {
                    Text(strings.addSampleHint, style = MaterialTheme.typography.bodyMedium)
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.name) },
                        singleLine = true,
                        enabled = !recording,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (recording) {
                    LinearProgressIndicator(
                        progress = { 1f - secondsLeft.toFloat() / ENROLL_SECONDS },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                Text(
                    when {
                        recording && secondsLeft > 0 -> strings.recording(secondsLeft)
                        recording -> strings.processing
                        error != null -> error!!
                        else -> strings.enrollInstructions(ENROLL_SECONDS)
                    },
                    color = if (error != null && !recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    strings.enrollPassageNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        strings.enrollPassage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !recording,
                onClick = {
                    recording = true
                    error = null
                    secondsLeft = ENROLL_SECONDS
                    enroll { result ->
                        scope.launch { // back on the UI thread
                            recording = false
                            result
                                .onSuccess {
                                    onRecorded(name, it)
                                    onDismiss()
                                }
                                .onFailure { error = strings.enrollError(it) }
                        }
                    }
                }
            ) { Text(strings.record) }
        },
        dismissButton = {
            TextButton(enabled = !recording, onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
