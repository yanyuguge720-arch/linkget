package com.linkget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun pending(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE status IN ('PENDING', 'RUNNING') ORDER BY createdAt ASC")
    suspend fun active(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun findById(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DownloadTaskEntity>)

    @Query("UPDATE download_tasks SET status = 'RUNNING', error = NULL, speedBytesPerSecond = 0 WHERE id = :id")
    suspend fun markRunning(id: String)

    @Query(
        "UPDATE download_tasks SET bytesDownloaded = :downloaded, totalBytes = :total, " +
            "speedBytesPerSecond = :speed WHERE id = :id AND status = 'RUNNING'"
    )
    suspend fun updateProgress(id: String, downloaded: Long, total: Long, speed: Long = 0L)

    @Query(
        "UPDATE download_tasks SET status = 'COMPLETED', localUri = :uri, speedBytesPerSecond = 0, " +
            "bytesDownloaded = CASE WHEN totalBytes > 0 THEN totalBytes ELSE bytesDownloaded END, " +
            "error = NULL WHERE id = :id"
    )
    suspend fun complete(id: String, uri: String)

    @Query("UPDATE download_tasks SET status = 'FAILED', speedBytesPerSecond = 0, error = :error WHERE id = :id")
    suspend fun fail(id: String, error: String)

    @Query("UPDATE download_tasks SET status = 'PAUSED', speedBytesPerSecond = 0, error = :reason WHERE id = :id")
    suspend fun pause(id: String, reason: String? = null)

    @Query("UPDATE download_tasks SET status = 'PENDING', error = NULL, speedBytesPerSecond = 0 WHERE id = :id AND status IN ('PAUSED', 'FAILED')")
    suspend fun resume(id: String)

    @Query("UPDATE download_tasks SET status = 'PENDING', error = NULL, speedBytesPerSecond = 0 WHERE status = 'FAILED'")
    suspend fun retryAllFailed()

    @Query("UPDATE download_tasks SET status = 'PENDING', error = NULL, speedBytesPerSecond = 0 WHERE status = 'PAUSED'")
    suspend fun resumeAllPaused()

    @Query("UPDATE download_tasks SET status = 'PENDING', speedBytesPerSecond = 0 WHERE status = 'RUNNING'")
    suspend fun resetRunning()

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM download_tasks WHERE status IN ('COMPLETED', 'FAILED', 'PAUSED')")
    suspend fun clearFinished()

    @Query("DELETE FROM download_tasks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'FAILED'")
    suspend fun failedCount(): Int
}
