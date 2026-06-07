package com.ultrapuremusic.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ultrapuremusic.core.ui.components.DraggableItem
import com.ultrapuremusic.core.ui.components.SongCard
import com.ultrapuremusic.core.ui.components.SongOptionsSheetHost
import com.ultrapuremusic.core.ui.components.dragHandle
import com.ultrapuremusic.core.ui.components.rememberDragDropState
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.SuccessColor
import com.ultrapuremusic.core.ui.theme.SurfaceVariant
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel
import com.ultrapuremusic.data.model.Song

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
    actionsVm: SongActionsViewModel = hiltViewModel(),
) {
    val state   by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(playlistId) { viewModel.loadPlaylistDetail(playlistId) }

    // Snackbar — surfaces download completion summary + error messages
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { snackbarHost.showSnackbar(it) }
    }

    // Share export text when it arrives
    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { (name, text) ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Playlist: $name")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ playlist"))
        }
    }

    val playlist = state.selectedPlaylist?.takeIf { it.id == playlistId }
        ?: state.playlists.find { it.id == playlistId }

    // ── Multi-select mode ────────────────────────────────────────────────────
    val selectedIds    = remember { mutableStateListOf<String>() }
    val isSelectMode   = selectedIds.isNotEmpty()

    fun toggleSelect(id: String) {
        if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
    }

    fun exitSelectMode() = selectedIds.clear()

    // ── Reorder mode ────────────────────────────────────────────────────────
    var reorderMode by remember { mutableStateOf(false) }
    // Mutable working copy for drag-and-drop; synced from playlist.songs on load
    val workingSongs = remember { mutableStateListOf<Song>() }
    LaunchedEffect(playlist?.songs) {
        val songs = playlist?.songs ?: return@LaunchedEffect
        if (workingSongs.size != songs.size ||
            workingSongs.zip(songs).any { (a, b) -> a.id != b.id }
        ) {
            workingSongs.clear()
            workingSongs.addAll(songs)
        }
    }

    val listState    = rememberLazyListState()
    val dragDropState = rememberDragDropState(lazyListState = listState) { from, to ->
        workingSongs.add(to, workingSongs.removeAt(from))
    }

    // ── Rename dialog ────────────────────────────────────────────────────────
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText       by remember { mutableStateOf("") }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text  = "Đổi tên playlist",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
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
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor    = AccentPrimary,
                        cursorColor          = AccentPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.renamePlaylist(playlistId, trimmed)
                        }
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank(),
                ) {
                    Text("Lưu", color = AccentPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Huỷ", color = TextSecondary)
                }
            },
            containerColor = com.ultrapuremusic.core.ui.theme.CardBackground,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text  = if (isSelectMode) "${selectedIds.size} đã chọn"
                            else playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isSelectMode -> exitSelectMode()
                        reorderMode  -> {
                            viewModel.reorderTracks(playlistId, workingSongs.toList())
                            reorderMode = false
                        }
                        else -> onNavigateBack()
                    }
                }) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint               = TextPrimary,
                    )
                }
            },
            actions = {
                if (!isSelectMode) {
                    // Export / Share button
                    IconButton(onClick = {
                        viewModel.exportPlaylist(playlistId, playlist?.name ?: "Playlist")
                    }) {
                        Icon(
                            imageVector        = Icons.Default.IosShare,
                            contentDescription = "Xuất / Chia sẻ",
                            tint               = TextSecondary,
                        )
                    }
                    // Rename button
                    IconButton(onClick = {
                        renameText = playlist?.name ?: ""
                        showRenameDialog = true
                    }) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Đổi tên",
                            tint               = TextSecondary,
                        )
                    }
                    // Reorder toggle
                    if (workingSongs.isNotEmpty()) {
                        IconButton(onClick = {
                            if (reorderMode) viewModel.reorderTracks(playlistId, workingSongs.toList())
                            reorderMode = !reorderMode
                        }) {
                            Icon(
                                imageVector        = Icons.Default.DragHandle,
                                contentDescription = if (reorderMode) "Xong sắp xếp" else "Sắp xếp lại",
                                tint               = if (reorderMode) AccentPrimary else TextSecondary,
                            )
                        }
                    }
                } else {
                    // Select all toggle
                    TextButton(onClick = {
                        if (selectedIds.size == workingSongs.size) exitSelectMode()
                        else { selectedIds.clear(); selectedIds.addAll(workingSongs.map { it.id }) }
                    }) {
                        Text(
                            text  = if (selectedIds.size == workingSongs.size) "Bỏ chọn tất cả"
                                    else "Chọn tất cả",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentPrimary,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        LazyColumn(
            state          = listState,
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            // ── Playlist header ───────────────────────────────────────────────
            item {
                playlist?.let { pl ->
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth(0.55f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
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
                                    modifier           = Modifier.size(56.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text  = "${workingSongs.size} bài hát",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )

                        pl.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text  = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (workingSongs.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                // ── Play all ──────────────────────────────────
                                Row(
                                    modifier          = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(AccentPrimary)
                                        .clickable {
                                            viewModel.playPlaylist(workingSongs.toList(), startIndex = 0)
                                            onNavigateToPlayer()
                                        }
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint               = Color.White,
                                        modifier           = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text  = "Phát tất cả",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                }

                                // ── Download playlist ─────────────────────────
                                val progress = state.downloadProgress
                                val allDone  = progress?.isDone == true &&
                                               progress.failed == 0
                                val inFlight = progress?.isDone == false

                                Row(
                                    modifier          = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            when {
                                                allDone  -> SuccessColor.copy(alpha = 0.15f)
                                                inFlight -> AccentPrimary.copy(alpha = 0.12f)
                                                else     -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .clickable(enabled = !inFlight && !allDone) {
                                            viewModel.downloadPlaylist(workingSongs.toList())
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    when {
                                        inFlight -> {
                                            CircularProgressIndicator(
                                                modifier    = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color       = AccentPrimary,
                                            )
                                        }
                                        allDone  -> Icon(
                                            imageVector        = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint               = SuccessColor,
                                            modifier           = Modifier.size(16.dp),
                                        )
                                        else     -> Icon(
                                            imageVector        = Icons.Default.Download,
                                            contentDescription = null,
                                            tint               = TextSecondary,
                                            modifier           = Modifier.size(16.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text  = when {
                                            inFlight -> "${progress!!.processed}/${progress.total}"
                                            allDone  -> "Đã tải"
                                            else     -> "Tải xuống"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when {
                                            allDone  -> SuccessColor
                                            inFlight -> AccentPrimary
                                            else     -> TextSecondary
                                        },
                                    )
                                }
                            }

                            // ── Progress bar (visible while downloading) ──────
                            val progress = state.downloadProgress
                            if (progress?.isDone == false) {
                                Spacer(Modifier.height(12.dp))
                                Column(
                                    modifier            = Modifier.fillMaxWidth(0.8f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    LinearProgressIndicator(
                                        progress     = { progress.fraction },
                                        modifier     = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color        = AccentPrimary,
                                        trackColor   = AccentPrimary.copy(alpha = 0.2f),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text  = buildString {
                                            append("Đang tải ${progress.processed}/${progress.total}")
                                            if (progress.skipped > 0)
                                                append(" · ${progress.skipped} đã có")
                                            if (progress.failed > 0)
                                                append(" · ${progress.failed} lỗi")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            // ── Track list ───────────────────────────────────────────────────
            itemsIndexed(
                items = workingSongs,
                key   = { _, song -> song.id },
            ) { index, song ->
                DraggableItem(
                    index         = index,
                    dragDropState = dragDropState,
                ) { isDragging ->
                    val isItemSelected = song.id in selectedIds
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isItemSelected -> AccentPrimary.copy(alpha = 0.12f)
                                    isDragging     -> AccentPrimary.copy(alpha = 0.08f)
                                    else           -> Color.Transparent
                                }
                            )
                            .combinedClickable(
                                onClick = {
                                    when {
                                        isSelectMode -> toggleSelect(song.id)
                                        !reorderMode -> {
                                            viewModel.playPlaylist(workingSongs.toList(), startIndex = index)
                                            onNavigateToPlayer()
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!reorderMode) toggleSelect(song.id)
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Checkbox in select mode
                        if (isSelectMode) {
                            Checkbox(
                                checked         = isItemSelected,
                                onCheckedChange = { toggleSelect(song.id) },
                                modifier        = Modifier.padding(start = 4.dp),
                                colors          = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor   = AccentPrimary,
                                    checkmarkColor = Color.White,
                                ),
                            )
                        }

                        SongCard(
                            title        = song.title,
                            artist       = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            isDownloaded = song.isDownloaded,
                            onClick      = {
                                when {
                                    isSelectMode -> toggleSelect(song.id)
                                    !reorderMode -> {
                                        viewModel.playPlaylist(workingSongs.toList(), startIndex = index)
                                        onNavigateToPlayer()
                                    }
                                }
                            },
                            onMoreClick  = if (!reorderMode && !isSelectMode)
                                ({ actionsVm.showOptions(song) }) else null,
                            modifier     = Modifier
                                .weight(1f)
                                .padding(
                                    start  = if (isSelectMode) 0.dp else 16.dp,
                                    top    = 4.dp,
                                    bottom = 4.dp,
                                    end    = if (reorderMode) 0.dp else 16.dp,
                                ),
                        )

                        // Drag handle — only visible in reorder mode
                        if (reorderMode && !isSelectMode) {
                            Icon(
                                imageVector        = Icons.Default.DragHandle,
                                contentDescription = "Kéo để sắp xếp",
                                tint               = TextSecondary,
                                modifier           = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp)
                                    .dragHandle(index, dragDropState),
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bulk action bar ───────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = isSelectMode,
        enter   = slideInVertically { it },
        exit    = slideOutVertically { it },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .border(topOnly = true, color = GlassBorder)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Remove selected from playlist
                BulkActionButton(
                    icon    = Icons.Default.Delete,
                    label   = "Xoá (${selectedIds.size})",
                    tint    = Color(0xFFE53935),
                    onClick = {
                        selectedIds.toList().forEach { id ->
                            viewModel.reorderTracks(
                                playlistId,
                                workingSongs.filterNot { it.id == id },
                            )
                        }
                        // Also remove from playlist repository
                        selectedIds.toList().forEach { id ->
                            workingSongs.removeIf { it.id == id }
                        }
                        exitSelectMode()
                    },
                )
                // Add selected to queue
                BulkActionButton(
                    icon    = Icons.Default.PlaylistAdd,
                    label   = "Thêm vào hàng đợi",
                    onClick = {
                        workingSongs.filter { it.id in selectedIds }
                            .forEach { actionsVm.addToQueue(it) }
                        exitSelectMode()
                    },
                )
                // Download selected
                BulkActionButton(
                    icon    = Icons.Default.Download,
                    label   = "Tải xuống",
                    onClick = {
                        workingSongs.filter { it.id in selectedIds }
                            .forEach { actionsVm.downloadSong(it) }
                        exitSelectMode()
                    },
                )
            }
        }
    }

    SongOptionsSheetHost(
        actionsVm          = actionsVm,
        onNavigateToPlayer = onNavigateToPlayer,
    )

    // Snackbar overlay — completion summary + error messages
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHost)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** One-sided top border for the bulk action bar. */
private fun Modifier.border(topOnly: Boolean, color: androidx.compose.ui.graphics.Color): Modifier =
    if (!topOnly) this else this.then(
        Modifier.drawWithContent {
            drawContent()
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end   = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1f,
            )
        }
    )

@Composable
private fun BulkActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextSecondary,
) {
    Column(
        modifier            = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = tint,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
