package com.linkget.data.parser

import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns a Douyin item JSON object into Aweme. Douyin uses several field spellings
 * (play_addr / playAddr, aweme_id / awemeId) depending on the page flavour, so the
 * extractors are deliberately tolerant.
 */
object AwemeJson {

    fun fromItem(item: JSONObject, sourceUrl: String): Aweme? {
        val video = item.optJSONObject("video")
        val (images, liveVideos) = parseImages(item)
        val videoUrl = video?.let {
            findUrl(it, "play_addr", "playAddr", "play_addr_h264", "play_addr_265", "download_addr", "playApi")
        }?.let(::normalizeMediaUrl)
        val cover = video?.let {
            findUrl(it, "cover", "origin_cover", "originCover", "dynamic_cover")
        }?.let(::normalizeMediaUrl) ?: images.firstOrNull()
        val music = item.optJSONObject("music") ?: item.optJSONObject("music_info")
        val audio = music?.let { findUrl(it, "play_url", "playUrl", "play_url_h265", "playUrlH265") }
            ?.let(::normalizeMediaUrl)
        val musicTitle = music?.optString("title")?.takeIf { it.isNotBlank() }
        val musicDuration = music?.optLong("duration", 0L)?.takeIf { it > 0 }?.times(1000)

        if (videoUrl == null && images.isEmpty()) return null

        val id = item.string("aweme_id")
            ?: item.string("awemeId")
            ?: item.string("item_id")
            ?: sourceUrl.hashCode().toString()

        return Aweme(
            awemeId = id,
            title = item.string("desc")?.replace(Regex("\\s+"), " ")?.take(300).orEmpty()
                .ifBlank { "抖音作品" },
            author = item.optJSONObject("author")?.let { it.string("nickname") ?: it.string("nickName") },
            coverUrl = cover,
            videoUrl = videoUrl,
            imageUrls = images,
            audioUrl = audio,
            durationMs = video?.optLong("duration", 0L)?.takeIf { it > 0 },
            sourceUrl = sourceUrl,
            musicTitle = musicTitle,
            musicDurationMs = musicDuration,
            imageLiveVideos = liveVideos
        )
    }

    /** Breadth-limited recursive search, used when the page structure is unfamiliar. */
    fun deepFind(root: Any?, sourceUrl: String): Aweme? {
        var visited = 0

        fun walk(node: Any?, depth: Int): Aweme? {
            if (node == null || depth > 10 || visited > 6000) return null
            visited++
            when (node) {
                is JSONObject -> {
                    if (node.has("aweme_id") || node.has("awemeId") ||
                        (node.has("desc") && (node.has("video") || node.has("images")))
                    ) {
                        fromItem(node, sourceUrl)?.let { return it }
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        walk(node.opt(keys.next()), depth + 1)?.let { return it }
                    }
                }
                is JSONArray -> {
                    for (index in 0 until node.length()) {
                        walk(node.opt(index), depth + 1)?.let { return it }
                    }
                }
            }
            return null
        }

        return walk(root, 0)
    }

    private data class ParsedImages(
        val urls: List<String>,
        val liveVideos: List<String?>
    )

    private fun parseImages(item: JSONObject): ParsedImages {
        val images = LinkedHashMap<String, String?>()

        fun addEntry(value: Any?) {
            val entry = value as? JSONObject ?: return
            val image = entry.optJSONObject("display_image")
                ?: entry.optJSONObject("image")
                ?: entry
            val still = firstUrl(image) ?: firstUrl(entry) ?: return
            val normalized = normalizeMediaUrl(still)
            if (!normalized.startsWith("https://")) return
            val video = entry.optJSONObject("video") ?: image.optJSONObject("video")
            val live = video?.let {
                findUrl(it, "play_addr", "playAddr", "play_addr_h264", "play_addr_265", "playApi")
            }?.let(::normalizeMediaUrl)?.takeIf { it.startsWith("https://") }
            if (!images.containsKey(normalized) || images[normalized] == null) {
                images[normalized] = live
            }
        }

        item.optJSONArray("images")?.let { array ->
            for (index in 0 until array.length()) addEntry(array.opt(index))
        }
        item.optJSONObject("image_post_info")?.optJSONArray("images")?.let { array ->
            for (index in 0 until array.length()) addEntry(array.opt(index))
        }

        fun addLivePhoto(value: Any?) {
            val obj = value as? JSONObject ?: return
            val imageObj = obj.optJSONObject("image")
                ?: obj.optJSONObject("cover")
                ?: obj.optJSONObject("image_info")
            val videoObj = obj.optJSONObject("video")
                ?: obj.optJSONObject("video_info")
            val still = firstUrl(imageObj)
                ?: firstUrl(obj.opt("image"))
                ?: firstUrl(obj.opt("cover"))
                ?: return
            val normalized = normalizeMediaUrl(still)
            if (!normalized.startsWith("https://")) return
            val live = videoObj?.let {
                findUrl(it, "play_addr", "playAddr", "play_addr_h264", "play_addr_265", "playApi")
                    ?: firstUrl(it)
            }?.let(::normalizeMediaUrl)?.takeIf { it.startsWith("https://") }
            if (!images.containsKey(normalized) || images[normalized] == null) {
                images[normalized] = live
            }
        }

        item.optJSONObject("live_photo")?.let(::addLivePhoto)
        item.optJSONObject("live_photo_info")?.let(::addLivePhoto)
        item.optJSONArray("live_photos")?.let { array ->
            for (index in 0 until array.length()) addLivePhoto(array.opt(index))
        }
        item.optJSONObject("image_post_info")?.optJSONArray("live_photos")?.let { array ->
            for (index in 0 until array.length()) addLivePhoto(array.opt(index))
        }

        val urls = images.keys.take(300)
        return ParsedImages(urls, urls.map { images[it] })
    }

    private fun findUrl(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            firstUrl(obj.opt(key))?.let { return it }
        }
        return null
    }

    private fun firstUrl(value: Any?): String? = when (value) {
        is String -> value.takeIf { it.startsWith("http") || it.startsWith("//") }
        is JSONObject -> {
            firstUrl(value.opt("url_list"))
                ?: firstUrl(value.opt("urlList"))
                ?: firstUrl(value.opt("download_url_list"))
                ?: firstUrl(value.opt("url"))
                ?: value.optJSONArray("url_list")?.let { array ->
                    (0 until array.length()).firstNotNullOfOrNull { firstUrl(array.opt(it)) }
                }
        }
        is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { firstUrl(value.opt(it)) }
        else -> null
    }

    private fun normalizeMediaUrl(raw: String): String {
        var url = raw.trim()
        if (url.startsWith("//")) url = "https:" + url
        if (url.startsWith("http://")) url = "https://" + url.removePrefix("http://")
        url = url.replace("playwm", "play").replace("watermark=1", "watermark=0")
        return url
    }

    private fun JSONObject.string(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}