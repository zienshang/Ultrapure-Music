package com.ultrapuremusic.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ultrapuremusic.core.database.entity.PlaylistEntity
import com.ultrapuremusic.core.database.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

/** Aggregate row: a playlist plus how many of its tracks are downloaded. */
data class PlaylistWithDownloadInfo(
    @Embedded val playlist: PlaylistEntity,
    val downloadedCount: Int,
    val totalCount: Int,
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("SELECT * FROM playlists ORDER BY updated_at DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("SELECT song_id FROM playlist_song_cross_ref WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getSongIdsForPlaylist(playlistId: String): List<String>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Query("UPDATE playlists SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun renamePlaylist(id: String, name: String, now: Long = System.currentTimeMillis())

    /** Set thumbnail only if the playlist currently has none (first-song auto-cover). */
    @Query("UPDATE playlists SET thumbnail_url = :url WHERE id = :id AND (thumbnail_url IS NULL OR thumbnail_url = '')")
    suspend fun setThumbnailIfEmpty(id: String, url: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRefs(refs: List<PlaylistSongCrossRef>)

    /** Overwrite all cross-refs for a playlist in one transaction (used for reorder). */
    @Transaction
    suspend fun replacePlaylistSongs(playlistId: String, refs: List<PlaylistSongCrossRef>) {
        clearPlaylistSongs(playlistId)
        insertPlaylistSongCrossRefs(refs)
    }

    /**
     * Playlists that have at least one track downloaded for offline playback.
     * Joins playlists → cross-refs → songs and groups by playlist so the UI can
     * show "5/12 đã tải" without N+1 queries.
     */
    @Query("""
        SELECT
            p.*,
            COALESCE(SUM(CASE WHEN s.is_downloaded = 1 THEN 1 ELSE 0 END), 0) AS downloadedCount,
            COUNT(pcr.song_id) AS totalCount
        FROM playlists p
        LEFT JOIN playlist_song_cross_ref pcr ON pcr.playlist_id = p.id
        LEFT JOIN songs s ON s.id = pcr.song_id
        GROUP BY p.id
        HAVING downloadedCount > 0
        ORDER BY p.updated_at DESC
    """)
    fun getPlaylistsWithDownloads(): Flow<List<PlaylistWithDownloadInfo>>
}

