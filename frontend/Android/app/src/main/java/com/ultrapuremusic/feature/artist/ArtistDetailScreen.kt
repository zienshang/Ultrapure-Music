package com.ultrapuremusic.feature.artist

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val headerSong = state.allSongs.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {

        // ── Blurred header image ──────────────────────────────────────────────
        if (headerSong?.thumbnailUrl != null) {
            AsyncImage(
                model              = headerSong.thumbnailUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(24.dp),
            )
        }

        // Gradient overlay: transparent at top → Background at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.6f to Color.Black.copy(alpha = 0.70f),
                            1.0f to Background,
                        ),
                    ),
                ),
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            item {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint               = TextPrimary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.isLoadingRemote) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color       = AccentPrimary,
                        )
                    } else {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = "Tải lại",
                                tint               = TextSecondary,
                            )
                        }
                    }
                }
            }

            // ── Artist hero section ───────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Artist avatar circle (first song thumbnail, or placeholder)
                    Box(
                        modifier         = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (headerSong?.thumbnailUrl != null) {
                            AsyncImage(
                                model              = headerSong.thumbnailUrl,
                                contentDescription = state.artistName,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.matchParentSize(),
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = null,
                                tint               = TextSecondary,
                                modifier           = Modifier.size(48.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text  = state.artistName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "${state.allSongs.size} bài hát",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Play all button
                    if (state.allSongs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(AccentPrimary)
                                .clickable {
                                    viewModel.playAll()
                                    onNavigateToPlayer()
                                }
                                .padding(horizontal = 28.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector        = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(20.dp),
                                )
                                Text(
                                    text     = "Phát tất cả",
                                    style    = MaterialTheme.typography.labelLarge,
                                    color    = Color.White,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Section header ────────────────────────────────────────────────
            item {
                Text(
                    text     = "Bài hát",
                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color    = TextPrimary,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                )
            }

            // ── Empty / loading state ─────────────────────────────────────────
            if (state.allSongs.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isLoadingRemote) {
                            CircularProgressIndicator(color = AccentPrimary)
                        } else {
                            Text(
                                text  = "Không tìm thấy bài hát",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            // ── Song list ────────────────────────────────────────────────────
            items(state.allSongs, key = { it.id }) { song ->
                SongCard(
                    title        = song.title,
                    artist       = song.artist,
                    thumbnailUrl = song.thumbnailUrl,
                    onClick      = {
                        viewModel.playSong(song)   // sets full artist queue, starts at this song
                        onNavigateToPlayer()
                    },
                    onArtistClick = { artistName ->
                        if (artistName != state.artistName) onNavigateToArtist(artistName)
                    },
                    onMoreClick  = { actionsVm.showOptions(song) },
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }

    SongOptionsSheetHost(
        actionsVm          = actionsVm,
        onNavigateToPlayer = onNavigateToPlayer,
    )
}
