package io.github.nikoraasu.audioducking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun AudioDuckingScreen(
    settings: Settings,
    onStart: () -> Unit,
    onStop: () -> Unit,
    enrollVoice: VoiceEnroller,
    onAddTile: (() -> Unit)? = null,
    platformSettings: @Composable () -> Unit = {},
    podcastExtra: @Composable () -> Unit = {},
    platformAdvanced: @Composable () -> Unit = {},
    isPhone: Boolean = false
) {
    val status by Monitor.status.collectAsState()
    var language by remember { mutableStateOf(settings.language) }
    var showGuide by remember { mutableStateOf(!settings.setupGuideSeen) }
    var settingsGeneration by remember { mutableStateOf(0) } // bumped to redraw the sections after a reset

    CompositionLocalProvider(LocalStrings provides stringsFor(resolveLanguage(language))) {
        val strings = LocalStrings.current
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .statusBarsPadding() // outside the scroll, so content never slides under the status bar
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(DuckIcon, contentDescription = null, tint = DuckYellow, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Audio Ducking",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp).weight(1f)
                        )
                        LanguagePicker(language) {
                            language = it
                            settings.language = it
                        }
                    }

                    StatusCard(status, onToggle = if (status.running) onStop else onStart)

                    OutlinedButton(onClick = { showGuide = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.setupGuide)
                    }

                    key(settingsGeneration) {
                        ModeSection(settings, podcastExtra)

                        VoiceSettings(settings, enrollVoice)

                        platformSettings()

                        AdvancedSection(settings, platformAdvanced)
                    }

                    if (showGuide) {
                        val close = {
                            showGuide = false
                            settings.setupGuideSeen = true
                        }
                        SetupGuideDialog(
                            isPhone = isPhone,
                            onUseRecommended = {
                                settings.useRecommendedValues()
                                settingsGeneration++
                                close()
                            },
                            onDismiss = close
                        )
                    }

                    if (onAddTile != null) {
                        TextButton(onClick = onAddTile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(strings.addTile)
                        }
                    }
                }
            }
        }
    }
}

/** Step-by-step setup with values that work for most people, and a button to apply them. */
@Composable
private fun SetupGuideDialog(isPhone: Boolean, onUseRecommended: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.setupGuide) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.setupGuideIntro, style = MaterialTheme.typography.bodyMedium)
                strings.setupSteps(isPhone).forEachIndexed { i, (title, text) ->
                    Column {
                        Text("${i + 1}. $title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        dismissButton = { TextButton(onClick = onUseRecommended) { Text(strings.useRecommendedValues) } }
    )
}

private class Look(val title: String, val detail: String, val background: Color, val content: Color)

@Composable
private fun statusLook(status: MonitorStatus): Look {
    val colors = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    // With voice filtering, show the match score so the strictness can be set (see the setup guide).
    val who = strings.isTalking(status.speaker).let { talking ->
        status.matchScore?.takeIf { status.speaker != null }?.let { strings.withMatch(talking, strings.decimal(it)) } ?: talking
    }
    return when {
        status.error != null -> Look(
            strings.stopped,
            strings.error(status.error),
            colors.errorContainer,
            colors.onErrorContainer
        )
        !status.running -> Look(strings.notListening, strings.notListeningDetail, colors.surfaceVariant, colors.onSurfaceVariant)
        status.effect?.mode == Mode.DUCK ->
            Look(strings.ducking, strings.volumeLowered(who), colors.tertiaryContainer, colors.onTertiaryContainer)
        status.effect?.mode == Mode.BOOST ->
            Look(strings.boosting, strings.volumeRaised(who), colors.tertiaryContainer, colors.onTertiaryContainer)
        status.effect?.mode == Mode.PODCAST ->
            Look(strings.paused, strings.playbackPaused(who), colors.tertiaryContainer, colors.onTertiaryContainer)
        else -> Look(
            strings.listening,
            if (status.matchScore != null && status.speaker == null) strings.ignoringVoice(strings.decimal(status.matchScore))
            else strings.waitingForSpeech,
            colors.inverseSurface,
            colors.inverseOnSurface
        )
    }
}

/** Compact status for the floating overlay; [modifier] carries the drag and tap handling. */
@Composable
fun MiniStatus(status: MonitorStatus, modifier: Modifier = Modifier) {
    val look = statusLook(status)
    val background by animateColorAsState(look.background)
    val content by animateColorAsState(look.content)
    Surface(
        modifier = modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(24.dp),
        color = background,
        contentColor = content,
        shadowElevation = 6.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(DuckIcon, contentDescription = null, modifier = Modifier.size(32.dp), tint = content)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(look.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    look.detail,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                status.carBoost?.let {
                    Text(LocalStrings.current.carBoost((it * 100).roundToInt()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Live state of the listener, with the one Start/Stop toggle. */
@Composable
private fun StatusCard(status: MonitorStatus, onToggle: () -> Unit) {
    val strings = LocalStrings.current
    val look = statusLook(status)
    val background by animateColorAsState(look.background)
    val content by animateColorAsState(look.content)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = background, contentColor = content)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(DuckIcon, contentDescription = null, modifier = Modifier.size(64.dp), tint = content)
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(look.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(look.detail, style = MaterialTheme.typography.bodyMedium)
                    status.carBoost?.let {
                        Text(
                            strings.carBoost((it * 100).roundToInt()),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                // Stopped: the main call to action. Otherwise inverted card colours, so it reads as part of the card.
                colors = if (status.running) {
                    ButtonDefaults.buttonColors(containerColor = content, contentColor = background)
                } else {
                    ButtonDefaults.buttonColors(containerColor = DuckYellow, contentColor = OnDuckYellow)
                }
            ) {
                Text(if (status.running) strings.stopListening else strings.startListening, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun ModeSection(settings: Settings, podcastExtra: @Composable () -> Unit) {
    var mode by remember { mutableStateOf(settings.mode) }
    var duckPercent by remember { mutableStateOf(settings.duckPercent) }
    var boostPercent by remember { mutableStateOf(settings.boostPercent) }
    var rewindSeconds by remember { mutableStateOf(settings.rewindSeconds.toFloat()) }
    val strings = LocalStrings.current

    SectionCard(strings.mode) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Mode.entries.forEachIndexed { i, entry ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { mode = entry; settings.mode = entry },
                    shape = SegmentedButtonDefaults.itemShape(i, Mode.entries.size)
                ) {
                    Text(
                        when (entry) {
                            Mode.DUCK -> strings.duck
                            Mode.BOOST -> strings.noise
                            Mode.PODCAST -> strings.podcast
                        }
                    )
                }
            }
        }
        Text(
            text = when (mode) {
                Mode.DUCK -> strings.duckDescription
                Mode.BOOST -> strings.noiseDescription
                Mode.PODCAST -> strings.podcastDescription
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        when (mode) {
            Mode.DUCK -> SettingSlider(
                label = strings.volumeWhileTalking,
                valueText = "${(duckPercent * 100).roundToInt()}%",
                value = duckPercent,
                range = 0.05f..1f,
                info = strings.duckLevelInfo
            ) {
                duckPercent = ((it * 100).roundToInt() / 100f).coerceIn(0.05f, 1f)
                settings.duckPercent = duckPercent
            }
            Mode.BOOST -> SettingSlider(
                label = strings.volumeWhileTalking,
                valueText = "${(boostPercent * 100).roundToInt()}%",
                value = boostPercent,
                range = 0.05f..1f,
                info = strings.boostLevelInfo
            ) {
                boostPercent = ((it * 100).roundToInt() / 100f).coerceIn(0.05f, 1f)
                settings.boostPercent = boostPercent
            }
            Mode.PODCAST -> {
                SettingSlider(
                    label = strings.rewind,
                    valueText = strings.seconds(rewindSeconds * 1000),
                    value = rewindSeconds,
                    range = 0f..15f,
                    info = strings.rewindInfo
                ) {
                    rewindSeconds = it.roundToInt().toFloat()
                    settings.rewindSeconds = rewindSeconds.toLong()
                }
                podcastExtra()
            }
        }
    }
}

/** Detection fine-tuning, folded away by default. */
@Composable
private fun AdvancedSection(settings: Settings, platformAdvanced: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var speechHoldMs by remember { mutableStateOf(settings.speechHoldMs.toFloat()) }
    var allowedPauseMs by remember { mutableStateOf(settings.allowedPauseMs.toFloat()) }
    var speechThreshold by remember { mutableStateOf(settings.speechThreshold) }
    val strings = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.advanced,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) strings.collapse else strings.expand
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                SettingSlider(
                    label = strings.speechHold,
                    valueText = strings.seconds(speechHoldMs),
                    value = speechHoldMs,
                    range = 500f..10000f,
                    info = strings.speechHoldInfo
                ) {
                    speechHoldMs = ((it / 100).roundToInt() * 100f).coerceIn(500f, 10000f)
                    settings.speechHoldMs = speechHoldMs.toLong()
                }
                SettingSlider(
                    label = strings.allowedPause,
                    valueText = strings.seconds(allowedPauseMs),
                    value = allowedPauseMs,
                    range = 100f..6000f,
                    info = strings.allowedPauseInfo
                ) {
                    allowedPauseMs = ((it / 100).roundToInt() * 100f).coerceIn(100f, 6000f)
                    settings.allowedPauseMs = allowedPauseMs.toLong()
                }
                SettingSlider(
                    label = strings.speechThreshold,
                    valueText = strings.decimal(speechThreshold),
                    value = speechThreshold,
                    range = 0.05f..0.8f,
                    info = strings.speechThresholdInfo
                ) {
                    speechThreshold = ((it * 100).roundToInt() / 100f).coerceIn(0.05f, 0.8f)
                    settings.speechThreshold = speechThreshold
                }
                platformAdvanced()
            }
        }
    }
}
