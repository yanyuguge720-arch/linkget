package com.linkget.data.parser

import org.json.JSONObject

/**
 * Fallback strategy for pages that ship a URL-encoded `RENDER_DATA` JSON blob (the desktop
 * page and some A/B variants). The payload shape varies, so we walk it generically.
 */
object RenderDataStrategy : ParseStrategy {
    override val name = "render-data"

    override fun parse(html: String, sourceUrl: String): Aweme? {
        val raw = JsonSlicer.extractScriptContent(html, "RENDER_DATA") ?: return null
        val decoded = JsonSlicer.decodeMaybe(raw)
        val root = runCatching { JSONObject(decoded) }.getOrNull() ?: return null
        return AwemeJson.deepFind(root, sourceUrl)
    }
}
