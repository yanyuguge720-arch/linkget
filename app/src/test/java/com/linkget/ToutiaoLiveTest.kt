package com.linkget

import com.linkget.data.parser.ToutiaoWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test

class ToutiaoLiveTest {
    @Test
    fun parsesPublicVideo() = runBlocking {
        val parser = ToutiaoWorkParser(OkHttpClient())
        val work = parser.parse("https://m.toutiao.com/video/7576704796543942694/")
        println("TOUTIAO title=" + work.title + " author=" + work.author +
            " videos=" + work.videos.map { it.quality + ":" + it.url.take(40) } +
            " images=" + work.images.size)
    }
}
