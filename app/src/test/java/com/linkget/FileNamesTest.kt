package com.linkget

import com.linkget.core.FileNames
import com.linkget.data.model.ResourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {

    @Test
    fun sanitizesTitlesAndFolders() {
        assertEquals("LinkGet", FileNames.sanitizeFolder("  LinkGet  "))
        assertEquals("a b c", FileNames.sanitizeTitle("a/b:c*?"))
        assertTrue(FileNames.baseName("标题", 0L).startsWith("标题_"))
    }

    @Test
    fun picksSafeExtensions() {
        assertEquals("mp4", FileNames.extension("https://x/video.m3u8", ResourceKind.VIDEO))
        assertEquals("m4a", FileNames.extension("https://x/audio.m4s", ResourceKind.AUDIO))
        assertEquals("jpg", FileNames.extension("https://x/image", ResourceKind.IMAGE))
        assertEquals("video/mp4", FileNames.mimeType("a.mp4"))
    }
}
