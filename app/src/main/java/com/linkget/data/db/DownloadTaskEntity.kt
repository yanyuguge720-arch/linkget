package com.linkget.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { PENDING, RUNNING, PAUSED, COMPLETED, FAILED }

fun String.toDownloadStatus(): DownloadStatus =
    runCatching { DownloadStatus.valueOf(this) }.getOrDefault(DownloadStatus.PENDING)

/** One file in the download queue. */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val workId: String,
    val title: String,
    val kind: String,
    val platform: String,
    val quality: String? = null,
    val remoteUrl: String,
    val fileName: String,
    val folder: String,
    val status: String = DownloadStatus.PENDING.name,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val localUri: String? = null,
    val error: String? = null,
    val needMerge: Boolean = false,
    val audioUrl: String? = null,
    val liveVideoUrl: String? = null,
    val extractFromVideo: Boolean = false,
    val durationMs: Long? = null,
    val createdAt: Long
)
