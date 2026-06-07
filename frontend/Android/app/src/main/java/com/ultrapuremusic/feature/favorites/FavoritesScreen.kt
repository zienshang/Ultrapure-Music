package com.ultrapuremusic.feature.favorites

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text  = "Yêu thích",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    if (state.songs.isNotEmpty()) {
                        Text(
                            text  = "${state.songs.size} bài hát",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            },
            navigationIcon = {
                Icon(
                    imageVector        = Icons.Default.Favorite,
                    contentDescription = null,
                    tint               = Color(0xFFE91E8C),
                    modifier           = Modifier.padding(start = 16.dp).size(24.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // "Phát tất cả" + "Phát ngẫu nhiên" buttons
        if (state.songs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.playAll(0)
                        onNavigateToPlayer()
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Phát tất cả")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.playAll((state.songs.indices).random())
                        onNavigateToPlayer()
                    },
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ngẫu nhiên")
                }
            }
        }

        if (state.songs.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Default.Favorite,
                        contentDescription = null,
                        tint               = TextSecondary.copy(alpha = 0.4f),
                        modifier           = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text  = "Chưa có bài hát yêu thích",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Nhấn ♡ khi nghe nhạc để lưu vào đây",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                itemsIndexed(state.songs, key = { _, s -> s.id }) { index, song ->
                    SongCard(
                        title        = song.title,
                        artist       = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        onClick      = {
                            viewModel.playAll(index)
                            onNavigateToPlayer()
                        },
                        onMoreClick  = { actionsVm.showOptions(song) },
                        modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    SongOptionsSheetHost(
        actionsVm          = actionsVm,
        onNavigateToPlayer = onNavigateToPlayer,
    )
}
