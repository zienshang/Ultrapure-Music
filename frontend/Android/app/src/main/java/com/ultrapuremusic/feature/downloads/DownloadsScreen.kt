package com.ultrapuremusic.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.SuccessColor
import com.ultrapuremusic.core.ui.theme.SurfaceVariant
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text  = "Tải về",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                Icon(
                    imageVector        = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint               = AccentPrimary,
                    modifier           = Modifier
                        .padding(start = 16.dp)
                        .size(24.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // ── Tabs ─────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Background,
            contentColor     = TextPrimary,
            indicator        = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color    = AccentPrimary,
                    height   = 3.dp,
                )
            },
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text = {
                    Text(
                        text  = "Playlist (${state.playlists.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedTab == 0) AccentPrimary else TextSecondary,
                    )
                },
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text = {
                    Text(
                        text  = "Bài hát (${state.songs.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedTab == 1) AccentPrimary else TextSecondary,
                    )
                },
            )
        }

        // ── Content ──────────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> DownloadedPlaylistsList(
                playlists           = state.playlists,
                onNavigateToPlaylist = onNavigateToPlaylist,
            )
            1 -> DownloadedSongsList(
                songs               = state.songs,
                downloadingIds      = state.downloadingIds,
                onPlay              = { song ->
                    viewModel.playSong(song)
                    onNavigateToPlayer()
                },
                onMoreClick         = actionsVm::showOptions,
            )
        }
    }

    SongOptionsSheetHost(
        actionsVm          = actionsVm,
        onNavigateToPlayer = onNavigateToPlayer,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlist tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadedPlaylistsList(
    playlists: List<DownloadedPlaylist>,
    onNavigateToPlaylist: (String) -> Unit,
) {
    if (playlists.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.LibraryMusic,
            title   = "Chưa có playlist nào tải về",
            message = "Mở một playlist và bấm \"Tải xuống\" để lưu offline",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)) {
        items(playlists, key = { it.playlist.id }) { item ->
            DownloadedPlaylistRow(
                item    = item,
                onClick = { onNavigateToPlaylist(item.playlist.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DownloadedPlaylistRow(
    item: DownloadedPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pl = item.playlist
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!pl.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = pl.thumbnailUrl,
                    contentDescription = pl.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint               = TextSecondary,
                    modifier           = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = pl.name,
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = if (item.isFullyDownloaded)
                                "${item.totalCount} bài đã tải"
                            else
                                "${item.downloadedCount}/${item.totalCount} bài đã tải",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                if (item.isFullyDownloaded) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = SuccessColor,
                        modifier           = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Song tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadedSongsList(
    songs: List<com.ultrapuremusic.data.model.Song>,
    downloadingIds: Set<String>,
    onPlay: (com.ultrapuremusic.data.model.Song) -> Unit,
    onMoreClick: (com.ultrapuremusic.data.model.Song) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.CloudOff,
            title   = "Chưa có bài hát tải về",
            message = "Tải nhạc để nghe offline không cần mạng",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)) {
        items(songs, key = { it.id }) { song ->
            val isDownloading = song.id in downloadingIds
            SongCard(
                title        = song.title,
                artist       = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                isDownloaded = song.isDownloaded,
                onClick      = { if (!isDownloading) onPlay(song) },
                onMoreClick  = { onMoreClick(song) },
                modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.4f),
                modifier           = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.7f),
            )
        }
    }
}
