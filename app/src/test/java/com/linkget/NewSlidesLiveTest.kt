package com.linkget

import com.linkget.core.InMemoryCookieJar
import com.linkget.data.parser.DouyinWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test

class NewSlidesLiveTest {
    @Test
    fun parseNewLink() = runBlocking {
        val parser = DouyinWorkParser(
            OkHttpClient.Builder().cookieJar(InMemoryCookieJar()).build()
        )
        val work = parser.parse("https://v.douyin.com/-XdTKnbfmLA/")
        println("NEW images=" + work.images.size + " live=" + work.images.count { it.liveVideoUrl != null } +
            " audio=" + work.audios.firstOrNull()?.url + " title=" + work.title)
    }
}
