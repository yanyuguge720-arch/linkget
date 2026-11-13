package com.linkget.data.parser

import com.linkget.data.model.Platform
import java.net.URI

/** Extracts and classifies share links. Pure functions, shared by UI and parsers. */
object LinkText {
    private const val TRAILING = ".,;:!?)]}>'\"。，、；：！？）】》”’"

    private val URL_REGEX = Regex(
        "https?://[^\\s\\u3000，。！？、；：“”‘’《》【】（）()<>\\[\\]\"']+",
        RegexOption.IGNORE_CASE
    )
    private val BV_REGEX = Regex("""(BV[0-9A-Za-z]{10})""")
    private val AV_REGEX = Regex("""(?:^|/|\?|&)av(\d+)""", RegexOption.IGNORE_CASE)
    private val VIDEO_ID_REGEX = Regex("""/(?:video|note)/(\d{5,})""")

    fun extractUrl(text: String): String? = URL_REGEX.findAll(text)
        .map { it.value.trimEnd { ch -> ch in TRAILING } }
        .firstOrNull { it.isNotBlank() }

    fun platformOf(url: String?): Platform {
        if (url == null) return Platform.UNKNOWN
        val host = runCatching { URI(url.trim()).host }.getOrNull()?.lowercase() ?: return Platform.UNKNOWN
        return when {
            host == "douyin.com" || host.endsWith(".douyin.com") ||
                host == "iesdouyin.com" || host.endsWith(".iesdouyin.com") -> Platform.DOUYIN
            host == "bilibili.com" || host.endsWith(".bilibili.com") ||
                host == "b23.tv" || host.endsWith(".b23.tv") -> Platform.BILIBILI
            host == "xiaohongshu.com" || host.endsWith(".xiaohongshu.com") ||
                host == "xhslink.com" || host.endsWith(".xhslink.com") -> Platform.XIAOHONGSHU
            host == "kuaishou.com" || host.endsWith(".kuaishou.com") ||
                host == "v.kuaishou.com" || host == "chenzhongtech.com" ||
                host.endsWith(".chenzhongtech.com") -> Platform.KUAISHOU
            host == "toutiao.com" || host.endsWith(".toutiao.com") ||
                host == "m.toutiao.com" -> Platform.TOUTIAO
            else -> Platform.GENERIC
        }
    }

    fun isDouyinUrl(url: String): Boolean = platformOf(url) == Platform.DOUYIN
    fun isBilibiliUrl(url: String): Boolean = platformOf(url) == Platform.BILIBILI

    fun awemeId(url: String): String? =
        VIDEO_ID_REGEX.find(url)?.groupValues?.getOrNull(1)

    fun bilibiliId(url: String): String? {
        BV_REGEX.find(url)?.let { return it.groupValues[1] }
        AV_REGEX.find(url)?.let { return "av" + it.groupValues[1] }
        return null
    }
}
