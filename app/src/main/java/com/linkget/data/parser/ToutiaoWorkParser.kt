package com.linkget.data.parser

import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder

/**
 * Toutiao video parser. The mobile page embeds a URL-encoded RENDER_DATA payload with
 * articleInfo.playAuthTokenV2. Decoding that token gives a VOD GetPlayInfo request whose
 * PlayInfoList contains 360p/480p/720p/1080p MP4 URLs.
 */
class ToutiaoWorkParser(private val client: OkHttpClient) {

    suspend fun parse(shareText: String): ParsedWork = withContext(Dispatchers.IO) {
        val inputUrl = LinkText.extractUrl(shareText) ?: throw ParseException(ParseError.NO_LINK)
        if (LinkText.platformOf(inputUrl) != Platform.TOUTIAO) {
            throw ParseException(ParseError.UNSUPPORTED_HOST)
        }
        val finalUrl = resolve(inputUrl)
        val html = fetch(finalUrl)
        val data = extractRenderData(html) ?: throw ParseException(ParseError.PARSE_FAILED)
        val info = data.optJSONObject("articleInfo") ?: throw ParseException(ParseError.PARSE_FAILED)
        val title = info.optString("title").ifBlank { "今日头条视频" }
        val author = info.optString("detailSource").ifBlank {
            info.optJSONObject("mediaUser")?.optString("screenName").orEmpty()
        }.takeIf { it.isNotBlank() }
        val duration = info.optLong("videoDuration", 0L).takeIf { it > 0 }?.times(1000)
        val cover = info.optString("posterUrl").takeIf { it.startsWith("http") }
        val resources = ArrayList<MediaResource>()
        val token = decodePlayToken(info.optString("playAuthTokenV2"))
        if (token != null) {
            fetchPlayInfo(token).forEachIndexed { index, item ->
                val url = item.optString("MainPlayUrl")
                if (!url.startsWith("http")) return@forEachIndexed
                val definition = item.optString("Definition")
                val quality = definition.uppercase()
                resources.add(
                    MediaResource(
                        key = "video:" + quality,
                        kind = ResourceKind.VIDEO,
                        url = url,
                        label = "视频",
                        quality = quality.ifBlank { "原画" },
                        durationMs = (item.optDouble("Duration", 0.0) * 1000).toLong()
                            .takeIf { it > 0 } ?: duration,
                        sizeBytes = item.optLong("Size", 0L).takeIf { it > 0 },
                        extension = "mp4",
                        index = index
                    )
                )
            }
        }
        // Article posts can contain still images alongside or instead of a video.
        val images = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(info.optString("content"))
            .map { it.groupValues[1] }
            .filter { it.startsWith("http") }
            .distinct()
            .take(60)
            .toList()
        images.forEachIndexed { index, url ->
            resources.add(
                MediaResource(
                    key = "image:" + index,
                    kind = ResourceKind.IMAGE,
                    url = url,
                    label = "图片 " + (index + 1),
                    extension = "jpg",
                    index = index + 1
                )
            )
        }
        if (resources.isEmpty()) throw ParseException(ParseError.NO_RESOURCES)
        ParsedWork(
            id = info.optString("gid").ifBlank { finalUrl.hashCode().toString() },
            platform = Platform.TOUTIAO,
            title = title.replace(Regex("\\s+"), " ").take(300),
            author = author,
            coverUrl = cover ?: images.firstOrNull(),
            durationMs = duration,
            sourceUrl = inputUrl,
            resources = resources.sortedWith(
                compareByDescending<MediaResource> { qualityRank(it.quality) }
                    .thenBy { it.kind != ResourceKind.VIDEO }
            )
        )
    }

    private fun resolve(url: String): String {
        if (!url.contains("m.toutiao.com/is/")) return url
        return try {
            val request = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "今日头条短链解析失败", e)
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.toutiao.com/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ParseException(ParseError.NETWORK, "HTTP " + response.code)
                return response.body?.string().orEmpty()
            }
        } catch (e: ParseException) {
            throw e
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "今日头条网络请求失败", e)
        }
    }

    private fun extractRenderData(html: String): JSONObject? {
        val match = Regex("""<script[^>]*id=["']RENDER_DATA["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html) ?: return null
        return runCatching {
            JSONObject(URLDecoder.decode(match.groupValues[1], "UTF-8"))
        }.getOrNull()
    }

    private fun decodePlayToken(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val decoded = String(java.util.Base64.getDecoder().decode(raw), Charsets.UTF_8)
            JSONObject(decoded).optString("GetPlayInfoToken").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun fetchPlayInfo(token: String): List<JSONObject> {
        val request = Request.Builder()
            .url("https://vod.bytedanceapi.com/?" + token)
            .header("User-Agent", UA)
            .header("Referer", "https://www.toutiao.com/")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ParseException(ParseError.NETWORK, "VOD HTTP " + response.code)
                val json = JSONObject(response.body?.string().orEmpty())
                val list = json.optJSONObject("Result")?.optJSONObject("Data")
                    ?.optJSONArray("PlayInfoList") ?: return emptyList()
                return (0 until list.length()).mapNotNull { list.optJSONObject(it) }
            }
        } catch (e: ParseException) {
            throw e
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "头条视频信息请求失败", e)
        } catch (e: Exception) {
            throw ParseException(ParseError.PARSE_FAILED, "头条视频信息解析失败", e)
        }
    }

    private fun qualityRank(quality: String?): Int = when (quality) {
        "1080P" -> 80
        "720P" -> 70
        "480P" -> 40
        "360P" -> 30
        else -> 0
    }

    private companion object {
        const val UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    }
}