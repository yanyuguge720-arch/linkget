package com.linkget.data.parser

import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * Adapter for the PRD's backend contract:
 * POST /api/parse { "url": "..." } -> { code, msg, data: { title, author, cover, resource_list } }
 *
 * The app does not require the service: local Douyin / Bilibili parsing stays available.
 */
class ApiWorkParser(private val client: OkHttpClient) {

    suspend fun parse(shareText: String, baseUrl: String): ParsedWork = withContext(Dispatchers.IO) {
        if (shareText.isBlank()) throw ParseException(ParseError.EMPTY_INPUT)
        val endpoint = if (baseUrl.trimEnd('/').endsWith("/api/parse")) {
            baseUrl.trim()
        } else {
            baseUrl.trimEnd('/') + "/api/parse"
        }
        val payload = JSONObject().put("url", shareText.trim()).toString()
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ParseException(ParseError.API_ERROR, "HTTP " + response.code)
                }
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrElse {
                    throw ParseException(ParseError.API_ERROR, "接口返回格式异常")
                }
                val code = json.optInt("code", -1)
                if (code != 200) {
                    throw ParseException(
                        ParseError.PARSE_FAILED,
                        json.optString("msg").takeIf { it.isNotBlank() }
                    )
                }
                val data = json.optJSONObject("data")
                    ?: throw ParseException(ParseError.API_ERROR, "接口没有返回数据")
                val resources = ArrayList<MediaResource>()
                val seen = HashSet<String>()
                fun add(resource: MediaResource) {
                    if (resource.url.isBlank()) return
                    if (!seen.add(resource.key)) return
                    resources.add(resource)
                }

                data.optJSONArray("resource_list")?.let { list ->
                    for (i in 0 until list.length()) {
                        val item = list.optJSONObject(i) ?: continue
                        val type = item.optString("type").lowercase(Locale.ROOT)
                        val kind = when {
                            type == "video" -> ResourceKind.VIDEO
                            type == "audio" -> ResourceKind.AUDIO
                            else -> ResourceKind.IMAGE
                        }
                        val quality = item.optString("quality").takeIf { it.isNotBlank() }
                        val url = item.optString("download_url")
                            .ifBlank { item.optString("url") }
                        if (url.isBlank()) continue
                        val index = if (kind == ResourceKind.IMAGE) i + 1 else 0
                        add(
                            MediaResource(
                                key = kind.name.lowercase() + ":" + i,
                                kind = kind,
                                url = url,
                                label = when (kind) {
                                    ResourceKind.VIDEO -> if (quality != null) "视频 · " + quality else "视频"
                                    ResourceKind.AUDIO -> "音频"
                                    ResourceKind.IMAGE -> "图片 " + index
                                },
                                quality = quality,
                                durationMs = parseDuration(item.optString("duration")),
                                sizeBytes = parseSize(item.optString("size")),
                                extension = extensionFor(url, kind),
                                needMerge = item.optBoolean("need_merge", false),
                                audioUrl = item.optString("audio_url").takeIf { it.startsWith("http") },
                                index = index
                            )
                        )
                    }
                }

                // Some backends also return a plain images array.
                data.optJSONArray("images")?.let { images ->
                    for (i in 0 until images.length()) {
                        val url = images.optString(i)
                        if (!url.startsWith("http")) continue
                        add(
                            MediaResource(
                                key = "image_list:" + i,
                                kind = ResourceKind.IMAGE,
                                url = url,
                                label = "图片 " + (i + 1),
                                extension = extensionFor(url, ResourceKind.IMAGE),
                                index = i + 1
                            )
                        )
                    }
                }

                if (resources.isEmpty()) throw ParseException(ParseError.NO_RESOURCES)
                val sourceUrl = LinkText.extractUrl(shareText).orEmpty()
                ParsedWork(
                    id = data.optString("id").takeIf { it.isNotBlank() }
                        ?: Integer.toHexString(shareText.hashCode()),
                    platform = LinkText.platformOf(sourceUrl),
                    title = data.optString("title").ifBlank { "解析结果" },
                    author = data.optString("author").takeIf { it.isNotBlank() },
                    coverUrl = data.optString("cover").takeIf { it.startsWith("http") },
                    durationMs = parseDuration(data.optString("duration")),
                    sourceUrl = sourceUrl,
                    resources = resources
                )
            }
        } catch (e: ParseException) {
            throw e
        } catch (e: IOException) {
            throw ParseException(ParseError.API_ERROR, "解析服务连接失败", e)
        }
    }

    private fun parseDuration(raw: String): Long? {
        if (raw.isBlank()) return null
        raw.toLongOrNull()?.let { return if (it < 10_000) it * 1000 else it }
        val parts = raw.split(":").mapNotNull { it.trim().toLongOrNull() }
        if (parts.isEmpty()) return null
        var seconds = 0L
        for (part in parts) seconds = seconds * 60 + part
        return seconds * 1000
    }

    private fun parseSize(raw: String): Long? {
        if (raw.isBlank()) return null
        val match = Regex("([0-9.]+)\\s*(B|KB|MB|GB|TB)", RegexOption.IGNORE_CASE).find(raw)
            ?: return raw.toLongOrNull()
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].uppercase(Locale.ROOT)) {
            "TB" -> 1024.0 * 1024 * 1024 * 1024
            "GB" -> 1024.0 * 1024 * 1024
            "MB" -> 1024.0 * 1024
            "KB" -> 1024.0
            else -> 1.0
        }
        return (value * multiplier).toLong()
    }

    private fun extensionFor(url: String, kind: ResourceKind): String? {
        val raw = url.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (kind) {
            ResourceKind.VIDEO -> if (raw in setOf("mp4", "webm", "mov", "m4v")) raw else "mp4"
            ResourceKind.AUDIO -> if (raw in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")) raw else "m4a"
            ResourceKind.IMAGE -> if (raw in setOf("jpg", "jpeg", "png", "webp", "gif")) raw else "jpg"
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
