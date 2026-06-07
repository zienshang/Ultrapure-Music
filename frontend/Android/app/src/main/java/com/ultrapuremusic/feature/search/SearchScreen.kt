package com.ultrapuremusic.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // expanded = true khi có query hoặc genre được chọn
    val expanded = state.query.isNotEmpty() || state.selectedGenre != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query          = state.query,
                    onQueryChange  = viewModel::onQueryChange,
                    onSearch       = { /* debounce handles it */ },
                    expanded       = expanded,
                    onExpandedChange = { /* controlled by query */ },
                    placeholder = {
                        Text("Tìm bài hát, nghệ sĩ...", color = TextSecondary)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearSearch) {
                                Icon(Icons.Default.Close, contentDescription = "Xóa", tint = TextSecondary)
                            }
                        }
                    },
                )
            },
            expanded         = expanded,
            onExpandedChange = { if (!it) viewModel.clearSearch() },
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ── Results pane (inside the expanded SearchBar) ──────────────────
            when {
                state.isSearching -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CircularProgressIndicator(color = AccentPrimary)
                    }
                }

                state.error != null -> {
                    SearchEmptyState(
                        icon    = Icons.Default.SearchOff,
                        title   = "Có lỗi xảy ra",
                        message = state.error ?: "",
                    )
                }

                state.results.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        item { GenreChipRow(selectedGenre = state.selectedGenre, onSelect = viewModel::setGenre) }
                        item {
                            Text(
                                text     = "${state.results.size} kết quả${if (state.selectedGenre != null) " · ${state.selectedGenre}" else ""}",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = TextSecondary,
                                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp),
                            )
                        }
                        items(state.results, key = { it.id }) { song ->
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
                }

                state.hasSearched -> {
                    Column {
                        GenreChipRow(selectedGenre = state.selectedGenre, onSelect = viewModel::setGenre)
                        SearchEmptyState(
                            icon    = Icons.Default.SearchOff,
                            title   = "Không tìm thấy",
                            message = "Không có kết quả nào cho \"${state.query}\"",
                        )
                    }
                }

                else -> {
                    // Browsing by genre with no text query — show chips + loading
                    Column {
                        GenreChipRow(selectedGenre = state.selectedGenre, onSelect = viewModel::setGenre)
                        if (state.isSearching) {
                            Box(
                                modifier         = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(28.dp),
                                    color       = AccentPrimary,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        SongOptionsSheetHost(
            actionsVm          = actionsVm,
            onNavigateToPlayer = onNavigateToPlayer,
        )

        // ── Idle state (no query, no genre selected) ─────────────────────────
        if (!expanded) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text  = "Tìm kiếm",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Khám phá hàng triệu bài hát",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(28.dp))
                // Genre quick-browse — tapping a genre immediately searches for it
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text     = "Thể loại",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = TextPrimary,
                        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
                    )
                    val rows = MUSIC_GENRES.chunked(3)
                    rows.forEach { row ->
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardBackground)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setGenre(genre) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text  = genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                    )
                                }
                            }
                            // Fill empty slots in the last row
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Genre chip row — horizontal scroll strip shown inside the expanded search pane
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GenreChipRow(selectedGenre: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(MUSIC_GENRES) { genre ->
            val isSelected = genre == selectedGenre
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) AccentPrimary else CardBackground)
                    .border(1.dp, if (isSelected) AccentPrimary else GlassBorder, RoundedCornerShape(20.dp))
                    .clickable { onSelect(genre) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text  = genre,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else TextSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty/error state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.4f),
                modifier           = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.7f),
            )
        }
    }
}
