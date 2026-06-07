package com.ultrapuremusic.core.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.ultrapuremusic.core.ui.viewmodel.SongActionsViewModel
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.launch

/**
 * Drop-in composable that shows [SongOptionsSheet] whenever [actionsVm] has a selected song.
 *
 * Place it at the end of any screen composable that calls [SongActionsViewModel.showOptions].
 *
 * @param onNavigateToPlayer  Called after "Phát ngay" so the screen can open the player.
 * @param onAddToPlaylist     Called when the user taps "Thêm vào playlist". Defaults to no-op.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheetHost(
    actionsVm: SongActionsViewModel,
    onNavigateToPlayer: () -> Unit,
    onAddToPlaylist: (Song) -> Unit = {},
) {
    val selectedSong by actionsVm.selectedSong.collectAsState()
    val isFavorite   by actionsVm.isSelectedFavorite.collectAsState()
    val isDownloaded by actionsVm.isSelectedDownloaded.collectAsState()
    val scope        = rememberCoroutineScope()

    selectedSong?.let { song ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        SongOptionsSheet(
            song             = song,
            sheetState       = sheetState,
            isFavorite       = isFavorite,
            isDownloaded     = isDownloaded,
            onDismiss        = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { actionsVm.dismissOptions() }
            },
            onPlayNow        = { s ->
                actionsVm.playNow(s)
                onNavigateToPlayer()
            },
            onPlayNext       = actionsVm::playNext,
            onAddToQueue     = actionsVm::addToQueue,
            onToggleFavorite = actionsVm::toggleFavorite,
            onAddToPlaylist  = onAddToPlaylist,
            onDownload       = actionsVm::downloadSong,
            onDeleteDownload = actionsVm::deleteDownload,
        )
    }
}
