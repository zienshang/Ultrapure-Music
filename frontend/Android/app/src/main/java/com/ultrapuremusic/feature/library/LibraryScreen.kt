package com.ultrapuremusic.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.SurfaceVariant
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.feature.favorites.FavoritesViewModel
import com.ultrapuremusic.feature.playlist.PlaylistViewModel

private enum class PlaylistSort(val label: String) {
    DEFAULT("Mặc định"),
    ALPHA("A → Z"),
    MOST_SONGS("Nhiều bài"),
    SYNCED("YouTube"),
}

@Composable
fun LibraryScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
    favoritesVm: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val favState by favoritesVm.uiState.collectAsState()

    // Pull latest playlists from backend (incl. newly YouTube-synced ones) on open.
    LaunchedEffect(Unit) { viewModel.refreshPlaylists() }

    // ── Sort state (local — no persistence needed) ────────────────────────────
    var sortOrder by remember { mutableStateOf(PlaylistSort.DEFAULT) }
    val displayedPlaylists = remember(state.playlists, sortOrder) {
        when (sortOrder) {
            PlaylistSort.DEFAULT    -> state.playlists
            PlaylistSort.ALPHA      -> state.playlists.sortedBy { it.name.lowercase() }
            PlaylistSort.MOST_SONGS -> state.playlists.sortedByDescending { it.songCount }
            PlaylistSort.SYNCED     -> state.playlists.sortedByDescending { it.isSynced }
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName  by remember { mutableStateOf("") }

    // ── Import dialog ─────────────────────────────────────────────────────────
    var showImportDialog  by remember { mutableStateOf(false) }
    var importName        by remember { mutableStateOf("") }
    var importTracksText  by remember { mutableStateOf("") }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importName = ""; importTracksText = "" },
            title = {
                Text(
                    text  = "Nhập playlist từ văn bản",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value         = importName,
                        onValueChange = { importName = it },
                        label         = { Text("Tên playlist") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor    = AccentPrimary,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = importTracksText,
                        onValueChange = { importTracksText = it },
                        label         = { Text("Danh sách bài hát") },
                        placeholder   = { Text("Nghệ sĩ - Tên bài\nMỗi dòng một bài", color = TextSecondary) },
                        minLines      = 5,
                        maxLines      = 10,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor    = AccentPrimary,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                        ),
                    )
                    if (state.isImporting) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color    = AccentPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = "Đang tìm và thêm bài hát... (có thể mất vài phút)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick  = {
                        if (importName.isNotBlank() && importTracksText.isNotBlank()) {
                            viewModel.importFromText(importName.trim(), importTracksText.trim())
                            showImportDialog = false
                            importName = ""; importTracksText = ""
                        }
                    },
                    enabled  = importName.isNotBlank() && importTracksText.isNotBlank() && !state.isImporting,
                ) { Text("Nhập", color = AccentPrimary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showImportDialog = false; importName = ""; importTracksText = "" },
                ) { Text("Huỷ", color = TextSecondary) }
            },
            containerColor = CardBackground,
            shape          = RoundedCornerShape(20.dp),
        )
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    var renameTargetId   by remember { mutableStateOf<String?>(null) }
    var renameText       by remember { mutableStateOf("") }
    val showRenameDialog = renameTargetId != null

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { renameTargetId = null },
            title = {
                Text(
                    text  = "Đổi tên playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = com.ultrapuremusic.core.ui.theme.TextPrimary,
                )
            },
            text = {
                OutlinedTextField(
                    value         = renameText,
                    onValueChange = { renameText = it },
                    singleLine    = true,
                    label         = { Text("Tên mới") },
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentPrimary,
                        unfocusedBorderColor = com.ultrapuremusic.core.ui.theme.GlassBorder,
                        focusedLabelColor    = AccentPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        val id = renameTargetId
                        val name = renameText.trim()
                        if (id != null && name.isNotEmpty()) viewModel.renamePlaylist(id, name)
                        renameTargetId = null
                    },
                    enabled = renameText.isNotBlank(),
                ) {
                    Text("Lưu", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetId = null }) {
                    Text("Huỷ", color = com.ultrapuremusic.core.ui.theme.TextSecondary)
                }
            },
            containerColor = CardBackground,
        )
    }

    // ── Create-playlist dialog ─────────────────────────────────────────────────
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newPlaylistName  = ""
            },
            title = {
                Text(
                    text  = "Tạo playlist mới",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            },
            text = {
                OutlinedTextField(
                    value         = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label         = { Text("Tên playlist") },
                    placeholder   = { Text("Ví dụ: Nhạc thư giãn", color = TextSecondary) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor    = AccentPrimary,
                        cursorColor          = AccentPrimary,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            showCreateDialog = false
                            newPlaylistName  = ""
                        }
                    },
                    enabled  = newPlaylistName.isNotBlank(),
                ) {
                    Text(
                        text  = "Tạo",
                        color = if (newPlaylistName.isNotBlank()) AccentPrimary else TextSecondary,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newPlaylistName  = ""
                    },
                ) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor    = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor  = TextPrimary,
            shape             = RoundedCornerShape(20.dp),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .background(Background),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(
                        start  = 20.dp,
                        end    = 20.dp,
                        top    = 24.dp,
                        bottom = 8.dp,
                    ),
                ) {
                    Text(
                        text  = "Thư viện",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "Bộ sưu tập của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            // ── Quick access row ──────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickAccessCard(
                        icon     = Icons.Default.Favorite,
                        label    = "Yêu thích",
                        sublabel = if (favState.songs.isNotEmpty()) "${favState.songs.size} bài" else null,
                        tint     = Color(0xFFE91E8C),
                        onClick  = onNavigateToFavorites,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAccessCard(
                        icon     = Icons.Default.CloudDownload,
                        label    = "Tải về",
                        tint     = AccentPrimary,
                        onClick  = onNavigateToDownloads,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Playlists section ─────────────────────────────────────────────
            item {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "Playlist của tôi",
                        style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color    = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text     = "${displayedPlaylists.size} playlist",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextSecondary,
                    )
                    androidx.compose.material3.IconButton(
                        onClick = { showImportDialog = true },
                    ) {
                        Icon(
                            imageVector        = Icons.Default.FileUpload,
                            contentDescription = "Nhập playlist",
                            tint               = TextSecondary,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── Sort chips ────────────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(PlaylistSort.entries) { sort ->
                        val selected = sort == sortOrder
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) AccentPrimary else CardBackground)
                                .border(
                                    1.dp,
                                    if (selected) AccentPrimary else GlassBorder,
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable { sortOrder = sort }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text  = sort.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else TextSecondary,
                            )
                        }
                    }
                }
            }

            if (displayedPlaylists.isEmpty() && state.playlists.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint               = TextSecondary.copy(alpha = 0.4f),
                                modifier           = Modifier.size(56.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text      = "Chưa có playlist nào",
                                style     = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color     = TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "Nhấn + để tạo playlist đầu tiên",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = TextSecondary.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayedPlaylists,
                    key   = { it.id },
                ) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick  = { onNavigateToPlaylist(playlist.id) },
                        onRename = {
                            renameText     = playlist.name
                            renameTargetId = playlist.id
                        },
                        onDelete = { viewModel.deletePlaylist(playlist.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick        = { showCreateDialog = true },
            containerColor = AccentPrimary,
            contentColor   = Color.White,
            shape          = RoundedCornerShape(16.dp),
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tạo playlist")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick access card (Favorites / Downloads)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
) {
    Row(
        modifier          = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            if (sublabel != null) {
                Text(
                    text  = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlist row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier          = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail or placeholder
        Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
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
                    modifier           = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = playlist.name,
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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

        // MoreVert → Dropdown
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector        = Icons.Default.MoreVert,
                    contentDescription = "Tùy chọn",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded         = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text          = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DriveFileRenameOutline, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Đổi tên")
                        }
                    },
                    onClick       = { showMenu = false; onRename() },
                )
                DropdownMenuItem(
                    text          = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Delete, null,
                                Modifier.size(18.dp),
                                tint = androidx.compose.ui.graphics.Color(0xFFE53935),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Xoá playlist", color = androidx.compose.ui.graphics.Color(0xFFE53935))
                        }
                    },
                    onClick       = { showMenu = false; onDelete() },
                )
            }
        }
    }
}
