package com.ultrapuremusic.core.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.Surface
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    sheetState: SheetState,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onDismiss: () -> Unit,
    onPlayNow: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onDeleteDownload: (Song) -> Unit,
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = null,
    ) {
        // ── Song info header ─────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model              = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.matchParentSize(),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = song.title,
                    style     = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color     = TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(color = GlassBorder)

        // ── Options ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
        ) {
            OptionRow(
                icon    = Icons.Default.PlayArrow,
                label   = "Phát ngay",
                onClick = { onPlayNow(song); onDismiss() },
            )
            OptionRow(
                icon    = Icons.Default.SkipNext,
                label   = "Phát tiếp theo",
                onClick = { onPlayNext(song); onDismiss() },
            )
            OptionRow(
                icon    = Icons.Default.PlaylistAdd,
                label   = "Thêm vào hàng đợi",
                onClick = { onAddToQueue(song); onDismiss() },
            )

            HorizontalDivider(
                color    = GlassBorder,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )

            OptionRow(
                icon     = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                iconTint = if (isFavorite) AccentPrimary else TextSecondary,
                label    = if (isFavorite) "Bỏ yêu thích" else "Yêu thích",
                onClick  = { onToggleFavorite(song); onDismiss() },
            )
            OptionRow(
                icon    = Icons.Default.PlaylistAdd,
                label   = "Thêm vào playlist",
                onClick = { onAddToPlaylist(song); onDismiss() },
            )

            HorizontalDivider(
                color    = GlassBorder,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )

            if (isDownloaded) {
                OptionRow(
                    icon     = Icons.Default.CheckCircle,
                    iconTint = AccentPrimary,
                    label    = "Đã tải · Xoá khỏi máy",
                    onClick  = { onDeleteDownload(song); onDismiss() },
                )
            } else {
                OptionRow(
                    icon    = Icons.Default.Download,
                    label   = "Tải xuống",
                    onClick = { onDownload(song); onDismiss() },
                )
            }
            OptionRow(
                icon    = Icons.Default.Share,
                label   = "Chia sẻ",
                onClick = {
                    val youtubeUrl = "https://www.youtube.com/watch?v=${song.youtubeId ?: song.id}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}\n$youtubeUrl")
                    }
                    context.startActivity(Intent.createChooser(intent, "Chia sẻ bài hát"))
                    onDismiss()
                },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = TextSecondary,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .background(CardBackground, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}
