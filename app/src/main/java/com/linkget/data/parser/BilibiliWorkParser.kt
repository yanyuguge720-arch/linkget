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
import java.util.LinkedHashMap

/**
 * Bilibili parser built on the public web APIs.
 *
 * - Combined durl streams give 720P / 360P MP4s without a login and need no remux.
 * - DASH streams are also inspected: when a higher quality is available it is offered with
 *   needMerge=true, and the app muxes the selected audio track after download.
 */
class BilibiliWorkParser(
    private val client: OkHttpClient,
    private val settings: com.linkget.data.settings.SettingsRepository? = null
) {

    suspend fun parse(shareText: String): ParsedWork = withContext(Dispatchers.IO) {
        if (shareText.isBlank()) throw ParseException(ParseError.EMPTY_INPUT)
        val inputUrl = LinkText.extractUrl(shareText) ?: throw ParseException(ParseError.NO_LINK)
        if (!LinkText.isBilibiliUrl(inputUrl)) throw ParseException(ParseError.UNSUPPORTED_HOST)

        val resolved = resolve(inputUrl)
        var bvid = LinkText.bilibiliId(resolved) ?: LinkText.bilibiliId(inputUrl)
        var aid: Long? = null
        if (bvid == null) throw ParseException(ParseError.LINK_EXPIRED)
        if (bvid.startsWith("av", ignoreCase = true)) {
            aid = bvid.drop(2).toLongOrNull()
            bvid = null
        }

        val viewUrl = if (bvid != null) {
            "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid
        } else {
            "https://api.bilibili.com/x/web-interface/view?aid=" + aid
        }
        val view = getJson(viewUrl)
        if (view.optInt("code", -1) != 0) {
            throw ParseException(ParseError.CONTENT_UNAVAILABLE, view.optString("message"))
        }
        val data = view.optJSONObject("data")
            ?: throw ParseException(ParseError.PARSE_FAILED)
        val realBvid = data.optString("bvid").takeIf { it.isNotBlank() } ?: bvid ?: "av" + aid
        val pages = data.optJSONArray("pages")
        val firstPage = pages?.optJSONObject(0)
        val cid = firstPage?.optLong("cid", 0L)?.takeIf { it > 0 }
            ?: data.optLong("cid", 0L).takeIf { it > 0 }
            ?: throw ParseException(ParseError.PARSE_FAILED)
        val durationSec = data.optLong("duration", 0L).takeIf { it > 0 }
            ?: firstPage?.optLong("duration", 0L)?.takeIf { it > 0 }
        val durationMs = durationSec?.times(1000)
        val pageCount = pages?.length() ?: 1
        val part = firstPage?.optString("part").orEmpty()
        val title = data.optString("title").ifBlank { "B 站视频" }
        val author = data.optJSONObject("owner")?.optString("name")?.takeIf { it.isNotBlank() }
        val cover = data.optString("pic").takeIf { it.isNotBlank() }
            ?.replace("http://", "https://")

        val resources = LinkedHashMap<String, MediaResource>()

        // 1) Combined MP4 streams. 720P usually works without cookies.
        for (qn in intArrayOf(64, 32, 16)) {
            val info = playUrl(realBvid, cid, qn, fnval = 0) ?: continue
            val quality = qualityLabel(info.optInt("quality", qn))
            val key = "video:" + quality
            if (resources.containsKey(key)) continue
            val durl = info.optJSONArray("durl") ?: continue
            val first = durl.optJSONObject(0) ?: continue
            val url = first.optString("url").takeIf { it.startsWith("http") } ?: continue
            resources[key] = MediaResource(
                key = key,
                kind = ResourceKind.VIDEO,
                url = url,
                label = if (pageCount > 1 && part.isNotBlank()) part else "视频",
                quality = quality,
                durationMs = durationMs ?: first.optLong("length", 0L).takeIf { it > 0 },
                sizeBytes = first.optLong("size", 0L).takeIf { it > 0 },
                extension = "mp4",
                index = 0
            )
        }

        // 2) DASH streams: offer higher qualities and the standalone audio track.
        val dashInfo = playUrl(realBvid, cid, 80, fnval = 16)
        val dash = dashInfo?.optJSONObject("dash")
        var bestAudioUrl: String? = null
        dash?.optJSONArray("audio")?.let { audios ->
            var bestBandwidth = -1L
            for (i in 0 until audios.length()) {
                val audio = audios.optJSONObject(i) ?: continue
                val url = audio.optString("baseUrl").ifBlank { audio.optString("base_url") }
                if (!url.startsWith("http")) continue
                val bandwidth = audio.optLong("bandwidth", 0L)
                if (bandwidth > bestBandwidth) {
                    bestBandwidth = bandwidth
                    bestAudioUrl = url
                }
            }
        }
        bestAudioUrl?.let { url ->
            resources["audio"] = MediaResource(
                key = "audio",
                kind = ResourceKind.AUDIO,
                url = url,
                label = "音频轨道",
                durationMs = durationMs,
                extension = "m4a",
                index = 0
            )
        }

        dash?.optJSONArray("video")?.let { videos ->
            val bestPerQuality = LinkedHashMap<Int, JSONObject>()
            for (i in 0 until videos.length()) {
                val video = videos.optJSONObject(i) ?: continue
                val codecid = video.optInt("codecid", 0)
                // Prefer H.264 for broad device compatibility; fall back to whatever exists.
                if (codecid != 0 && codecid != 7) continue
                val qn = video.optInt("id", 0)
                val previous = bestPerQuality[qn]
                if (previous == null || video.optLong("bandwidth", 0L) > previous.optLong("bandwidth", 0L)) {
                    bestPerQuality[qn] = video
                }
            }
            for ((qn, video) in bestPerQuality) {
                val quality = qualityLabel(qn)
                val key = "video:" + quality
                if (resources.containsKey(key)) continue
                val url = video.optString("baseUrl").ifBlank { video.optString("base_url") }
                if (!url.startsWith("http")) continue
                resources[key] = MediaResource(
                    key = key,
                    kind = ResourceKind.VIDEO,
                    url = url,
                    label = if (pageCount > 1 && part.isNotBlank()) part else "视频",
                    quality = quality,
                    durationMs = durationMs,
                    needMerge = bestAudioUrl != null,
                    audioUrl = bestAudioUrl,
                    extension = "mp4",
                    index = 0
                )
            }
        }

        if (resources.isEmpty()) throw ParseException(ParseError.NO_RESOURCES)

        ParsedWork(
            id = realBvid,
            platform = Platform.BILIBILI,
            title = if (pageCount > 1 && part.isNotBlank()) title + " · " + part else title,
            author = author,
            coverUrl = cover,
            durationMs = durationMs,
            sourceUrl = inputUrl,
            resources = resources.values.sortedWith(
                compareBy<MediaResource> { it.kind != ResourceKind.VIDEO }
                    .thenByDescending { qualityRank(it.quality) }
                    .thenBy { it.kind != ResourceKind.AUDIO }
            )
        )
    }

    private fun resolve(url: String): String {
        if (!url.contains("b23.tv")) return url
        return try {
            val request = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "短链接解析失败", e)
        }
    }

    private fun playUrl(bvid: String, cid: Long, qn: Int, fnval: Int): JSONObject? {
        // HTML5 + high_quality returns the same 720P MP4 on a regular bilivideo CDN.
        // The default PC endpoint can hand back a PCDN host that rejects direct downloads.
        val extra = if (fnval == 0) "&platform=html5&high_quality=1" else ""
        val url = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid +
            "&cid=" + cid + "&qn=" + qn + "&fnval=" + fnval + "&fourk=0" + extra
        val json = runCatching { getJson(url) }.getOrNull() ?: return null
        if (json.optInt("code", -1) != 0) return null
        return json.optJSONObject("data")
    }

    private fun getJson(url: String): JSONObject {
        val cookie = settings?.state?.value?.bilibiliCookie.orEmpty()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .header("Accept", "application/json, text/plain, */*")
            .apply { if (cookie.isNotBlank()) header("Cookie", cookie) }
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) throw ParseException(ParseError.LINK_EXPIRED)
                if (response.code == 403 || response.code == 412 || response.code == 429) {
                    throw ParseException(ParseError.CONTENT_UNAVAILABLE)
                }
                if (!response.isSuccessful) {
                    throw ParseException(ParseError.NETWORK, "HTTP " + response.code)
                }
                val body = response.body?.string().orEmpty()
                return runCatching { JSONObject(body) }.getOrElse {
                    throw ParseException(ParseError.PARSE_FAILED)
                }
            }
        } catch (e: ParseException) {
            throw e
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "网络请求失败", e)
        }
    }

    private fun qualityLabel(qn: Int): String = when (qn) {
        127 -> "8K"
        126 -> "杜比视界"
        125 -> "HDR"
        120 -> "4K"
        116 -> "1080P60"
        112 -> "1080P+"
        80 -> "1080P"
        74 -> "720P60"
        64 -> "720P"
        32 -> "480P"
        16 -> "360P"
        else -> qn.toString() + "P"
    }

    private fun qualityRank(quality: String?): Int = when (quality) {
        "8K" -> 100
        "杜比视界" -> 95
        "HDR" -> 94
        "4K" -> 90
        "1080P60" -> 86
        "1080P+" -> 85
        "1080P" -> 80
        "720P60" -> 76
        "720P" -> 70
        "480P" -> 40
        "360P" -> 30
        else -> 0
    }

    private companion object {
        const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
