package com.ultrapuremusic.feature.album

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val state     by viewModel.uiState.collectAsState()
    val coverSong = state.songs.firstOrNull()

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Album art cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (coverSong?.thumbnailUrl != null) {
                        AsyncImage(
                            model              = coverSong.thumbnailUrl,
                            contentDescription = state.albumName,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.matchParentSize(),
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Album,
                            contentDescription = null,
                            tint               = TextSecondary.copy(alpha = 0.3f),
                            modifier           = Modifier
                                .size(96.dp)
                                .align(Alignment.Center),
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.7f to Color.Black.copy(alpha = 0.5f),
                                        1.0f to Background,
                                    ),
                                ),
                            ),
                    )
                }

                // Back button (overlaid on top of art)
                IconButton(
                    onClick  = onNavigateBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint               = Color.White,
                    )
                }
            }
        }

        // ── Album info ────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text  = state.albumName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                if (coverSong != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = coverSong.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentPrimary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${state.songs.size} bài hát",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(16.dp))

                // Play all button
                if (state.songs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(AccentPrimary)
                            .padding(horizontal = 28.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text  = "Phát tất cả",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // ── Empty / loading ───────────────────────────────────────────────────
        if (state.isLoading) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
        } else if (state.songs.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Không có bài hát nào trong album này",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        // ── Track list ────────────────────────────────────────────────────────
        items(state.songs, key = { it.id }) { song ->
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
                modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
