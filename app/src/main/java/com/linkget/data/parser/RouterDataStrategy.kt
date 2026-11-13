package com.linkget.data.parser

import org.json.JSONObject

/**
 * Primary strategy: the mobile share page embeds a fully hydrated payload in
 * `window._ROUTER_DATA`, under `loaderData["video_(id)/page"].videoInfoRes.item_list`.
 */
object RouterDataStrategy : ParseStrategy {
    override val name = "router-data"

    override fun parse(html: String, sourceUrl: String): Aweme? {
        val raw = JsonSlicer.extractJsonObject(html, "window._ROUTER_DATA") ?: return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        val loaderData = root.optJSONObject("loaderData")
        if (loaderData != null) {
            val keys = loaderData.keys()
            while (keys.hasNext()) {
                val page = loaderData.optJSONObject(keys.next()) ?: continue
                val info = page.optJSONObject("videoInfoRes") ?: page.optJSONObject("videoInfo")
                    ?: continue
                val items = info.optJSONArray("item_list") ?: info.optJSONArray("itemList")
                    ?: continue
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    AwemeJson.fromItem(item, sourceUrl)?.let { return it }
                }
            }
        }

        return AwemeJson.deepFind(root, sourceUrl)
    }
}
