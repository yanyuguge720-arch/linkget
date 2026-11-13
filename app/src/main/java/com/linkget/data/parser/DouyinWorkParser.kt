package com.linkget.data.parser

import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import com.linkget.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Douyin share-link parser. The page is hydrated by SSR; two strategies tolerate the known
 * page shapes, and the canonical iesdouyin page is retried once to receive the ttwid cookie.
 */
class DouyinWorkParser(
    private val client: OkHttpClient,
    private val strategies: List<ParseStrategy> = listOf(RouterDataStrategy, RenderDataStrategy),
    private val settings: SettingsRepository? = null
) {

    suspend fun parse(shareText: String): ParsedWork = withContext(Dispatchers.IO) {
        if (shareText.isBlank()) throw ParseException(ParseError.EMPTY_INPUT)
        val inputUrl = ShareText.extractUrl(shareText)
            ?: throw ParseException(ParseError.NO_LINK)
        if (!ShareText.isDouyinUrl(inputUrl)) {
            throw ParseException(ParseError.UNSUPPORTED_HOST)
        }

        val firstPage = fetchOnce(inputUrl)
        val awemeId = ShareText.awemeId(firstPage.url) ?: ShareText.awemeId(inputUrl)

        // Cookie-free detail API: unlike slidesinfo it keeps the complete images[] array
        // and every image's live-photo video. Use it first for slides and notes.
        if (awemeId != null && (
                firstPage.url.contains("/slides/") || inputUrl.contains("/slides/") ||
                    firstPage.url.contains("/note/") || inputUrl.contains("/note/")
                )
        ) {
            runCatching { fetchDetail(awemeId, inputUrl) }.getOrNull()?.let { detail ->
                if (detail.imageUrls.isNotEmpty()) {
                    return@withContext finish(detail, inputUrl, awemeId)
                }
            }
            if (firstPage.url.contains("/slides/") || inputUrl.contains("/slides/")) {
                runCatching { fetchSlidesInfo(awemeId, inputUrl) }.getOrNull()?.let {
                    return@withContext finish(it, inputUrl, awemeId)
                }
            }
        }

        parseWithStrategies(firstPage.body, firstPage.url)?.let {
            return@withContext finish(it, inputUrl, awemeId)
        }

        var lastCanonical: FetchedPage? = null
        if (awemeId != null) {
            val preferred = when {
                firstPage.url.contains("/slides/") || inputUrl.contains("/slides/") -> "slides"
                firstPage.url.contains("/note/") || inputUrl.contains("/note/") -> "note"
                else -> "video"
            }
            val kinds = when (preferred) {
                "slides" -> listOf("slides", "note", "video")
                "note" -> listOf("note", "slides", "video")
                else -> listOf("video", "note")
            }
            for (kind in kinds) {
                val canonical = "https://www.iesdouyin.com/share/$kind/$awemeId/"
                val page = runCatching { fetchWithCookieRetry(canonical) }.getOrNull() ?: continue
                lastCanonical = page
                parseWithStrategies(page.body, firstPage.url)?.let {
                    return@withContext finish(it, inputUrl, awemeId)
                }
            }
        }

        val probe = lastCanonical?.body ?: firstPage.body
        val reason = when {
            looksLikeChallenge(probe) || looksLikeChallenge(firstPage.body) ->
                ParseError.CONTENT_UNAVAILABLE
            looksUnavailable(probe) || looksUnavailable(firstPage.body) ->
                ParseError.CONTENT_UNAVAILABLE
            else -> ParseError.PARSE_FAILED
        }
        throw ParseException(reason)
    }

    private fun finish(local: Aweme, sourceUrl: String, awemeId: String?): ParsedWork {
        if (awemeId != null && shouldTryCloud(local)) {
            runCatching { cloudParse(sourceUrl, awemeId) }.getOrNull()?.let { cloud ->
                // Never let a shorter cloud result hide local images; merge when needed.
                val base = if (cloud.imageUrls.size >= local.imageUrls.size) {
                    cloud
                } else {
                    mergeCloudImages(local, cloud)
                }
                // The cloud parser often drops the slideshow's background music.
                return base.copy(
                    author = base.author ?: local.author,
                    audioUrl = base.audioUrl ?: local.audioUrl,
                    durationMs = base.durationMs ?: local.durationMs,
                    musicTitle = base.musicTitle ?: local.musicTitle,
                    musicDurationMs = base.musicDurationMs ?: local.musicDurationMs
                ).toWork()
            }
        }
        return local.toWork()
    }

    private fun mergeCloudImages(local: Aweme, cloud: Aweme): Aweme {
        val cloudLives = cloud.imageUrls.mapIndexed { index, url ->
            imageKey(url) to cloud.imageLiveVideos.getOrNull(index)
        }.toMap()
        val images = local.imageUrls.toMutableList()
        val lives = local.imageUrls.map { cloudLives[imageKey(it)] }.toMutableList()
        cloud.imageUrls.forEachIndexed { index, url ->
            if (images.none { imageKey(it) == imageKey(url) }) {
                images.add(url)
                lives.add(cloud.imageLiveVideos.getOrNull(index))
            }
        }
        return cloud.copy(imageUrls = images, imageLiveVideos = lives)
    }

    private fun imageKey(url: String): String =
        url.substringBefore('?').substringBefore('#').substringAfterLast('/')

    private fun shouldTryCloud(local: Aweme): Boolean {
        if (settings?.state?.value?.cloudLiveFallback != true) return false
        if (local.imageUrls.isEmpty()) return false
        if (local.imageLiveVideos.any { !it.isNullOrBlank() }) return false
        val title = local.title.lowercase()
        return local.imageUrls.size == 1 ||
            title.contains("实况") ||
            title.contains("live") ||
            title.contains("动态照片") ||
            title.contains("动态壁纸")
    }

    /**
     * Douyin's public slidesinfo endpoint filters some note/live-photo works (reason 8).
     * When the local strategies only recover a single still image, optionally ask the
     * configured cloud parser for the image + live-photo video pair. The switch is
     * exposed in Settings and enabled by default.
     */
    private fun cloudParse(sourceUrl: String, awemeId: String): Aweme? {
        if (settings?.state?.value?.cloudLiveFallback != true) return null
        val body = JSONObject().put("text", sourceUrl).toString()
        val request = Request.Builder()
            .url("https://video.zacao.top/api/parse")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", MOBILE_UA)
            .header("Accept", "application/json")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val root = JSONObject(response.body?.string().orEmpty())
                if (root.optInt("code", -1) != 200) return null
                val data = root.optJSONObject("data") ?: return null
                val rawImages = data.optJSONArray("image_list")
                    ?: data.optJSONArray("source_image_list")
                val images = ArrayList<String>()
                val lives = ArrayList<String?>()
                if (rawImages != null) {
                    for (i in 0 until rawImages.length()) {
                        when (val item = rawImages.opt(i)) {
                            is String -> {
                                if (item.startsWith("http")) {
                                    images.add(item)
                                    lives.add(null)
                                }
                            }
                            is JSONObject -> {
                                val url = item.optString("url")
                                if (url.startsWith("http")) {
                                    images.add(url)
                                    lives.add(item.optString("live_photo_url").takeIf { it.startsWith("http") })
                                }
                            }
                        }
                    }
                }
                val video = data.optString("video_url").takeIf { it.startsWith("http") }
                    ?: data.optString("source_video_url").takeIf { it.startsWith("http") }
                if (images.isEmpty() && video == null) return null
                val author = when (val a = data.opt("author")) {
                    is String -> a
                    is JSONObject -> a.optString("nickname").ifBlank { a.optString("name") }
                    else -> ""
                }.takeIf { it.isNotBlank() }
                Aweme(
                    awemeId = awemeId,
                    title = data.optString("title").ifBlank { "抖音作品" }.replace(Regex("\\s+"), " ").take(300),
                    author = author,
                    coverUrl = data.optString("cover_url").takeIf { it.startsWith("http") },
                    videoUrl = video,
                    imageUrls = images,
                    audioUrl = data.optString("audio_url").takeIf { it.startsWith("http") },
                    durationMs = null,
                    sourceUrl = sourceUrl,
                    imageLiveVideos = lives
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun Aweme.toWork(): ParsedWork {
        val resources = buildList {
            // Image posts carry a generated slideshow video; only expose that video for real videos.
            videoUrl?.takeIf { imageUrls.isEmpty() }?.let { url ->
                add(
                    MediaResource(
                        key = "video",
                        kind = ResourceKind.VIDEO,
                        url = url,
                        label = "视频",
                        quality = qualityOf(url),
                        durationMs = durationMs,
                        extension = "mp4",
                        index = 0
                    )
                )
            }
            coverUrl?.takeIf { imageUrls.isEmpty() && videoUrl != null }?.let { url ->
                add(
                    MediaResource(
                        key = "cover",
                        kind = ResourceKind.IMAGE,
                        url = url,
                        label = "封面",
                        extension = "jpg",
                        index = 0
                    )
                )
            }
            imageUrls.forEachIndexed { index, url ->
                val liveVideo = imageLiveVideos.getOrNull(index)
                add(
                    MediaResource(
                        key = "image:" + index,
                        kind = ResourceKind.IMAGE,
                        url = url,
                        label = "图片 " + (index + 1),
                        extension = "jpg",
                        liveVideoUrl = liveVideo,
                        index = index + 1
                    )
                )
                if (!liveVideo.isNullOrBlank()) {
                    add(
                        MediaResource(
                            key = "live:" + index,
                            kind = ResourceKind.VIDEO,
                            url = liveVideo,
                            label = "实况动态 " + (index + 1),
                            quality = "实况",
                            extension = "mp4",
                            liveMotion = true,
                            index = index + 1
                        )
                    )
                }
            }
            audioUrl?.let { url ->
                add(
                    MediaResource(
                        key = "audio",
                        kind = ResourceKind.AUDIO,
                        url = url,
                        label = "背景音乐",
                        durationMs = musicDurationMs ?: durationMs,
                        extension = "mp3",
                        index = 0
                    )
                )
            }
        }
        if (resources.isEmpty()) throw ParseException(ParseError.NO_RESOURCES)
        return ParsedWork(
            id = awemeId,
            platform = Platform.DOUYIN,
            title = title,
            author = author,
            coverUrl = coverUrl,
            durationMs = durationMs,
            sourceUrl = sourceUrl,
            resources = resources
        )
    }

    private fun qualityOf(url: String): String? {
        val ratio = Regex("[?&]ratio=(\\d+)p").find(url)?.groupValues?.getOrNull(1)
        if (ratio != null) return ratio + "P"
        return when {
            url.contains("1080") -> "1080P"
            url.contains("720") -> "720P"
            else -> "高清"
        }
    }

    /** Cookie-free, signature-free detail endpoint used by several open-source parsers. */
    private fun fetchDetail(awemeId: String, sourceUrl: String): Aweme? {
        val url = "https://api.amemv.com/aweme/v1/aweme/detail/" +
            "?aweme_id=" + awemeId + "&aid=6383&device_platform=web" +
            "&version_code=190500&version_name=19.5.0"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DETAIL_UA)
            .header("Accept", "application/json, text/plain, */*")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val root = JSONObject(response.body?.string().orEmpty())
                if (root.optInt("status_code", -1) != 0) return null
                val detail = root.optJSONObject("aweme_detail") ?: return null
                AwemeJson.fromItem(detail, sourceUrl)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchSlidesInfo(awemeId: String, sourceUrl: String): Aweme? {
        val url = "https://www.iesdouyin.com/web/api/v2/aweme/slidesinfo/" +
            "?aweme_ids=%5B" + awemeId + "%5D&aweme_type=0&aid=1128&request_source=200"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://www.douyin.com/")
            .header("Accept", "application/json, text/plain, */*")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
                if (root.optInt("status_code", 0) != 0) return null
                val details = root.optJSONArray("aweme_details") ?: return null
                if (details.length() == 0) return null
                val item = details.optJSONObject(0) ?: return null
                AwemeJson.fromItem(item, sourceUrl)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWithStrategies(html: String, sourceUrl: String): Aweme? {
        for (strategy in strategies) {
            strategy.parse(html, sourceUrl)?.let { return it }
        }
        return null
    }

    private fun fetchWithCookieRetry(url: String): FetchedPage {
        var page = fetchOnce(url)
        // The first request only sets ttwid. Depending on the edge node, the hydrated
        // payload can take one or two cookie-bearing requests to appear.
        repeat(3) {
            if (hasMediaPayload(page.body)) return page
            page = runCatching { fetchOnce(url) }.getOrNull() ?: return page
        }
        return page
    }

    private fun looksUnavailable(html: String): Boolean =
        html.contains("filter_reason") ||
            html.contains("status_self_see") ||
            html.contains("作品不见了") ||
            html.contains("无法观看")

    private fun hasMediaPayload(html: String): Boolean =
        html.contains("videoInfoRes") ||
            html.contains("item_list") ||
            html.contains("play_addr") ||
            html.contains("playwm")

    private fun fetchOnce(url: String): FetchedPage {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.6")
            .header("Referer", "https://www.douyin.com/")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 || response.code == 410 ->
                        throw ParseException(ParseError.LINK_EXPIRED)
                    response.code == 403 || response.code == 412 || response.code == 429 ->
                        throw ParseException(ParseError.CONTENT_UNAVAILABLE)
                    !response.isSuccessful ->
                        throw ParseException(ParseError.NETWORK, "HTTP " + response.code)
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) throw ParseException(ParseError.PARSE_FAILED)
                return FetchedPage(response.request.url.toString(), body)
            }
        } catch (e: ParseException) {
            throw e
        } catch (e: IOException) {
            throw ParseException(ParseError.NETWORK, "网络请求失败", e)
        }
    }

    private fun looksLikeChallenge(html: String): Boolean {
        val lower = html.lowercase()
        return listOf(
            "验证码", "滑块", "访问过于频繁", "安全验证",
            "captcha-container", "secsdk-captcha", "verify-center", "verify_data"
        ).any(lower::contains)
    }

    private data class FetchedPage(val url: String, val body: String)

    private companion object {
        const val MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 " +
                "Mobile/15E148 Safari/604.1"
        const val DETAIL_UA =
            "Mozilla/5.0 (Linux; Android 13; 2210132C Build/TKQ1.220829.002) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
    }
}