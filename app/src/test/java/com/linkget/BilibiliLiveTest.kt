package com.linkget

import com.linkget.data.model.ResourceKind
import com.linkget.data.parser.BilibiliWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Live smoke test against a public Bilibili video. Requires network. */
class BilibiliLiveTest {

    @Test
    fun parsesPublicVideoWithQualities() = runBlocking {
        val parser = BilibiliWorkParser(OkHttpClient())
        val work = parser.parse("https://www.bilibili.com/video/BV1GJ411x7h7")
        assertTrue(work.title.isNotBlank())
        assertNotNull(work.coverUrl)
        assertFalse(work.videos.isEmpty())
        assertTrue(work.videos.all { it.url.startsWith("https://") })
        println("Bilibili qualities: " + work.videos.map { it.quality })
        println("Audio: " + work.audios.map { it.url.take(60) })
        assertTrue(work.resources.any { it.kind == ResourceKind.VIDEO })

        // CDN nodes are regional and occasionally rate-limit; report but do not fail the
        // parser test on a transient 403. The on-device download flow is tested manually.
        val best = work.videos.first()
        val request = okhttp3.Request.Builder()
            .url(best.url)
            .header("Referer", "https://www.bilibili.com/")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .header("Range", "bytes=0-1023")
            .build()
        runCatching {
            OkHttpClient().newCall(request).execute().use { response ->
                println("CDN status: " + response.code)
            }
        }
        Unit
    }
}