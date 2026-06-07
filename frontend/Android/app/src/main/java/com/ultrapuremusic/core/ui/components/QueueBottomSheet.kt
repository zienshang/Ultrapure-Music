package com.ultrapuremusic.core.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.Surface
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Song>,
    currentIndex: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClearQueue: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = null,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "Hàng đợi (${queue.size})",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color    = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (queue.isNotEmpty()) {
                TextButton(onClick = onClearQueue) {
                    Text(
                        text  = "Xoá tất cả",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPrimary,
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint               = TextSecondary,
                )
            }
        }

        if (queue.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "Hàng đợi trống",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(currentIndex) {
                // Scroll so the currently playing item is visible
                if (currentIndex >= 0 && currentIndex < queue.size) {
                    listState.animateScrollToItem(currentIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) AccentSubtle else CardBackground.copy(alpha = 0f))
                            .clickable { onJumpTo(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Drag handle (visual only)
                        Icon(
                            imageVector        = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint               = GlassBorder,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))

                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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

                        // Title + Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text      = song.title,
                                style     = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color     = if (isCurrent) AccentPrimary else TextPrimary,
                                maxLines  = 1,
                                overflow  = TextOverflow.Ellipsis,
                            )
                            Text(
                                text     = song.artist,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Playing indicator badge for current song
                        if (isCurrent) {
                            Text(
                                text  = "▶",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }

                        // Remove button
                        IconButton(
                            onClick  = { onRemove(index) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Xoá khỏi hàng đợi",
                                tint               = TextSecondary,
                                modifier           = Modifier.size(16.dp),
                            )
                        }
                    }

                    if (index < queue.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 80.dp)
                                .background(GlassBorder),
                        )
                    }
                }
            }
        }
    }
}
