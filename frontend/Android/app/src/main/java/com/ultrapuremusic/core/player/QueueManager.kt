package com.ultrapuremusic.core.player

import com.ultrapuremusic.data.model.Song

class QueueManager {
    private val _queue = mutableListOf<Song>()
    private var _currentIndex = 0
    private var _isShuffled = false
    private val _shuffledIndices = mutableListOf<Int>()

    val queue: List<Song> get() = _queue.toList()
    val currentIndex: Int get() = _currentIndex
    val currentSong: Song? get() = _queue.getOrNull(_currentIndex)

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.clear()
        _queue.addAll(songs)
        _currentIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        if (_isShuffled) buildShuffledOrder()
    }

    fun addToQueue(song: Song) {
        _queue.add(song)
    }

    /** Insert [song] immediately after the currently playing track. */
    fun playNext(song: Song) {
        val insertAt = (_currentIndex + 1).coerceAtMost(_queue.size)
        _queue.add(insertAt, song)
    }

    /**
     * Remove the song at [index] from the queue.
     * If the removed song is the one currently playing, the index is adjusted
     * so that playback continues with the next available song (or the previous
     * one when we were at the tail).
     */
    fun removeAtIndex(index: Int) {
        if (index < 0 || index >= _queue.size) return
        _queue.removeAt(index)
        when {
            _queue.isEmpty() -> _currentIndex = 0
            index < _currentIndex -> _currentIndex-- // shift left
            index == _currentIndex -> _currentIndex = _currentIndex.coerceAtMost(_queue.size - 1)
            // index > _currentIndex → nothing changes
        }
    }

    fun moveToNext(): Song? {
        if (_queue.isEmpty()) return null
        return if (_currentIndex < _queue.size - 1) {
            _currentIndex++
            currentSong
        } else null
    }

    fun moveToPrev(): Song? {
        if (_queue.isEmpty()) return null
        return if (_currentIndex > 0) {
            _currentIndex--
            currentSong
        } else null
    }

    /** Jump to an explicit index (used for repeat-all wrap-around). */
    fun moveToIndex(index: Int): Song? {
        if (_queue.isEmpty()) return null
        _currentIndex = index.coerceIn(0, _queue.size - 1)
        return currentSong
    }

    /**
     * Replace the song at the current queue position in-place.
     * Used by [AudioPlayerManager] to clear a stale [Song.streamUrl] before
     * triggering a retry so [StreamUrlResolver] is forced to fetch a fresh URL.
     */
    fun replaceCurrent(song: Song) {
        if (_currentIndex in _queue.indices) _queue[_currentIndex] = song
    }

    /** Returns the next song without advancing the index, or null at end of queue. */
    fun peekNext(): Song? = _queue.getOrNull(_currentIndex + 1)

    /** Updates the song at [index] in-place — used to store a pre-fetched stream URL. */
    fun updateAt(index: Int, song: Song) {
        if (index in _queue.indices) _queue[index] = song
    }

    fun toggleShuffle() {
        _isShuffled = !_isShuffled
        if (_isShuffled) buildShuffledOrder()
    }

    fun isShuffled() = _isShuffled

    private fun buildShuffledOrder() {
        _shuffledIndices.clear()
        _shuffledIndices.addAll((0 until _queue.size).toMutableList().also { it.shuffle() })
    }
}
