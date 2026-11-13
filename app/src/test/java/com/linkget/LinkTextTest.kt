package com.linkget

import com.linkget.data.model.Platform
import com.linkget.data.parser.LinkText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkTextTest {

    @Test
    fun extractsUrlFromDouyinShareText() {
        val text = "7.92 复制打开抖音，看看【阿岛的作品】海边日落 https://v.douyin.com/iRNBho6/ 复制此链接"
        assertEquals("https://v.douyin.com/iRNBho6/", LinkText.extractUrl(text))
        assertEquals(Platform.DOUYIN, LinkText.platformOf("https://v.douyin.com/iRNBho6/"))
    }

    @Test
    fun detectsBilibiliAndShortLink() {
        assertEquals(Platform.BILIBILI, LinkText.platformOf("https://www.bilibili.com/video/BV1GJ411x7h7"))
        assertEquals(Platform.BILIBILI, LinkText.platformOf("https://b23.tv/abc123"))
        assertEquals("BV1GJ411x7h7", LinkText.bilibiliId("https://www.bilibili.com/video/BV1GJ411x7h7?share_source=copy"))
    }

    @Test
    fun detectsXiaohongshuAndKuaishou() {
        assertEquals(Platform.XIAOHONGSHU, LinkText.platformOf("https://www.xiaohongshu.com/explore/abc"))
        assertEquals(Platform.XIAOHONGSHU, LinkText.platformOf("https://xhslink.com/a1b2c3"))
        assertEquals(Platform.KUAISHOU, LinkText.platformOf("https://v.kuaishou.com/abc123"))
        assertEquals(Platform.KUAISHOU, LinkText.platformOf("https://www.kuaishou.com/short-video/3x123"))
        assertEquals(Platform.TOUTIAO, LinkText.platformOf("https://m.toutiao.com/video/7576704796543942694/"))
        assertEquals(Platform.TOUTIAO, LinkText.platformOf("https://www.toutiao.com/article/123/"))
    }

    @Test
    fun rejectsUnsupportedHost() {
        assertEquals(Platform.GENERIC, LinkText.platformOf("https://example.com/video/1"))
        assertTrue(LinkText.extractUrl("no url here") == null)
    }
}