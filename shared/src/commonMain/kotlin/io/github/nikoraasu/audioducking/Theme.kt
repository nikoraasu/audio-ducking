package io.github.nikoraasu.audioducking

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Monochrome like the duck icon, with a vivid duck-yellow accent for controls and "the app is acting now".
// Light theme keeps text buttons black: yellow text on white is unreadable.
val DuckYellow = Color(0xFFFFC800)
val OnDuckYellow = Color(0xFF1C1400)

private val LightColors = lightColorScheme(
    primary = Color(0xFF151515),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFFFE07A),
    onSecondaryContainer = OnDuckYellow,
    tertiaryContainer = DuckYellow,
    onTertiaryContainer = OnDuckYellow,
    background = Color(0xFFF4F3F0),
    onBackground = Color(0xFF151515),
    surface = Color.White,
    onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFE6E4DF),
    onSurfaceVariant = Color(0xFF55534E),
    surfaceContainerHighest = Color(0xFFE6E4DF),
    inverseSurface = Color(0xFF151515),
    inverseOnSurface = Color.White,
    outline = Color(0xFFB9B6AF),
    outlineVariant = Color(0xFFDAD8D2)
)

private val DarkColors = darkColorScheme(
    primary = DuckYellow,
    onPrimary = OnDuckYellow,
    secondaryContainer = Color(0xFF5C4600),
    onSecondaryContainer = Color(0xFFFFE07A),
    tertiaryContainer = DuckYellow,
    onTertiaryContainer = OnDuckYellow,
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF191919),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFB5B3AE),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF151515),
    outline = Color(0xFF5E5C58),
    outlineVariant = Color(0xFF333333)
)

@Composable
fun AudioDuckingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

@Composable
fun duckSliderColors() = SliderDefaults.colors(
    thumbColor = DuckYellow,
    activeTrackColor = DuckYellow,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
)

@Composable
fun duckSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = OnDuckYellow,
    checkedTrackColor = DuckYellow,
    checkedBorderColor = DuckYellow
)

/** A titled group of settings. */
@Composable
fun SectionCard(title: String, info: String? = null, content: @Composable ColumnScope.() -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                )
                if (info != null) InfoButton(title) { showInfo = true }
            }
            Column(modifier = Modifier.padding(end = 12.dp), content = content)
        }
    }
    if (showInfo && info != null) InfoDialog(title, info) { showInfo = false }
}

/** Label, current value and slider; the caller snaps/stores the value in [onChange]. */
@Composable
fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    info: String? = null,
    onChange: (Float) -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (info != null) InfoButton(label) { showInfo = true }
    }
    Slider(value = value, onValueChange = onChange, valueRange = range, colors = duckSliderColors())
    if (showInfo && info != null) InfoDialog(label, info) { showInfo = false }
}

@Composable
internal fun InfoButton(about: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = LocalStrings.current.about(about),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(LocalStrings.current.ok) } }
    )
}
