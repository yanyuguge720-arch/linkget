package com.linkget

import com.linkget.data.download.ResumableFileDownloader
import com.linkget.data.parser.BilibiliWorkParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Live end-to-end check: parse a Bilibili video, then download its audio track. */
class DownloadSmokeTest {

    @Test
    fun downloadsBilibiliAudioWithReferer() = runBlocking {
        val client = OkHttpClient()
        val work = BilibiliWorkParser(client).parse("https://www.bilibili.com/video/BV1GJ411x7h7")
        val audio = work.audios.firstOrNull() ?: return@runBlocking
        val target = File.createTempFile("linkget-audio-", ".m4a")
        try {
            ResumableFileDownloader(client).download(
                audio.url,
                target,
                mapOf(
                    "Referer" to "https://www.bilibili.com/",
                    "Origin" to "https://www.bilibili.com"
                )
            )
            println("Downloaded audio bytes: " + target.length())
            assertTrue(target.length() > 100_000L)
        } finally {
            target.delete()
        }
    }
}
