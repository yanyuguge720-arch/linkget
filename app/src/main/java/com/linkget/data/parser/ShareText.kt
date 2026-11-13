package com.linkget.data.parser

import java.net.URI

/**
 * Extracts the first Douyin URL from a share text blob and pulls the numeric aweme id.
 * Pure functions only: the clipboard watcher, the parser and the unit tests all share this.
 */
object ShareText {
    private const val TRAILING = ".,;:!?)]}>'\"。，、；：！？）】》”’"

    private val URL_REGEX = Regex(
        "https?://[^\\s\\u3000，。！？、；：“”‘’《》【】（）()<>\\[\\]\"']+",
        RegexOption.IGNORE_CASE
    )

    private val AWEME_ID_PATTERNS = listOf(
        Regex("""/(?:video|note|slides)/(\d{5,})"""),
        Regex("""[?&](?:modal_id|aweme_id|item_id)=(\d{5,})""")
    )

    /** Returns the cleaned URL of the first Douyin link in [text], or null. */
    fun extractUrl(text: String): String? = URL_REGEX.findAll(text)
        .map { it.value.trimEnd { ch -> ch in TRAILING } }
        .firstOrNull { it.isNotBlank() && isDouyinUrl(it) }

    fun looksLikeDouyin(text: String): Boolean = extractUrl(text) != null

    fun isDouyinUrl(url: String): Boolean {
        val host = runCatching { URI(url.trim()).host }.getOrNull()?.lowercase() ?: return false
        return host == "douyin.com" ||
            host == "iesdouyin.com" ||
            host.endsWith(".douyin.com") ||
            host.endsWith(".iesdouyin.com")
    }

    fun awemeId(url: String): String? {
        for (pattern in AWEME_ID_PATTERNS) {
            val match = pattern.find(url) ?: continue
            return match.groupValues[1]
        }
        return null
    }
}
