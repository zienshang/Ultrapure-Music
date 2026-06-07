package com.ultrapuremusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ultrapuremusic.core.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    /** Reactive variant — emits whenever the row changes (e.g. after download completes). */
    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeSongById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs ORDER BY added_at DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE is_downloaded = 1 ORDER BY added_at DESC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    suspend fun searchSongs(query: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY added_at DESC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY added_at DESC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL AND album != '' ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET is_downloaded = :isDownloaded, local_path = :localPath WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean, localPath: String?)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)
}

