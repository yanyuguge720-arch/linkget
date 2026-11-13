package com.linkget.data.parser

import java.net.URLDecoder

/**
 * Small, dependency-free helpers for pulling JSON out of an HTML page.
 *
 * Douyin embeds its payload either as `window._ROUTER_DATA = {...}` or in a
 * `&lt;script id="RENDER_DATA"&gt;` block. A plain regex cannot match nested braces, so we
 * scan the document once, tracking string state and brace depth.
 */
object JsonSlicer {

    fun extractJsonObject(source: String, marker: String): String? {
        val markerIndex = source.indexOf(marker)
        if (markerIndex < 0) return null
        var index = markerIndex + marker.length
        while (index < source.length && source[index] != '{') index++
        if (index >= source.length) return null
        return sliceBalancedObject(source, index)
    }

    /** Returns the raw text of a script tag selected by its `id` attribute. */
    fun extractScriptContent(html: String, scriptId: String): String? {
        val regex = Regex(
            "<script[^>]*id=[\"']" + Regex.escape(scriptId) + "[\"'][^>]*>(.*?)</script>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return regex.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** RENDER_DATA is URL-encoded on most pages, but plain JSON on some. Accept both. */
    fun decodeMaybe(raw: String): String {
        if (!raw.contains('%')) return raw
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    }

    private fun sliceBalancedObject(source: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escaped = false
        var index = start
        while (index < source.length) {
            val ch = source[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return source.substring(start, index + 1)
                    }
                }
            }
            index++
        }
        return null
    }
}
