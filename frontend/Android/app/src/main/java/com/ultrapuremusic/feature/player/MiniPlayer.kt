package com.ultrapuremusic.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.GlassmorphismCard
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

/** Glassmorphism mini-player — sits above the bottom nav bar when a song is playing. */
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.playerState.collectAsState()
    val song  = state.currentSong ?: return

    GlassmorphismCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            // ── Progress bar at the very top ─────────────────────────────────
            LinearProgressIndicator(
                progress          = { state.progress.coerceIn(0f, 1f) },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color             = AccentPrimary,
                trackColor        = Color.White.copy(alpha = 0.12f),
            )

            // ── Content row ──────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album thumbnail
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model              = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.matchParentSize(),
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = song.title,
                        style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color    = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text     = song.artist,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Play / Pause
                IconButton(
                    onClick  = viewModel::togglePlayPause,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector        = if (state.isPlaying) Icons.Default.Pause
                                             else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Tạm dừng" else "Phát",
                        tint               = TextPrimary,
                        modifier           = Modifier.size(24.dp),
                    )
                }

                // Skip next
                IconButton(
                    onClick  = viewModel::skipNext,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Default.SkipNext,
                        contentDescription = "Tiếp theo",
                        tint               = TextSecondary,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
