package io.github.nikoraasu.audioducking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CarModeSettings(settings: Settings) {
    var carMode by remember { mutableStateOf(settings.carMode) }
    var steps by remember { mutableStateOf(settings.speedSteps) }
    var showSteps by remember { mutableStateOf(false) }
    var noiseBoostMax by remember { mutableStateOf(settings.noiseBoostMaxPercent) }
    var smooth by remember { mutableStateOf(settings.carSmoothSteps) }
    var transitionSeconds by remember { mutableStateOf(settings.carTransitionSeconds) }
    var accelerationMax by remember { mutableStateOf(settings.accelerationBoostMax) }
    val strings = LocalStrings.current

    SectionCard(title = strings.car, info = strings.carInfo) {
        val options = listOf(CarMode.OFF to strings.off, CarMode.ON to strings.on)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = carMode == value,
                    onClick = { carMode = value; settings.carMode = value },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size)
                ) { Text(label, maxLines = 1) }
            }
        }

        Text(
            strings.carModeAppliesNextStart,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (carMode != CarMode.OFF) {
            Text(
                strings.carDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showSteps = !showSteps }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.speedSteps, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    if (steps.size <= 3) steps.joinToString(" · ") { "${it.kmh.roundToInt()}: +${(it.boost * 100).roundToInt()}%" }
                    else "${steps.first().kmh.roundToInt()}–${steps.last().kmh.roundToInt()} km/h (${steps.size})",
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    if (showSteps) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showSteps) strings.collapse else strings.expand
                )
            }
            if (showSteps) {
                Text(
                    strings.speedStepsInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                fun save(changed: List<SpeedStep>) {
                    steps = changed
                    settings.speedSteps = changed
                }
                steps.forEachIndexed { i, step ->
                    fun update(changed: SpeedStep) = save(steps.toMutableList().also { it[i] = changed })
                    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.speedStepTitle(i + 1),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (steps.size > 1) {
                            IconButton(onClick = { save(steps.filterIndexed { j, _ -> j != i }) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = strings.removeSpeedStep(i + 1), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    // Each step stays at least 5 km/h above the previous one and below the next.
                    val minKmh = (steps.getOrNull(i - 1)?.kmh ?: 15f) + 5f
                    val maxKmh = (steps.getOrNull(i + 1)?.kmh ?: 165f) - 5f
                    SettingSlider(
                        label = strings.fromSpeed,
                        valueText = "${step.kmh.roundToInt()} km/h",
                        value = step.kmh,
                        range = 20f..160f
                    ) { update(step.copy(kmh = ((it / 5).roundToInt() * 5f).coerceIn(minKmh, maxKmh))) }
                    SettingSlider(
                        label = strings.extraVolume,
                        valueText = "+${(step.boost * 100).roundToInt()}%",
                        value = step.boost,
                        range = 0f..0.5f
                    ) { update(step.copy(boost = ((it * 100).roundToInt() / 100f).coerceIn(0f, 0.5f))) }
                }
                // A new step goes above the last one, a little louder (room permitting).
                val last = steps.last()
                if (steps.size < MAX_SPEED_STEPS && last.kmh <= 150f) {
                    TextButton(onClick = {
                        save(steps + SpeedStep((last.kmh + 20f).coerceAtMost(160f), (last.boost + 0.05f).coerceAtMost(0.5f)))
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(strings.addSpeedStep, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            SwitchRow(strings.smoothSteps, smooth, info = strings.smoothStepsInfo) {
                smooth = it
                settings.carSmoothSteps = it
            }
            SettingSlider(
                label = strings.transitionTime,
                valueText = strings.seconds(transitionSeconds * 1000),
                value = transitionSeconds,
                range = 0f..10f,
                info = strings.transitionTimeInfo
            ) {
                transitionSeconds = ((it * 2).roundToInt() / 2f).coerceIn(0f, 10f)
                settings.carTransitionSeconds = transitionSeconds
            }
            SettingSlider(
                label = strings.accelerationBoost,
                valueText = "+${(accelerationMax * 100).roundToInt()}%",
                value = accelerationMax,
                range = 0f..0.3f,
                info = strings.accelerationBoostInfo
            ) {
                accelerationMax = ((it * 100f).roundToInt() / 100f).coerceIn(0f, 0.3f)
                settings.accelerationBoostMax = accelerationMax
            }
            SettingSlider(
                label = strings.noiseBoostUpTo,
                valueText = "+${(noiseBoostMax * 100).roundToInt()}%",
                value = noiseBoostMax,
                range = 0f..0.4f,
                info = strings.noiseBoostInfo
            ) {
                noiseBoostMax = ((it * 100f).roundToInt() / 100f).coerceIn(0f, 0.4f)
                settings.noiseBoostMaxPercent = noiseBoostMax
            }
        }
    }
}

/** Microphone choice, shown inside Advanced. */
@Composable
fun MicrophoneSetting(settings: Settings) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var chosen by remember { mutableStateOf(settings.micSource) }
    var showInfo by remember { mutableStateOf(false) }
    val options = listOfNotNull(
        MicSource.VOICE_RECOGNITION to strings.micAssistant,
        MicSource.MIC to strings.micStandard,
        MicSource.CAMCORDER to strings.micVideo,
        (MicSource.UNPROCESSED to strings.micRaw).takeIf { unprocessedMicSupported(context) }
    )

    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(strings.microphone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { showInfo = true }) {
            Icon(Icons.Outlined.Info, contentDescription = strings.about(strings.microphone), modifier = Modifier.size(20.dp))
        }
    }
    for ((source, label) in options) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { chosen = source.name; settings.micSource = source.name },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = chosen == source.name, onClick = { chosen = source.name; settings.micSource = source.name })
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (showInfo) InfoDialog(strings.microphone, strings.microphoneInfo) { showInfo = false }
}

/** The floating status switch; turning it on asks for "display over other apps" first if needed. */
@Composable
fun DisplaySettings(settings: Settings, overlayAllowed: Boolean, requestOverlay: () -> Unit) {
    var floating by remember { mutableStateOf(settings.floatingStatus) }
    val strings = LocalStrings.current
    SectionCard(strings.display) {
        SwitchRow(strings.floatingStatus, floating && overlayAllowed, info = strings.floatingStatusInfo) {
            floating = it
            settings.floatingStatus = it
            if (it && !overlayAllowed) requestOverlay()
        }
    }
}

/** Podcast mode: offers notification access, without which playback can't be rewound. */
@Composable
fun MediaAccessRow(granted: Boolean, onAllow: () -> Unit) {
    if (granted) return
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(
            strings.mediaAccessNeeded,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAllow) { Text(strings.allowMediaAccess) }
    }
}
