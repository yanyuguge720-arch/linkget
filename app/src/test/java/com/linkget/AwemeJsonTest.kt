package com.linkget

import com.linkget.data.parser.AwemeJson
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AwemeJsonTest {

    @Test
    fun readsAllImagesLiveClipsAndMusic() {
        val json = JSONObject(
            """
            {
              "aweme_id": "123456789",
              "desc": "实况图测试",
              "author": {"nickname": "作者"},
              "images": [
                {"url_list":["https://img/1.webp"]},
                {"url_list":["https://img/2.webp"], "video":{"play_addr":{"url_list":["https://video/2.mp4"]}}},
                {"url_list":["https://img/3.webp"], "video":{"play_addr":{"url_list":["https://video/3.mp4"]}}}
              ],
              "music": {
                "title": "背景音乐",
                "duration": 49,
                "play_url": {"url_list":["https://music/bgm.mp3"]}
              }
            }
            """.trimIndent()
        )
        val aweme = AwemeJson.fromItem(json, "https://v.douyin.com/test/")
        assertNotNull(aweme)
        assertEquals(3, aweme!!.imageUrls.size)
        assertEquals(2, aweme.imageLiveVideos.count { it != null })
        assertEquals("https://video/2.mp4", aweme.imageLiveVideos[1])
        assertEquals("https://music/bgm.mp3", aweme.audioUrl)
        assertEquals(49_000L, aweme.musicDurationMs)
        assertTrue(aweme.audioUrl!!.endsWith(".mp3"))
    }

    @Test
    fun readsTopLevelLivePhotoSchema() {
        val json = JSONObject(
            """
            {
              "aweme_id": "987654321",
              "desc": "LivePhoto",
              "author": {"nickname": "作者"},
              "live_photo": {
                "image": {"url_list":["https://img/live-cover.jpg"]},
                "video": {"play_addr":{"url_list":["https://video/live-motion.mp4"]}}
              }
            }
            """.trimIndent()
        )
        val aweme = AwemeJson.fromItem(json, "https://v.douyin.com/live/")
        assertNotNull(aweme)
        assertEquals(1, aweme!!.imageUrls.size)
        assertEquals("https://img/live-cover.jpg", aweme.imageUrls[0])
        assertEquals("https://video/live-motion.mp4", aweme.imageLiveVideos[0])
    }
}
