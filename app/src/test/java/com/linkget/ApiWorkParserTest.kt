package com.linkget

import com.linkget.data.model.ResourceKind
import com.linkget.data.parser.ApiWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiWorkParserTest {

    @Test
    fun mapsPrdResponseIntoResources() = runBlocking {
        val json = """
            {
              "code": 200,
              "msg": "解析成功",
              "data": {
                "title": "作品标题",
                "author": "作者名称",
                "cover": "https://example.com/cover.jpg",
                "duration": "00:15",
                "resource_list": [
                  {"type":"video","quality":"1080P","duration":"00:15","size":"24MB","download_url":"https://example.com/video.mp4","need_merge":true,"audio_url":"https://example.com/audio.m4a"},
                  {"type":"audio","duration":"00:15","size":"3MB","download_url":"https://example.com/audio.m4a"},
                  {"type":"image","download_url":"https://example.com/image.webp"}
                ]
              }
            }
        """.trimIndent()
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )
        server.start()
        try {
            val base = "http://" + server.hostName + ":" + server.port
            val work = ApiWorkParser(OkHttpClient()).parse(
                "https://www.bilibili.com/video/BV1GJ411x7h7",
                base
            )
            assertEquals("/api/parse", server.takeRequest().path)
            assertEquals("作品标题", work.title)
            assertEquals(3, work.resources.size)
            val video = work.resources.first { it.kind == ResourceKind.VIDEO }
            assertEquals("1080P", video.quality)
            assertTrue(video.needMerge)
            assertEquals("https://example.com/audio.m4a", video.audioUrl)
            assertEquals(15_000L, video.durationMs)
            assertEquals(24L * 1024 * 1024, video.sizeBytes)
            val image = work.resources.first { it.kind == ResourceKind.IMAGE }
            assertEquals("webp", image.resolvedExtension())
            assertNotNull(work.coverUrl)
        } finally {
            server.shutdown()
        }
    }
}
