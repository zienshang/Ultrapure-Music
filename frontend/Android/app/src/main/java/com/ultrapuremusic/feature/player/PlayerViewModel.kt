package com.ultrapuremusic.feature.player

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.player.PlayerState
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import com.ultrapuremusic.domain.usecase.cache.DownloadSongUseCase
import com.ultrapuremusic.domain.usecase.favorites.AddFavoriteUseCase
import com.ultrapuremusic.domain.usecase.favorites.RemoveFavoriteUseCase
import com.ultrapuremusic.domain.usecase.playback.NextSongUseCase
import com.ultrapuremusic.domain.usecase.playback.PauseSongUseCase
import com.ultrapuremusic.domain.usecase.playback.PrevSongUseCase
import com.ultrapuremusic.domain.usecase.playback.SeekUseCase
import com.ultrapuremusic.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One line of a synced lyric with its start timestamp. */
data class LyricLine(val timestampMs: Long, val text: String)

/** Parse an LRC-format string into a list of timestamped lines. */
fun parseSyncedLyrics(raw: String): List<LyricLine> {
    val regex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
    return raw.trim().lines().mapNotNull { line ->
        regex.matchEntire(line.trim())?.let { m ->
            val (min, sec, cs, text) = m.destructured
            val ms = min.toLong() * 60_000L +
                     sec.toLong() * 1_000L +
                     cs.padEnd(3, '0').toLong()
            LyricLine(ms, text.trim()).takeIf { it.text.isNotEmpty() }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val musicRepository: MusicRepository,
    private val pauseSongUseCase: PauseSongUseCase,
    private val nextSongUseCase: NextSongUseCase,
    private val prevSongUseCase: PrevSongUseCase,
    private val seekUseCase: SeekUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao,
    private val downloadSongUseCase: DownloadSongUseCase,
) : BaseViewModel<PlayerState>(PlayerState()) {

    val playerState: StateFlow<PlayerState> = playerRepository.playerState

    /** Reacts to both song changes and Room DB changes — always up-to-date. */
    val isFavorite: StateFlow<Boolean> = playerState
        .map { it.currentSong?.id }
        .flatMapLatest { songId ->
            if (songId == null) flowOf(false)
            else favoriteDao.isFavorite(songId)
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * Live downloaded status for the song currently in the player.
     * Observes the songs table directly, so the UI flips to "Đã tải" the moment
     * MusicRepositoryImpl.downloadSong upserts the row with isDownloaded = true —
     * the in-memory Song inside PlayerState is never mutated, so we cannot rely
     * on `playerState.value.currentSong.isDownloaded`.
     */
    val isCurrentSongDownloaded: StateFlow<Boolean> = playerState
        .map { it.currentSong?.id }
        .flatMapLatest { songId ->
            if (songId == null) flowOf(false)
            else songDao.observeSongById(songId).map { it?.isDownloaded == true }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    // ── Lyrics ───────────────────────────────────────────────────────────────

    private val _lyrics         = MutableStateFlow<List<LyricLine>>(emptyList())
    private val _lyricsLoading  = MutableStateFlow(false)
    val lyrics:        StateFlow<List<LyricLine>> = _lyrics.asStateFlow()
    val lyricsLoading: StateFlow<Boolean>          = _lyricsLoading.asStateFlow()

    // ── Related songs ────────────────────────────────────────────────────────

    private val _relatedSongs = MutableStateFlow<List<Song>>(emptyList())
    val relatedSongs: StateFlow<List<Song>> = _relatedSongs.asStateFlow()

    init {
        playerState
            .map { it.currentSong }
            .distinctUntilChanged { a, b -> a?.id == b?.id }
            .onEach { song ->
                _relatedSongs.value = emptyList()
                _lyrics.value       = emptyList()
                if (song != null) {
                    val vid = song.youtubeId ?: song.id
                    if (vid.isNotBlank()) fetchRelatedSongs(vid)
                    fetchLyrics(song)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchLyrics(song: Song) {
        launch {
            _lyricsLoading.value = true
            val durationSec = (song.durationMs / 1000L).toInt()
            val resp = musicRepository.getLyrics(song.title, song.artist, durationSec)
            _lyrics.value = when {
                resp?.syncedLyrics != null -> parseSyncedLyrics(resp.syncedLyrics)
                resp?.plainLyrics  != null -> resp.plainLyrics.lines()
                    .filter { it.isNotBlank() }
                    .mapIndexed { i, t -> LyricLine(i * 3_000L, t) }
                else -> emptyList()
            }
            _lyricsLoading.value = false
        }
    }

    private fun fetchRelatedSongs(videoId: String) {
        launch {
            if (videoId.isNotBlank()) {
                when (val result = musicRepository.getRelatedSongs(videoId)) {
                    is ResultWrapper.Success -> _relatedSongs.value = result.data
                    else -> Unit
                }
            }
        }
    }

    // ── Download state ───────────────────────────────────────────────────────

    /**
     * Current download progress:
     *   null  — no download in flight
     *   0f    — started, no bytes reported yet (server didn't send Content-Length)
     *   0..1f — bytes written / expected bytes
     */
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    // ── Playback ─────────────────────────────────────────────────────────────

    fun togglePlayPause() {
        val state = playerState.value
        if (state.isPlaying) pauseSongUseCase()
        else playerRepository.resume()
    }

    fun skipNext() = nextSongUseCase()
    fun skipPrev() = prevSongUseCase()
    fun seekTo(positionMs: Long) = seekUseCase(positionMs)
    fun toggleShuffle() = playerRepository.toggleShuffle()
    fun toggleRepeat() = playerRepository.toggleRepeat()

    // ── Queue ────────────────────────────────────────────────────────────────

    fun addToQueue(song: Song) = playerRepository.addToQueue(song)
    fun playNext(song: Song)   = playerRepository.playNext(song)
    fun setQueue(songs: List<Song>, startIndex: Int) = playerRepository.setQueue(songs, startIndex)
    fun jumpToQueueIndex(index: Int)  = playerRepository.jumpToQueueIndex(index)
    fun removeFromQueue(index: Int)   = playerRepository.removeFromQueue(index)

    // ── Speed & sleep ─────────────────────────────────────────────────────────

    fun setPlaybackSpeed(speed: Float)   = playerRepository.setPlaybackSpeed(speed)
    fun setSleepTimer(durationMs: Long)  = playerRepository.setSleepTimer(durationMs)
    fun cancelSleepTimer()               = playerRepository.cancelSleepTimer()

    // ── Favorite ─────────────────────────────────────────────────────────────

    fun toggleFavorite() {
        val song = playerState.value.currentSong ?: return
        launch {
            if (isFavorite.value) removeFavoriteUseCase(song.id)
            else addFavoriteUseCase(song)
        }
    }

    // ── Download ─────────────────────────────────────────────────────────────

    fun downloadCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (_downloadProgress.value != null) return    // already in flight — ignore tap
        launch {
            _downloadProgress.value = 0f
            try {
                when (val result = downloadSongUseCase(song) { fraction ->
                    _downloadProgress.value = fraction
                }) {
                    is com.ultrapuremusic.core.util.ResultWrapper.Error -> {
                        emitError(result.message)
                    }
                    else -> Unit
                }
            } finally {
                _downloadProgress.value = null
            }
        }
    }
}
