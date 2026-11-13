package com.linkget.data.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadException(
    message: String,
    val statusCode: Int = 0,
    cause: Throwable? = null
) : IOException(message, cause)

/** Downloads one URL with HTTP Range resume, retries and caller-supplied headers. */
class ResumableFileDownloader(
    private val client: OkHttpClient,
    private val maxAttempts: Int = 3
) {

    suspend fun download(
        url: String,
        target: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        for (attempt in 0 until maxAttempts) {
            try {
                return@withContext downloadOnce(url, target, headers, onProgress)
            } catch (e: IOException) {
                lastError = e
                if (e is DownloadException && e.statusCode in 400..499) throw e
                if (attempt < maxAttempts - 1) delay(400L * (attempt + 1))
            }
        }
        throw lastError ?: IOException("下载失败")
    }

    private suspend fun downloadOnce(
        url: String,
        target: File,
        headers: Map<String, String>,
        onProgress: (Long, Long) -> Unit
    ): File {
        val existing = if (target.exists()) target.length() else 0L
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Encoding", "identity")
            .apply {
                headers.forEach { (key, value) -> header(key, value) }
                if (existing > 0) header("Range", "bytes=" + existing + "-")
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 416 && existing > 0) return target
            if (response.code !in 200..299) {
                throw DownloadException("HTTP " + response.code, response.code)
            }
            val body = response.body ?: throw IOException("服务器返回空内容")
            val append = existing > 0 && response.code == 206
            if (!append && target.exists()) target.delete()

            val length = body.contentLength()
            val total = when {
                append && length > 0 -> existing + length
                append -> 0L
                else -> length.coerceAtLeast(0L)
            }
            var written = if (append) existing else 0L

            target.parentFile?.mkdirs()
            FileOutputStream(target, append).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        onProgress(written, total)
                    }
                }
            }
            if (total > 0 && written != total) {
                throw IOException("传输中断：" + written + "/" + total)
            }
            return target
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
