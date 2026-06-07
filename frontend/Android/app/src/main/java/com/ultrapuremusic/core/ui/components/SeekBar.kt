package com.ultrapuremusic.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.core.util.toFormattedDuration

@Composable
fun MusicSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableFloatStateOf(-1f) }
    val displayPosition = if (isDragging >= 0f) (isDragging * durationMs).toLong() else currentPositionMs
    val progress = if (durationMs > 0) displayPosition.toFloat() / durationMs.toFloat() else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { isDragging = it },
            onValueChangeFinished = {
                if (isDragging >= 0f) {
                    onSeek((isDragging * durationMs).toLong())
                    isDragging = -1f
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                text = displayPosition.toFormattedDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = durationMs.toFormattedDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
