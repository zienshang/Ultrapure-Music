package com.ultrapuremusic.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.ultrapuremusic.core.player.RepeatMode
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import com.ultrapuremusic.core.ui.components.MusicSeekBar
import com.ultrapuremusic.core.ui.components.PlayerIconButton
import com.ultrapuremusic.core.ui.components.QueueBottomSheet
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.PlayerGradientStart
import com.ultrapuremusic.core.ui.theme.Surface
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel,
) {
    val state            by viewModel.playerState.collectAsState()
    val isFavorite       by viewModel.isFavorite.collectAsState()
    val isDownloaded     by viewModel.isCurrentSongDownloaded.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val relatedSongs     by viewModel.relatedSongs.collectAsState()
    val lyrics           by viewModel.lyrics.collectAsState()
    val lyricsLoading    by viewModel.lyricsLoading.collectAsState()
    val song             = state.currentSong
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    // ── Sheet states ─────────────────────────────────────────────────────────
    val queueSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sleepSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueueSheet  by remember { mutableStateOf(false) }
    var showSpeedSheet  by remember { mutableStateOf(false) }
    var showSleepSheet  by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    // ── Snackbar for download / playback errors ──────────────────────────────
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    // ── Dynamic accent color from album art palette ───────────────────────────
    var accentColor by remember { mutableStateOf(PlayerGradientStart) }
    val animatedAccent by animateColorAsState(
        targetValue   = accentColor,
        animationSpec = tween(durationMillis = 800),
        label         = "player_accent",
    )

    LaunchedEffect(song?.thumbnailUrl) {
        val url = song?.thumbnailUrl
        if (url.isNullOrBlank()) { accentColor = PlayerGradientStart; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context).data(url).size(200, 200).build()
                val result  = context.imageLoader.execute(request)
                val rawBitmap = (result as? SuccessResult)?.image?.toBitmap()
                val bitmap = rawBitmap?.let { bmp ->
                    if (bmp.config == android.graphics.Bitmap.Config.HARDWARE)
                        bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    else bmp
                }
                bitmap?.let { bmp ->
                    val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                    val swatch  = palette.darkVibrantSwatch ?: palette.dominantSwatch ?: palette.vibrantSwatch
                    swatch?.let { s -> accentColor = Color(s.rgb) }
                }
            } catch (_: Exception) { }
        }
    }

    // ── Gradient background ───────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to animatedAccent.copy(alpha = 0.85f),
                        0.45f to animatedAccent.copy(alpha = 0.35f),
                        1.0f to Background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Đóng",
                        tint               = TextSecondary,
                        modifier           = Modifier.size(32.dp),
                    )
                }
                Text(
                    text      = "Đang phát",
                    style     = MaterialTheme.typography.labelLarge,
                    color     = TextSecondary,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        imageVector        = if (isFavorite) Icons.Default.Favorite
                                             else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích",
                        tint               = if (isFavorite) AccentPrimary else TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Album art ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(
                        elevation    = 32.dp,
                        shape        = RoundedCornerShape(28.dp),
                        ambientColor = animatedAccent.copy(alpha = 0.6f),
                        spotColor    = animatedAccent.copy(alpha = 0.6f),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model              = song?.thumbnailUrl,
                    contentDescription = song?.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.matchParentSize(),
                )
                // Animated equalizer bars — visible when playing, fades to rest when paused
                AnimatedEqualizer(
                    isPlaying = state.isPlaying,
                    modifier  = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .width(60.dp)
                        .height(24.dp),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Title & artist ────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = song?.title ?: "—",
                        style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color    = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = song?.artist ?: "—",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Seek bar ──────────────────────────────────────────────────────
            MusicSeekBar(
                currentPositionMs = state.positionMs,
                durationMs        = state.durationMs,
                onSeek            = viewModel::seekTo,
                modifier          = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // ── Playback controls ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                PlayerIconButton(
                    icon               = Icons.Default.Shuffle,
                    contentDescription = "Ngẫu nhiên",
                    onClick            = viewModel::toggleShuffle,
                    isActive           = state.isShuffled,
                    tint               = if (state.isShuffled) AccentPrimary else TextSecondary,
                )
                PlayerIconButton(
                    icon               = Icons.Default.SkipPrevious,
                    contentDescription = "Trước",
                    onClick            = viewModel::skipPrev,
                    size               = 56.dp,
                    iconSize           = 32.dp,
                )
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .background(AccentPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick  = viewModel::togglePlayPause,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector        = if (state.isPlaying) Icons.Default.Pause
                                                 else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Tạm dừng" else "Phát",
                            tint               = Color.White,
                            modifier           = Modifier.size(38.dp),
                        )
                    }
                }
                PlayerIconButton(
                    icon               = Icons.Default.SkipNext,
                    contentDescription = "Tiếp theo",
                    onClick            = viewModel::skipNext,
                    size               = 56.dp,
                    iconSize           = 32.dp,
                )
                PlayerIconButton(
                    icon               = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else           -> Icons.Default.Repeat
                    },
                    contentDescription = "Lặp lại",
                    onClick            = viewModel::toggleRepeat,
                    isActive           = state.repeatMode != RepeatMode.NONE,
                    tint               = if (state.repeatMode != RepeatMode.NONE) AccentPrimary
                                         else TextSecondary,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Secondary row: Queue / Speed / Sleep timer ────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Queue
                SecondaryButton(
                    icon               = Icons.Default.QueueMusic,
                    label              = "Hàng đợi",
                    isActive           = state.queue.isNotEmpty(),
                    onClick            = { showQueueSheet = true },
                )

                // Playback speed
                SecondaryButton(
                    icon               = Icons.Default.Speed,
                    label              = if (state.playbackSpeed == 1.0f) "Tốc độ"
                                         else "${state.playbackSpeed}x",
                    isActive           = state.playbackSpeed != 1.0f,
                    onClick            = { showSpeedSheet = true },
                )

                // Sleep timer
                SecondaryButton(
                    icon    = Icons.Default.AccessTime,
                    label   = if (state.isSleepTimerActive)
                                  formatSleepRemaining(state.sleepTimerEndMs)
                              else "Hẹn giờ",
                    isActive = state.isSleepTimerActive,
                    onClick  = { showSleepSheet = true },
                )

                // Lyrics
                SecondaryButton(
                    icon     = Icons.Default.Lyrics,
                    label    = "Lời",
                    isActive = lyrics.isNotEmpty(),
                    onClick  = { showLyricsSheet = true },
                )

                // Download
                DownloadButton(
                    isDownloaded     = isDownloaded,
                    downloadProgress = downloadProgress,
                    onClick          = viewModel::downloadCurrentSong,
                )
            }

            // ── Related songs ─────────────────────────────────────────────────
            if (relatedSongs.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = "Bài hát tương tự",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    relatedSongs.forEach { relatedSong ->
                        RelatedSongCard(
                            song    = relatedSong,
                            onClick = { viewModel.setQueue(listOf(relatedSong), 0) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Queue sheet ───────────────────────────────────────────────────────────
    if (showQueueSheet) {
        QueueBottomSheet(
            queue        = state.queue,
            currentIndex = state.currentIndex,
            sheetState   = queueSheetState,
            onDismiss    = {
                scope.launch { queueSheetState.hide() }.invokeOnCompletion {
                    showQueueSheet = false
                }
            },
            onJumpTo     = { index ->
                viewModel.jumpToQueueIndex(index)
                scope.launch { queueSheetState.hide() }.invokeOnCompletion {
                    showQueueSheet = false
                }
            },
            onRemove     = viewModel::removeFromQueue,
            onClearQueue = {
                // Clear queue and stop — set an empty queue
                state.queue.indices.reversed().forEach { viewModel.removeFromQueue(it) }
            },
        )
    }

    // ── Speed sheet ───────────────────────────────────────────────────────────
    if (showSpeedSheet) {
        SpeedPickerSheet(
            currentSpeed = state.playbackSpeed,
            sheetState   = speedSheetState,
            onDismiss    = {
                scope.launch { speedSheetState.hide() }.invokeOnCompletion {
                    showSpeedSheet = false
                }
            },
            onSelectSpeed = { speed ->
                viewModel.setPlaybackSpeed(speed)
                scope.launch { speedSheetState.hide() }.invokeOnCompletion {
                    showSpeedSheet = false
                }
            },
        )
    }

    // ── Sleep timer sheet ─────────────────────────────────────────────────────
    if (showSleepSheet) {
        SleepTimerSheet(
            isActive    = state.isSleepTimerActive,
            endMs       = state.sleepTimerEndMs,
            sheetState  = sleepSheetState,
            onDismiss   = {
                scope.launch { sleepSheetState.hide() }.invokeOnCompletion {
                    showSleepSheet = false
                }
            },
            onSetTimer  = { durationMs ->
                viewModel.setSleepTimer(durationMs)
                scope.launch { sleepSheetState.hide() }.invokeOnCompletion {
                    showSleepSheet = false
                }
            },
            onCancel    = {
                viewModel.cancelSleepTimer()
                scope.launch { sleepSheetState.hide() }.invokeOnCompletion {
                    showSleepSheet = false
                }
            },
        )
    }

    // ── Lyrics sheet ─────────────────────────────────────────────────────────
    if (showLyricsSheet) {
        LyricsSheet(
            lyrics      = lyrics,
            isLoading   = lyricsLoading,
            positionMs  = state.positionMs,
            sheetState  = lyricsSheetState,
            onDismiss   = {
                scope.launch { lyricsSheetState.hide() }.invokeOnCompletion {
                    showLyricsSheet = false
                }
            },
        )
    }

    // ── Snackbar overlay ─────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHost)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Download button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadButton(
    isDownloaded: Boolean,
    downloadProgress: Float?,            // null = not downloading
    onClick: () -> Unit,
) {
    val isDownloading = downloadProgress != null

    Column(
        modifier            = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isDownloaded && !isDownloading) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            isDownloading -> {
                // Determinate circular ring with percentage in the center.
                // Animates smoothly so the user sees the value tick up.
                val animated by androidx.compose.animation.core.animateFloatAsState(
                    targetValue   = downloadProgress ?: 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                    label         = "download_progress",
                )
                Box(
                    modifier         = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Faint track ring underneath so 0 % still looks like a circle
                    CircularProgressIndicator(
                        progress    = { 1f },
                        modifier    = Modifier.size(28.dp),
                        color       = AccentPrimary.copy(alpha = 0.18f),
                        strokeWidth = 2.5.dp,
                    )
                    // Filled progress on top
                    CircularProgressIndicator(
                        progress    = { animated },
                        modifier    = Modifier.size(28.dp),
                        color       = AccentPrimary,
                        strokeWidth = 2.5.dp,
                        trackColor  = androidx.compose.ui.graphics.Color.Transparent,
                    )
                    Text(
                        text     = "${(animated * 100).toInt()}",
                        style    = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(9f,
                                androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                        color    = AccentPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
            isDownloaded  -> Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = "Đã tải xuống",
                tint               = AccentPrimary,
                modifier           = Modifier.size(22.dp),
            )
            else          -> Icon(
                imageVector        = Icons.Default.Download,
                contentDescription = "Tải xuống",
                tint               = TextSecondary,
                modifier           = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = when {
                isDownloading -> "Đang tải"
                isDownloaded  -> "Đã tải"
                else          -> "Tải xuống"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isDownloading || isDownloaded -> AccentPrimary
                else                          -> TextSecondary
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Speed picker sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPickerSheet(
    currentSpeed: Float,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = null,
    ) {
        Text(
            text     = "Tốc độ phát",
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color    = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelectSpeed(speed) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = if (speed == 1.0f) "1x" else "${speed}x",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else TextPrimary,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sleep timer sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    isActive: Boolean,
    endMs: Long,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSetTimer: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    val presets = listOf(
        "15 phút"  to TimeUnit.MINUTES.toMillis(15),
        "30 phút"  to TimeUnit.MINUTES.toMillis(30),
        "45 phút"  to TimeUnit.MINUTES.toMillis(45),
        "60 phút"  to TimeUnit.MINUTES.toMillis(60),
        "90 phút"  to TimeUnit.MINUTES.toMillis(90),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = null,
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "Hẹn giờ tắt nhạc",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color    = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                Text(
                    text  = "Còn ${formatSleepRemaining(endMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPrimary,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            presets.forEach { (label, durationMs) ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSetTimer(durationMs) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isActive && endMs > 0 &&
                        (endMs - System.currentTimeMillis()) in
                        (durationMs - 30_000)..(durationMs + 30_000)
                    ) {
                        Text("✓", color = AccentPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFE5393510))
                        .clickable { onCancel() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Huỷ hẹn giờ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = androidx.compose.ui.graphics.Color(0xFFE53935),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecondaryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
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
            tint               = if (isActive) AccentPrimary else TextSecondary,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) AccentPrimary else TextSecondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated equalizer bars
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barCount: Int = 5,
) {
    val animatables = remember { List(barCount) { Animatable(0.2f) } }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatables.mapIndexed { index, anim ->
                launch {
                    delay(index * 70L)
                    while (isActive) {
                        anim.animateTo(
                            targetValue    = 0.2f + Random.nextFloat() * 0.8f,
                            animationSpec  = tween(
                                durationMillis = 120 + Random.nextInt(250),
                                easing         = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
            }
        } else {
            animatables.map { anim ->
                launch { anim.animateTo(0.2f, tween(300)) }
            }
        }
    }

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.Bottom,
    ) {
        animatables.forEach { anim ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(anim.value)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Related song card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RelatedSongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model              = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text     = song.title,
            style    = MaterialTheme.typography.labelMedium,
            color    = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text     = song.artist,
            style    = MaterialTheme.typography.labelSmall,
            color    = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lyrics sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(
    lyrics: List<LyricLine>,
    isLoading: Boolean,
    positionMs: Long,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Index of the lyric line whose timestamp is closest to / just before positionMs.
    val currentIndex by remember(positionMs, lyrics) {
        derivedStateOf {
            if (lyrics.isEmpty()) 0
            else lyrics.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
        }
    }

    // Auto-scroll to the active line whenever it changes.
    LaunchedEffect(currentIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(
                index        = currentIndex,
                scrollOffset = -120,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = null,
    ) {
        Text(
            text     = "Lời bài hát",
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color    = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        when {
            isLoading -> Box(
                modifier         = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color    = AccentPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }

            lyrics.isEmpty() -> Box(
                modifier         = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "Không tìm thấy lời bài hát",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            else -> LazyColumn(
                state           = listState,
                modifier        = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding  = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == currentIndex
                    Text(
                        text      = line.text,
                        style     = if (isActive)
                            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium,
                        color     = if (isActive) AccentPrimary else TextSecondary.copy(alpha = 0.6f),
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun formatSleepRemaining(endMs: Long): String {
    if (endMs <= 0L) return ""
    val remaining = (endMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val minutes   = TimeUnit.MILLISECONDS.toMinutes(remaining)
    val seconds   = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
    return if (minutes > 0) "${minutes}p ${seconds}s" else "${seconds}s"
}
