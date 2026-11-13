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
 * Xiaohongshu parser. Share links carry an xsec_token and render a
 * window.__INITIAL_STATE__ payload; we read that directly so videos and albums both work.
 * Without a valid token XHS returns an empty note, which is surfaced as a useful error.
 */
class XiaohongshuWorkParser(private val client: OkHttpClient) {

    suspend fun parse(shareText: String): ParsedWork = withContext(Dispatchers.IO) {
        val inputUrl = LinkText.extractUrl(shareText) ?: throw ParseException(ParseError.NO_LINK)
        if (LinkText.platformOf(inputUrl) != Platform.XIAOHONGSHU) {
            throw ParseException(ParseError.UNSUPPORTED_HOST)
        }
        val finalUrl = resolve(inputUrl)
        val html = fetch(finalUrl)
        val state = extractState(html) ?: throw ParseException(ParseError.PARSE_FAILED)
        val note = findNote(state) ?: throw ParseException(ParseError.CONTENT_UNAVAILABLE)
        val title = note.optString("title").ifBlank { note.optString("desc") }.ifBlank { "小红书笔记" }
        val author = note.optJSONObject("user")?.optString("nickname")?.takeIf { it.isNotBlank() }
        val images = readImages(note)
        val video = readVideo(note)
        val cover = note.optJSONObject("cover")?.let { imageUrl(it) }
            ?: note.optJSONObject("imageList")?.let { null }
            ?: images.firstOrNull()
        val resources = ArrayList<MediaResource>()
        if (video != null) {
            resources.add(
                MediaResource(
                    key = "video",
                    kind = ResourceKind.VIDEO,
                    url = video,
                    label = "视频",
                    quality = "原画",
                    durationMs = note.optJSONObject("video")?.optJSONObject("capa")
                        ?.optLong("duration", 0L)?.takeIf { it > 0 },
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
            id = note.optString("noteId").ifBlank { note.optString("note_id") }
                .ifBlank { finalUrl.hashCode().toString() },
            platform = Platform.XIAOHONGSHU,
            title = title.replace(Regex("\\s+"), " ").take(300),
            author = author,
            coverUrl = cover,
            durationMs = null,
            sourceUrl = inputUrl,
            resources = resources
        )
    }

    private fun resolve(url: String): String {
        if (!url.contains("xhslink.com")) return url
        return try {
            val request = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "小红书短链解析失败", e)
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.xiaohongshu.com/")
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
            throw ParseException(ParseError.NETWORK, "小红书网络请求失败", e)
        }
    }

    private fun extractState(html: String): JSONObject? {
        val marker = "window.__INITIAL_STATE__"
        val start = html.indexOf(marker)
        if (start < 0) return null
        val brace = html.indexOf('{', start)
        if (brace < 0) return null
        val json = balancedJson(html, brace) ?: return null
        return runCatching {
            JSONObject(json.replace("undefined", "null"))
        }.getOrNull()
    }

    private fun findNote(root: JSONObject): JSONObject? {
        val noteData = root.optJSONObject("noteData") ?: return null
        noteData.optJSONObject("data")?.takeIf { it.length() > 0 }?.let { return it }
        noteData.optJSONObject("normalNotePreloadData")?.let { preload ->
            preload.optJSONObject("note")?.let { return it }
            if (preload.has("imageList") || preload.has("video")) return preload
        }
        val queue = ArrayDeque<Any?>()
        queue.add(noteData)
        var visited = 0
        while (queue.isNotEmpty() && visited < 3000) {
            visited++
            val node = queue.removeFirst() ?: continue
            if (node is JSONObject) {
                if (node.has("imageList") || node.has("video") || node.has("noteId")) {
                    if (node.has("imageList") || node.has("video")) return node
                }
                val keys = node.keys()
                while (keys.hasNext()) queue.add(node.opt(keys.next()))
            } else if (node is JSONArray) {
                for (i in 0 until node.length()) queue.add(node.opt(i))
            }
        }
        return null
    }

    private fun readImages(note: JSONObject): List<String> {
        val out = LinkedHashSet<String>()
        note.optJSONArray("imageList")?.let { list ->
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                imageUrl(item)?.let(out::add)
            }
        }
        note.optJSONArray("images")?.let { list ->
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                imageUrl(item)?.let(out::add)
            }
        }
        return out.filter { it.startsWith("https://") }.take(100)
    }

    private fun imageUrl(obj: JSONObject): String? {
        for (key in listOf("urlDefault", "url", "urlPre", "cover")) {
            obj.optString(key).takeIf { it.startsWith("http") }?.let { return it }
        }
        obj.optJSONArray("infoList")?.let { list ->
            for (i in 0 until list.length()) {
                val url = list.optJSONObject(i)?.optString("url")
                if (url?.startsWith("http") == true) return url
            }
        }
        return null
    }

    private fun readVideo(note: JSONObject): String? {
        val video = note.optJSONObject("video") ?: return null
        val stream = video.optJSONObject("media")?.optJSONObject("stream")
        for (codec in listOf("h264", "h265", "av1")) {
            val array = stream?.optJSONArray(codec) ?: continue
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("masterUrl")
                if (url.startsWith("http")) return url
            }
        }
        val key = video.optJSONObject("consumer")?.optString("originVideoKey")
        if (!key.isNullOrBlank()) return "https://sns-video-bd.xhscdn.com/" + key
        return null
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