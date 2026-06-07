package com.ultrapuremusic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ultrapuremusic.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    @ColumnInfo(name = "stream_url") val streamUrl: String?,
    @ColumnInfo(name = "youtube_id") val youtubeId: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean,
    @ColumnInfo(name = "local_path") val localPath: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

fun SongEntity.toModel() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    thumbnailUrl = thumbnailUrl,
    streamUrl = streamUrl,
    youtubeId = youtubeId,
    durationMs = durationMs,
    isDownloaded = isDownloaded,
    addedAt = addedAt
)

fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    thumbnailUrl = thumbnailUrl,
    streamUrl = streamUrl,
    youtubeId = youtubeId,
    durationMs = durationMs,
    isDownloaded = isDownloaded,
    localPath = null,
    addedAt = addedAt
)

