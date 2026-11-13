package com.linkget

import com.linkget.data.parser.RouterDataStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinStrategyTest {

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/" + name)!!.readText()

    @Test
    fun readsVideoPayload() {
        val aweme = RouterDataStrategy.parse(
            fixture("router_video.html"),
            "https://www.iesdouyin.com/share/video/7300000000000000001/"
        )
        assertNotNull(aweme)
        assertEquals("海边日落 #旅行 #治愈", aweme!!.title)
        assertEquals("阿岛", aweme.author)
        assertNotNull(aweme.videoUrl)
        assertTrue(!aweme.videoUrl!!.contains("playwm"))
    }

    @Test
    fun readsImagePayload() {
        val aweme = RouterDataStrategy.parse(
            fixture("router_images.html"),
            "https://www.iesdouyin.com/share/note/7300000000000000002/"
        )
        assertNotNull(aweme)
        assertEquals(2, aweme!!.imageUrls.size)
        assertNotNull(aweme.audioUrl)
    }
}
