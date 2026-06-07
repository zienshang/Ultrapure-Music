package com.ultrapuremusic.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val queueManager: QueueManager,
    private val streamUrlResolver: StreamUrlResolver,
    private val audioEffectsManager: AudioEffectsManager,
    private val userPreferences: UserPreferences,
) {
    // Audio focus is handled by ExoPlayer (PlayerModule: setAudioAttributes(attrs, true)).
    // A separate AudioFocusRequest conflicts with ExoPlayer's internal request and causes
    // AUDIOFOCUS_LOSS to fire right after play — keep focus handling entirely in ExoPlayer.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "AudioPlayerManager coroutine error")
        _playerState.value = _playerState.value.copy(
            isBuffering = false,
            error = throwable.message ?: "Playback error",
        )
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    /** True after [release] is called — prevents calling ExoPlayer post-teardown. */
    @Volatile private var isReleased = false
    /** Number of consecutive auto-retries for the current song (reset on success). */
    private var retryCount = 0
    private val maxRetries = 1

    // ── Widget ───────────────────────────────────────────────────────────────
    private val widgetUpdater = WidgetUpdater(context)

    // ── Crossfade ────────────────────────────────────────────────────────────
    private var crossfadeDurationMs: Long = 0L
    private var isCrossfading = false
    private var crossfadeJob: Job? = null
    private var fadeInJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        // Keep crossfadeDurationMs in sync with user's setting.
        userPreferences.crossfadeDurationMs
            .onEach { ms -> crossfadeDurationMs = ms }
            .launchIn(scope)

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                if (isPlaying) startPositionUpdater() else stopPositionUpdater()
                notifyWidget()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                _playerState.value = _playerState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = duration
                )
                // Auto-advance when the current song ends. (REPEAT_ONE is handled
                // natively by ExoPlayer and never reaches STATE_ENDED.)
                if (playbackState == Player.STATE_ENDED) {
                    onSongCompleted()
                }
                // Pre-fetch the next song's stream URL as soon as the current one starts
                // playing so there is no network-fetch gap between tracks.
                if (playbackState == Player.STATE_READY) {
                    prefetchNextTrack()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val song = queueManager.currentSong
                _playerState.value = _playerState.value.copy(
                    currentSong  = song,
                    currentIndex = queueManager.currentIndex,
                    queue        = queueManager.queue,
                )
                notifyWidget()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val isHttpError = error.cause
                    ?.let { it is androidx.media3.datasource.HttpDataSource.HttpDataSourceException }
                    ?: false
                if (isHttpError && retryCount < maxRetries) {
                    retryCount++
                    // Force StreamUrlResolver to re-fetch by clearing the cached URL.
                    val stale = queueManager.currentSong
                    if (stale != null) {
                        queueManager.replaceCurrent(stale.copy(streamUrl = null))
                    }
                    android.util.Log.w("AudioPlayerManager",
                        "Playback HTTP error — retrying with a fresh stream URL (attempt $retryCount)")
                    playCurrent()
                } else {
                    retryCount = 0
                    exoPlayer.stop()  // clear ExoPlayer's error state
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        error = error.message,
                    )
                }
            }
        })
    }

    /** Play a single song — replaces the queue with just this song. */
    fun playSong(song: Song) {
        cancelCrossfade()
        queueManager.setQueue(listOf(song), 0)
        playCurrent()
    }

    /**
     * Loads and plays the queue's current song, resolving its stream URL lazily.
     * Used by [playSong], [setQueue], [skipToNext] and [skipToPrev] so the queue
     * is never destroyed and each track is resolved only when it actually plays.
     */
    private fun playCurrent() {
        val song = queueManager.currentSong ?: return
        if (isReleased) return   // service already torn down — don't touch ExoPlayer
        // Reflect the selection immediately (buffering UI) before the URL resolves.
        _playerState.value = _playerState.value.copy(
            currentSong  = song,
            currentIndex = queueManager.currentIndex,
            queue        = queueManager.queue,
            isBuffering  = true,
            error        = null,
        )
        scope.launch {
            val resolved = streamUrlResolver.resolve(song)
            val url = resolved.streamUrl
            if (url.isNullOrBlank()) {
                _playerState.value = _playerState.value.copy(
                    isBuffering = false,
                    error = "Không phát được \"${song.title}\"",
                )
                return@launch
            }
            if (isReleased) return@launch  // guard: released while URL was being fetched
            val wasCrossfading = isCrossfading
            val fadeDuration = crossfadeDurationMs.coerceAtLeast(500L)
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(resolved.title)
                        .setArtist(resolved.artist)
                        .setArtworkUri(resolved.thumbnailUrl?.let { Uri.parse(it) })
                        .build()
                )
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // When crossfading, start silent then fade in; otherwise full volume.
            exoPlayer.volume = if (wasCrossfading) 0f else 1f
            exoPlayer.play()
            retryCount = 0
            _playerState.value = _playerState.value.copy(currentSong = resolved)
            if (wasCrossfading) {
                // Fade in the new track over the crossfade window.
                crossfadeJob = null
                fadeInJob = scope.launch {
                    val steps = 20
                    val stepDelay = fadeDuration / steps
                    for (i in 1..steps) {
                        if (!isActive || isReleased) break
                        exoPlayer.volume = i.toFloat() / steps
                        delay(stepDelay)
                    }
                    exoPlayer.volume = 1f
                    isCrossfading = false
                }
            }
        }
    }

    fun pause() {
        cancelCrossfade()
        exoPlayer.pause()
    }

    fun resume() {
        // After the queue finishes, ExoPlayer sits in STATE_ENDED at the tail of the
        // last item; play() alone would be a no-op. Rewind so it replays.
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(positionMs = positionMs)
    }

    /**
     * Manual "next" (button press). Always does something: at the end of the queue
     * it wraps to the first song so the control never feels dead.
     */
    fun skipToNext() {
        cancelCrossfade()
        if (queueManager.moveToNext() != null) {
            playCurrent()
        } else if (queueManager.queue.isNotEmpty()) {
            queueManager.moveToIndex(0)
            playCurrent()
        }
    }

    fun skipToPrev() {
        cancelCrossfade()
        if (exoPlayer.currentPosition > 3000 || exoPlayer.playbackState == Player.STATE_ENDED) {
            seekTo(0)
            exoPlayer.play()
        } else if (queueManager.moveToPrev() != null) {
            playCurrent()
        } else if (queueManager.queue.isNotEmpty()) {
            // Wrap to the last song.
            queueManager.moveToIndex(queueManager.queue.size - 1)
            playCurrent()
        }
    }

    /**
     * Auto-advance when a song finishes. Moves to the next song; at the end of the
     * queue it wraps only when repeat-all is on, otherwise playback stops.
     */
    private fun onSongCompleted() {
        if (queueManager.moveToNext() != null) {
            playCurrent()
        } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
            queueManager.moveToIndex(0)
            playCurrent()
        }
        // else: end of queue, no repeat → stop. resume() will replay on demand.
    }

    /** Replace the queue with [songs] and start playing from [startIndex]. */
    fun setQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        cancelCrossfade()
        queueManager.setQueue(songs, startIndex)
        playCurrent()
    }

    fun addToQueue(song: Song) {
        queueManager.addToQueue(song)
        _playerState.value = _playerState.value.copy(queue = queueManager.queue)
    }

    /** Insert [song] right after the currently playing track. */
    fun playNext(song: Song) {
        queueManager.playNext(song)
        _playerState.value = _playerState.value.copy(queue = queueManager.queue)
    }

    /** Remove the queue entry at [index]. */
    fun removeFromQueue(index: Int) {
        queueManager.removeAtIndex(index)
        _playerState.value = _playerState.value.copy(
            queue        = queueManager.queue,
            currentIndex = queueManager.currentIndex,
        )
    }

    /** Jump to a specific position in the queue and play it. */
    fun jumpToQueueIndex(index: Int) {
        if (queueManager.moveToIndex(index) != null) playCurrent()
    }

    // ── Playback speed ───────────────────────────────────────────────────────

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    // ── Sleep timer ──────────────────────────────────────────────────────────

    fun setSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        val endMs = System.currentTimeMillis() + durationMs
        _playerState.value = _playerState.value.copy(sleepTimerEndMs = endMs)
        sleepTimerJob = scope.launch {
            delay(durationMs)
            exoPlayer.pause()
            _playerState.value = _playerState.value.copy(sleepTimerEndMs = 0L)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playerState.value = _playerState.value.copy(sleepTimerEndMs = 0L)
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
        _playerState.value = _playerState.value.copy(isShuffled = queueManager.isShuffled())
    }

    fun toggleRepeat() {
        val next = when (_playerState.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _playerState.value = _playerState.value.copy(repeatMode = next)
        // Only REPEAT_ONE is delegated to ExoPlayer (it loops the single current
        // MediaItem). REPEAT_ALL is handled by our queue logic on track-end, so we
        // keep ExoPlayer OFF — otherwise it would loop one item and never emit
        // STATE_ENDED, breaking auto-advance through the playlist.
        exoPlayer.repeatMode =
            if (next == RepeatMode.ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun release() {
        isReleased = true
        cancelCrossfade()
        stopPositionUpdater()
        sleepTimerJob?.cancel()
        audioEffectsManager.release()
        exoPlayer.release()
    }

    // ── Crossfade ────────────────────────────────────────────────────────────

    /** Cancel any in-progress volume fade and restore full volume immediately. */
    private fun cancelCrossfade() {
        crossfadeJob?.cancel(); crossfadeJob = null
        fadeInJob?.cancel();    fadeInJob    = null
        isCrossfading = false
        if (!isReleased) exoPlayer.volume = 1f
    }

    /** Begin fading out the current track. Called from the position updater. */
    private fun startCrossfadeOut(timeLeftMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        val startVol = exoPlayer.volume
        val totalMs  = timeLeftMs.coerceAtLeast(200L)
        crossfadeJob = scope.launch {
            val steps     = 20
            val stepDelay = totalMs / steps
            for (i in 1..steps) {
                if (!isActive || isReleased) break
                exoPlayer.volume = startVol * (1f - i.toFloat() / steps)
                delay(stepDelay)
            }
            if (!isReleased) exoPlayer.volume = 0f
        }
    }

    /**
     * Silently resolves the next song's stream URL while the current track plays.
     * When the user skips, [playCurrent] will skip the network round-trip because
     * [StreamUrlResolverImpl] returns early when [Song.streamUrl] is already populated.
     */
    private fun prefetchNextTrack() {
        val nextSong = queueManager.peekNext() ?: return
        if (!nextSong.streamUrl.isNullOrBlank()) return   // already resolved
        scope.launch {
            val resolved = streamUrlResolver.resolve(nextSong)
            if (!resolved.streamUrl.isNullOrBlank()) {
                queueManager.updateAt(queueManager.currentIndex + 1, resolved)
            }
        }
    }

    private fun notifyWidget() {
        val state = _playerState.value
        val song  = state.currentSong ?: return
        widgetUpdater.update(
            title     = song.title,
            artist    = song.artist,
            isPlaying = state.isPlaying,
            artUrl    = song.thumbnailUrl,
        )
    }

    private fun startPositionUpdater() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val position = exoPlayer.currentPosition
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                _playerState.value = _playerState.value.copy(positionMs = position)
                // Trigger crossfade when within the configured window before track end.
                if (crossfadeDurationMs > 0L && duration > 0L && !isCrossfading
                    && _playerState.value.repeatMode != RepeatMode.ONE
                ) {
                    val timeLeft = duration - position
                    if (timeLeft in 1L..(crossfadeDurationMs + 600L)) {
                        startCrossfadeOut(timeLeft.coerceAtLeast(200L))
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdater() { positionJob?.cancel() }
}
