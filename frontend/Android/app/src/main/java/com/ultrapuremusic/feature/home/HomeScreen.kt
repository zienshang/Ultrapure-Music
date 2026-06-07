package com.ultrapuremusic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongCardSkeleton
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.SurfaceVariant
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            HomeHeader()
        }

        // ── Search bar (tappable → SearchScreen) ──────────────────────────────
        item {
            SearchBarButton(
                onClick = onNavigateToSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // ── Hero card ─────────────────────────────────────────────────────────
        if (state.isLoading || state.trendingSongs.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                if (state.isLoading) {
                    HeroCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    HeroCard(
                        song = state.trendingSongs.first(),
                        onClick = {
                            viewModel.playSong(state.trendingSongs.first())
                            onNavigateToPlayer()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Trending Now ──────────────────────────────────────────────────────
        item {
            SectionHeader(
                title = "Trending Now",
                actionLabel = "Xem thêm",
                onAction = onNavigateToSearch,
            )
        }

        item {
            if (state.isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(5) { TrendingCardSkeleton() }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.trendingSongs.drop(1).take(10),
                        key   = { it.id },
                    ) { song ->
                        TrendingCard(
                            song    = song,
                            onClick = {
                                viewModel.playSong(song)
                                onNavigateToPlayer()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Playlists ─────────────────────────────────────────────────────────
        if (state.playlists.isNotEmpty()) {
            item {
                SectionHeader(title = "Danh sách phát")
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.playlists,
                        key   = { "pl_${it.id}" },
                    ) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick  = { onNavigateToPlaylist(playlist.id) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Recommendations (For you) ───────────────────────────────────────────
        if (state.recommendedSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Dành cho bạn")
                Spacer(Modifier.height(4.dp))
            }
            items(
                items = state.recommendedSongs.take(15),
                key   = { "rec_${it.id}" },
            ) { song ->
                SongCard(
                    title         = song.title,
                    artist        = song.artist,
                    thumbnailUrl  = song.thumbnailUrl,
                    isDownloaded  = song.isDownloaded,
                    onClick       = {
                        viewModel.playSong(song)
                        onNavigateToPlayer()
                    },
                    onArtistClick = onNavigateToArtist,
                    onMoreClick   = { actionsVm.showOptions(song) },
                    modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        } else if (state.isLoadingRecommendations) {
            item {
                SectionHeader(title = "Dành cho bạn")
                Spacer(Modifier.height(4.dp))
            }
            items(4) {
                SongCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        // ── Community-made YouTube playlists ────────────────────────────────────
        if (state.communityPlaylists.isNotEmpty()) {
            item {
                SectionHeader(title = "Danh sách từ mọi người tạo")
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.communityPlaylists,
                        key   = { "cpl_${it.id}" },
                    ) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick  = { onNavigateToPlaylist(playlist.id) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── From your YouTube account ───────────────────────────────────────────
        if (state.youtubeFeedSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Gợi ý từ YouTube của bạn")
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.youtubeFeedSongs.take(12),
                        key   = { "yt_${it.id}" },
                    ) { song ->
                        TrendingCard(
                            song    = song,
                            onClick = {
                                viewModel.playSong(song)
                                onNavigateToPlayer()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Popular with listeners ──────────────────────────────────────────────
        if (state.popularSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Thịnh hành với người nghe")
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.popularSongs.take(12),
                        key   = { "pop_${it.id}" },
                    ) { song ->
                        TrendingCard(
                            song    = song,
                            onClick = {
                                viewModel.playSong(song)
                                onNavigateToPlayer()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Recently Played ───────────────────────────────────────────────────
        if (state.recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Gần đây")
                Spacer(Modifier.height(4.dp))
            }

            items(
                items = state.recentlyPlayed.take(10),
                key   = { "recent_${it.id}" },
            ) { song ->
                SongCard(
                    title         = song.title,
                    artist        = song.artist,
                    thumbnailUrl  = song.thumbnailUrl,
                    isDownloaded  = song.isDownloaded,
                    onClick       = {
                        viewModel.playSong(song)
                        onNavigateToPlayer()
                    },
                    onArtistClick = onNavigateToArtist,
                    onMoreClick   = { actionsVm.showOptions(song) },
                    modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        state.error?.let { err ->
            item {
                Text(
                    text     = err,
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    SongOptionsSheetHost(
        actionsVm          = actionsVm,
        onNavigateToPlayer = onNavigateToPlayer,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
    ) {
        Text(
            text  = "Ultrapure Music",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Khám phá âm nhạc không giới hạn",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar button (navigates to SearchScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(GlassBackground)
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(50.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Default.Search,
            contentDescription = "Tìm kiếm",
            tint               = TextSecondary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text  = "Tìm bài hát, nghệ sĩ...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero card — first trending song, full-width, 200 dp tall
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        // Album art background
        AsyncImage(
            model            = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale     = ContentScale.Crop,
            modifier         = Modifier.matchParentSize(),
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors      = listOf(Color.Transparent, Color(0xCC000000)),
                        startY      = 0f,
                        endY        = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        // Text + play button
        Row(
            modifier          = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = "Nổi bật hôm nay",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = AccentPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color    = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text     = song.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Play button
            Box(
                modifier        = Modifier
                    .size(48.dp)
                    .background(AccentPrimary, CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.PlayArrow,
                    contentDescription = "Phát",
                    tint               = Color.White,
                    modifier           = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Trending card — 130 dp wide, 1:1 album art
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendingCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onClick() },
    ) {
        // 1:1 album art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(SurfaceVariant),
        ) {
            AsyncImage(
                model              = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize(),
            )
        }

        // Title + artist
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text     = song.title,
                style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = song.artist,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrendingCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(SurfaceVariant),
        )
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariant),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariant),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlist card — 140 dp wide, 1:1 cover, name + song count
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!playlist.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint               = TextSecondary,
                    modifier           = Modifier.size(36.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text     = playlist.name,
                style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "${playlist.songCount} bài hát",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color    = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text  = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPrimary,
                )
            }
        }
    }
}
