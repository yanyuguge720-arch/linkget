package com.linkget.core

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal in-memory cookie jar. Douyin's share page issues a `ttwid` cookie on the first
 * (shell-only) response and expects it back on a second request before the SSR payload is
 * filled with `videoInfoRes`. The default OkHttp client keeps no cookies, which is why the
 * first implementation always saw the shell.
 */
class InMemoryCookieJar : CookieJar {

    private val store = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val current = store[url.host].orEmpty()
        val merged = LinkedHashMap<String, Cookie>()
        current.forEach { merged[it.name] = it }
        cookies.forEach { merged[it.name] = it }
        store[url.host] = merged.values.toList()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return store[url.host].orEmpty().filter { it.expiresAt > now && it.matches(url) }
    }
}
