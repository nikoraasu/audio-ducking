package io.github.nikoraasu.audioducking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/** Flag button for the top corner; [setting] is a [Language.code] or "" for the system language. */
@Composable
fun LanguagePicker(setting: String, onChange: (String) -> Unit) {
    val strings = LocalStrings.current
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Flag(resolveLanguage(setting), Modifier.size(width = 30.dp, height = 20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val options = listOf("" to strings.systemLanguage) + Language.entries.map { it.code to it.nativeName }
            for ((code, label) in options) {
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Flag(resolveLanguage(code), Modifier.size(width = 24.dp, height = 16.dp)) },
                    trailingIcon = {
                        if (code == setting) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        open = false
                        onChange(code)
                    }
                )
            }
        }
    }
}

/** Simplified flags drawn as shapes: emoji flags don't render on Windows. */
@Composable
fun Flag(language: Language, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Canvas(
        modifier
            .clip(shape)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        when (language) {
            Language.PL -> stripes(Color.White to 1f, Color(0xFFDC143C) to 1f)
            Language.DE -> stripes(Color.Black to 1f, Color(0xFFDD0000) to 1f, Color(0xFFFFCE00) to 1f)
            Language.ES -> stripes(Color(0xFFAA151B) to 1f, Color(0xFFF1BF00) to 2f, Color(0xFFAA151B) to 1f)
            Language.EN -> unionJack()
        }
    }
}

/** Horizontal stripes, top to bottom, with relative heights. */
private fun DrawScope.stripes(vararg bands: Pair<Color, Float>) {
    val total = bands.sumOf { it.second.toDouble() }.toFloat()
    var top = 0f
    for ((color, weight) in bands) {
        val height = size.height * weight / total
        drawRect(color, topLeft = Offset(0f, top), size = Size(size.width, height))
        top += height
    }
}

private fun DrawScope.unionJack() {
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    val w = size.width
    val h = size.height
    drawRect(blue)
    // Diagonals: white saltire with a thinner red one on top.
    for ((color, stroke) in listOf(Color.White to h * 0.2f, red to h * 0.07f)) {
        drawLine(color, Offset(0f, 0f), Offset(w, h), strokeWidth = stroke)
        drawLine(color, Offset(w, 0f), Offset(0f, h), strokeWidth = stroke)
    }
    // Upright cross: white, then red.
    for ((color, stroke) in listOf(Color.White to h / 3, red to h / 5)) {
        drawRect(color, topLeft = Offset(0f, (h - stroke) / 2), size = Size(w, stroke))
        drawRect(color, topLeft = Offset((w - stroke) / 2, 0f), size = Size(stroke, h))
    }
}
