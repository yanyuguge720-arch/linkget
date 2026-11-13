package com.linkget.data.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.linkget.core.FileNames
import com.linkget.data.db.DownloadDao
import com.linkget.data.db.DownloadStatus
import com.linkget.data.db.DownloadTaskEntity
import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.ResourceKind
import com.linkget.data.settings.SettingsRepository
import com.linkget.service.DownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DownloadSummary(
    val active: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val percent: Int = -1,
    val speed: Long = 0L,
    val currentTitle: String? = null
)

/** Owns the queue. Work lives in Room and runs inside [DownloadService]. */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val fileDownloader: ResumableFileDownloader,
    private val publisher: MediaPublisher,
    private val settings: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    suspend fun enqueue(
        work: ParsedWork,
        resources: List<MediaResource>,
        extractAudio: Boolean = false
    ): Int {
        if (resources.isEmpty()) return 0
        val folder = FileNames.sanitizeFolder(settings.state.value.saveFolder)
        val now = System.currentTimeMillis()
        val imageCount = resources.count { it.kind == ResourceKind.IMAGE }
        val videoCount = resources.count { it.kind == ResourceKind.VIDEO }

        val tasks = resources.mapIndexed { position, resource ->
            val livePhoto = resource.kind == ResourceKind.IMAGE && !resource.liveVideoUrl.isNullOrBlank()
            val extension = if (livePhoto) "jpg" else resource.resolvedExtension()
            val base = FileNames.baseName(work.title, now)
            val suffix = buildString {
                when (resource.kind) {
                    ResourceKind.VIDEO -> {
                        if (resource.liveMotion) {
                            append("_实况_").append(resource.index.toString().padStart(2, '0'))
                        } else {
                            val quality = resource.quality
                            if (!quality.isNullOrBlank() && (videoCount > 1 || quality != "高清")) {
                                append("_").append(quality.replace("/", "-"))
                            }
                        }
                    }
                    ResourceKind.AUDIO -> append("_audio")
                    ResourceKind.IMAGE -> {
                        if (imageCount > 1) {
                            append("_").append(resource.index.toString().padStart(2, '0'))
                        }
                        if (livePhoto) append("_MP")
                    }
                }
            }
            DownloadTaskEntity(
                id = UUID.randomUUID().toString(),
                workId = work.id,
                title = work.title,
                kind = resource.kind.name,
                platform = work.platform.name,
                quality = resource.quality,
                remoteUrl = resource.url,
                fileName = (if (livePhoto) "MVIMG_" else "") + base + suffix + "." + extension,
                folder = folder,
                status = DownloadStatus.PENDING.name,
                needMerge = resource.needMerge && !resource.audioUrl.isNullOrBlank(),
                audioUrl = resource.audioUrl,
                liveVideoUrl = resource.liveVideoUrl,
                durationMs = resource.durationMs ?: work.durationMs,
                createdAt = now + position
            )
        }.toMutableList()

        val firstVideo = resources.firstOrNull { it.kind == ResourceKind.VIDEO }
        if (extractAudio && firstVideo != null) {
            val base = FileNames.baseName(work.title, now)
            tasks.add(
                DownloadTaskEntity(
                    id = UUID.randomUUID().toString(),
                    workId = work.id,
                    title = work.title,
                    kind = ResourceKind.AUDIO.name,
                    platform = work.platform.name,
                    quality = "M4A",
                    remoteUrl = firstVideo.url,
                    fileName = base + "_audio.m4a",
                    folder = folder,
                    status = DownloadStatus.PENDING.name,
                    extractFromVideo = true,
                    durationMs = firstVideo.durationMs ?: work.durationMs,
                    createdAt = now + resources.size
                )
            )
        }

        dao.insertAll(tasks)
        startService()
        return tasks.size
    }

    fun startService() {
        val intent = Intent(context, DownloadService::class.java)
            .setAction(DownloadService.ACTION_START)
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    fun resumeIfNeeded() {
        scope.launch {
            if (dao.pending().isNotEmpty()) startService()
        }
    }

    fun pause(id: String) {
        scope.launch {
            dao.pause(id)
            activeJobs[id]?.cancel()
        }
    }

    fun resume(id: String) {
        scope.launch {
            dao.resume(id)
            startService()
        }
    }

    fun retry(id: String) = resume(id)

    fun delete(id: String) {
        scope.launch {
            dao.pause(id)
            activeJobs[id]?.cancel()
            dao.delete(id)
            File(context.cacheDir, "downloads/" + id + ".part").delete()
            File(context.cacheDir, "downloads/" + id + ".audio.part").delete()
            File(context.cacheDir, "downloads/" + id + ".merged.mp4").delete()
        }
    }

    fun retryAllFailed() {
        scope.launch {
            dao.retryAllFailed()
            startService()
        }
    }

    fun resumeAllPaused() {
        scope.launch {
            dao.resumeAllPaused()
            startService()
        }
    }

    fun clearFinished() {
        scope.launch { dao.clearFinished() }
    }

    fun observeSummary(): Flow<DownloadSummary> = dao.observeAll()
        .map { tasks ->
            val activeTasks = tasks.filter {
                it.status == DownloadStatus.PENDING.name || it.status == DownloadStatus.RUNNING.name
            }
            val totalBytes = activeTasks.sumOf { it.totalBytes }
            val doneBytes = activeTasks.sumOf { it.bytesDownloaded }
            DownloadSummary(
                active = activeTasks.size,
                completed = tasks.count { it.status == DownloadStatus.COMPLETED.name },
                failed = tasks.count { it.status == DownloadStatus.FAILED.name },
                percent = if (totalBytes > 0) ((doneBytes * 100) / totalBytes).toInt() else -1,
                speed = activeTasks.sumOf { it.speedBytesPerSecond },
                currentTitle = activeTasks.firstOrNull {
                    it.status == DownloadStatus.RUNNING.name
                }?.title ?: activeTasks.firstOrNull()?.title
            )
        }
        .distinctUntilChanged()

    suspend fun processPending() {
        if (!mutex.tryLock()) return
        try {
            val pending = dao.pending()
            if (pending.isEmpty()) return
            val parallel = settings.state.value.maxParallel.coerceIn(1, 3)
            supervisorScope {
                val semaphore = Semaphore(parallel)
                val jobs = pending.map { task ->
                    launch(Dispatchers.IO) {
                        val job = currentCoroutineContext()[Job]
                        if (job != null) activeJobs[task.id] = job
                        try {
                            semaphore.withPermit { runTask(task) }
                        } finally {
                            if (job != null) activeJobs.remove(task.id, job)
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun runTask(task: DownloadTaskEntity) {
        val current = dao.findById(task.id) ?: return
        if (current.status != DownloadStatus.PENDING.name) return
        if (settings.state.value.wifiOnly && !isWifi()) {
            dao.pause(task.id, "仅 Wi-Fi 下载已开启，连接 Wi-Fi 后继续")
            return
        }

        val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val temp = File(dir, task.id + ".part")
        dao.markRunning(task.id)
        var lastBytes = 0L
        var lastAt = SystemClock.elapsedRealtime()
        var lastEmit = 0L

        try {
            val video = fileDownloader.download(
                task.remoteUrl,
                temp,
                headersFor(task)
            ) { downloaded, total ->
                val now = SystemClock.elapsedRealtime()
                val elapsed = (now - lastAt).coerceAtLeast(1)
                val speed = ((downloaded - lastBytes) * 1000L / elapsed).coerceAtLeast(0)
                if (now - lastEmit >= PROGRESS_INTERVAL_MS) {
                    lastEmit = now
                    lastBytes = downloaded
                    lastAt = now
                    scope.launch { dao.updateProgress(task.id, downloaded, total, speed) }
                }
            }

            if (task.kind == ResourceKind.IMAGE.name && !task.liveVideoUrl.isNullOrBlank()) {
                val liveVideo = File(dir, task.id + ".live.mp4")
                fileDownloader.download(task.liveVideoUrl, liveVideo, headersFor(task))
                val motion = File(dir, task.id + ".motion.jpg")
                if (!MotionPhotoBuilder.build(video, liveVideo, motion)) {
                    throw IOException("实况照片合成失败，请重试")
                }
                val uri = publisher.publish(motion, task)
                motion.delete()
                liveVideo.delete()
                video.delete()
                dao.complete(task.id, uri)
                return
            }

            if (task.kind == ResourceKind.IMAGE.name) {
                ImageFormatNormalizer.ensureJpeg(video)
            }

            if (task.extractFromVideo) {
                val audioFile = File(dir, task.id + ".m4a")
                if (!AudioExtractor.extract(video, audioFile)) {
                    throw IOException("视频没有可提取的音轨")
                }
                dao.updateProgress(task.id, audioFile.length(), audioFile.length(), 0)
                val uri = publisher.publish(audioFile, task)
                audioFile.delete()
                video.delete()
                dao.complete(task.id, uri)
                return
            }

            val finalFile: File
            if (task.needMerge && !task.audioUrl.isNullOrBlank()) {
                val audio = File(dir, task.id + ".audio.part")
                fileDownloader.download(task.audioUrl, audio, headersFor(task))
                val merged = File(dir, task.id + ".merged.mp4")
                if (!Mp4Muxer.merge(video, audio, merged)) {
                    throw IOException("音视频合并失败，请改选 720P 版本")
                }
                finalFile = merged
                audio.delete()
                video.delete()
            } else {
                finalFile = video
            }

            val uri = publisher.publish(finalFile, task)
            finalFile.delete()
            dao.complete(task.id, uri)
        } catch (e: CancellationException) {
            val latest = dao.findById(task.id)
            if (latest?.status == DownloadStatus.RUNNING.name) {
                dao.pause(task.id, "已暂停")
            }
            throw e
        } catch (e: Exception) {
            val latest = dao.findById(task.id)
            if (latest?.status == DownloadStatus.PAUSED.name) return
            dao.fail(task.id, readableError(e))
        } finally {
            if (dao.findById(task.id)?.status != DownloadStatus.RUNNING.name) {
                temp.delete()
                File(dir, task.id + ".audio.part").delete()
            }
        }
    }

    private fun headersFor(task: DownloadTaskEntity): Map<String, String> = when (task.platform) {
        "BILIBILI" -> buildMap {
            put("Referer", "https://www.bilibili.com/")
            put("Origin", "https://www.bilibili.com")
            put("Accept", "*/*")
            val cookie = settings.state.value.bilibiliCookie
            if (cookie.isNotBlank()) put("Cookie", cookie)
        }
        "DOUYIN" -> mapOf(
            "Referer" to "https://www.douyin.com/",
            "Accept" to "*/*"
        )
        "XIAOHONGSHU" -> mapOf(
            "Referer" to "https://www.xiaohongshu.com/",
            "Accept" to "*/*"
        )
        "KUAISHOU" -> mapOf(
            "Referer" to "https://www.kuaishou.com/",
            "Accept" to "*/*"
        )
        "TOUTIAO" -> mapOf(
            "Referer" to "https://www.toutiao.com/",
            "Accept" to "*/*"
        )
        else -> emptyMap()
    }

    private fun isWifi(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun readableError(error: Exception): String = when {
        error is DownloadException && error.statusCode == 403 -> "下载地址已失效或被拒绝（403）"
        error is DownloadException && error.statusCode in 400..499 -> "下载地址已失效（" + error.statusCode + "）"
        error.message?.contains("ENOSPC", ignoreCase = true) == true -> "存储空间不足"
        error.message?.contains("权限") == true -> "没有存储权限"
        else -> error.message ?: "下载失败，请重试"
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 350L
    }
}