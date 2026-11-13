package com.linkget.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Minimal OkHttp-backed image loader with a byte-bounded LRU cache. */
class ImageLoader(private val client: OkHttpClient) {

    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(url: String, maxDimension: Int = 1440): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val key = url + "@" + maxDimension
            cache.get(key)?.let { return@withContext it.asImageBitmap() }
            try {
                val referer = when {
                    url.contains("hdslb.com") || url.contains("bilibili.com") ->
                        "https://www.bilibili.com/"
                    url.contains("xhscdn.com") || url.contains("xiaohongshu.com") ->
                        "https://www.xiaohongshu.com/"
                    url.contains("kuaishou.com") || url.contains("kwaicdn.com") ||
                        url.contains("chenzhongtech.com") -> "https://www.kuaishou.com/"
                    url.contains("toutiao.com") || url.contains("toutiaoimg.com") ||
                        url.contains("toutiaostatic.com") -> "https://www.toutiao.com/"
                    else -> "https://www.douyin.com/"
                }
                val request = Request.Builder()
                    .url(url)
                    .header("Referer", referer)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sampleSize = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
                        sampleSize *= 2
                    }
                    val bitmap = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    ) ?: return@withContext null
                    cache.put(key, bitmap)
                    bitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
