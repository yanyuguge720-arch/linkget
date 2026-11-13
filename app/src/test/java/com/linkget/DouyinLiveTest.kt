package com.linkget

import com.linkget.data.parser.DouyinWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test

/** Live smoke test; links can expire, so this only reports rather than hard-asserting. */
class DouyinLiveTest {

    @Test
    fun triesKnownLinks() = runBlocking {
        val parser = DouyinWorkParser(
            OkHttpClient.Builder().cookieJar(com.linkget.core.InMemoryCookieJar()).build()
        )
        for (link in listOf(
            "https://v.douyin.com/IBo7M2SA99s/",
            "https://v.douyin.com/LQ3pEVTFq28/"
        )) {
            val result = runCatching { parser.parse(link) }
            result.onSuccess { work ->
                println(
                    "OK " + link + " -> " + work.title +
                        " / images=" + work.images.size +
                        " / live=" + work.images.count { it.liveVideoUrl != null } +
                        " / audio=" + work.audios.firstOrNull()?.url?.take(80) +
                        " / resources=" + work.resources.map { it.kind.name + ":" + it.label }
                )
            }.onFailure { error ->
                val detail = (error as? com.linkget.data.parser.ParseException)?.reason
                println("FAIL " + link + " -> " + detail + " / " + error.message)
                error.printStackTrace()
            }
        }
    }
}
