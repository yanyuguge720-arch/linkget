package com.linkget.data.parser

import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Kuaishou parser. Short links redirect to a photo page whose SSR state contains the
 * playable URL (photoUrl/srcNoMark/playUrl). Kuaishou changes field names frequently,
 * so the lookup is intentionally tolerant and walks the state tree for the first usable
 * video object.
 */
class KuaishouWorkParser(private val client: OkHttpClient) {

    suspend fun parse(shareText: String): ParsedWork = withContext(Dispatchers.IO) {
        val inputUrl = LinkText.extractUrl(shareText) ?: throw ParseException(ParseError.NO_LINK)
        if (LinkText.platformOf(inputUrl) != Platform.KUAISHOU) {
            throw ParseException(ParseError.UNSUPPORTED_HOST)
        }
        val finalUrl = resolve(inputUrl)
        val html = fetch(finalUrl)
        val state = extractState(html) ?: throw ParseException(ParseError.PARSE_FAILED)
        val photo = findPhoto(state) ?: throw ParseException(ParseError.CONTENT_UNAVAILABLE)
        val videoUrl = firstMediaUrl(photo)
        val images = readImages(photo)
        val resources = ArrayList<MediaResource>()
        if (videoUrl != null) {
            resources.add(
                MediaResource(
                    key = "video",
                    kind = ResourceKind.VIDEO,
                    url = videoUrl,
                    label = "视频",
                    quality = "原画",
                    durationMs = photo.optLong("duration", 0L).takeIf { it > 0 },
                    extension = "mp4",
                    index = 0
                )
            )
        }
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
            id = photo.optString("id").ifBlank { finalUrl.hashCode().toString() },
            platform = Platform.KUAISHOU,
            title = photo.optString("caption").ifBlank { photo.optString("title") }
                .ifBlank { "快手作品" }.replace(Regex("\\s+"), " ").take(300),
            author = photo.optString("userName").ifBlank {
                photo.optJSONObject("user")?.optString("name").orEmpty()
            }.takeIf { it.isNotBlank() },
            coverUrl = photo.optString("coverUrl").takeIf { it.startsWith("http") }
                ?: images.firstOrNull(),
            durationMs = photo.optLong("duration", 0L).takeIf { it > 0 },
            sourceUrl = inputUrl,
            resources = resources
        )
    }

    private fun resolve(url: String): String {
        if (!url.contains("v.kuaishou.com")) return url
        return try {
            val request = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "快手短链解析失败", e)
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.kuaishou.com/")
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
            throw ParseException(ParseError.NETWORK, "快手网络请求失败", e)
        }
    }

    private fun extractState(html: String): JSONObject? {
        for (marker in listOf("window.__APOLLO_STATE__", "window.__INITIAL_STATE__")) {
            val start = html.indexOf(marker)
            if (start < 0) continue
            val brace = html.indexOf('{', start)
            if (brace < 0) continue
            val json = balancedJson(html, brace) ?: continue
            runCatching { JSONObject(json.replace("undefined", "null")) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun findPhoto(root: JSONObject): JSONObject? {
        val queue = ArrayDeque<Any?>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 5000) {
            visited++
            val node = queue.removeFirst() ?: continue
            if (node is JSONObject) {
                if (firstMediaUrl(node) != null && (node.has("caption") || node.has("userName") || node.has("coverUrl"))) {
                    return node
                }
                val keys = node.keys()
                while (keys.hasNext()) queue.add(node.opt(keys.next()))
            } else if (node is JSONArray) {
                for (i in 0 until node.length()) queue.add(node.opt(i))
            }
        }
        return null
    }

    private fun firstMediaUrl(obj: JSONObject): String? {
        for (key in listOf("photoUrl", "playUrl", "srcNoMark", "videoUrl", "mainMvUrls")) {
            val value = obj.opt(key)
            when (value) {
                is String -> if (value.startsWith("http")) return value
                is JSONArray -> for (i in 0 until value.length()) {
                    val item = value.opt(i)
                    if (item is String && item.startsWith("http")) return item
                    if (item is JSONObject) {
                        val url = item.optString("url")
                        if (url.startsWith("http")) return url
                    }
                }
            }
        }
        return null
    }

    private fun readImages(photo: JSONObject): List<String> {
        val out = LinkedHashSet<String>()
        for (key in listOf("atlas", "images", "imageUrls")) {
            val array = photo.optJSONArray(key) ?: continue
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url").ifBlank { item.optString("cdn") }
                if (url.startsWith("http")) out.add(url)
            }
        }
        return out.take(100)
    }

    private fun balancedJson(text: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private companion object {
        const val UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    }
}