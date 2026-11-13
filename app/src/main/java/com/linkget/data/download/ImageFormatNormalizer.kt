package com.linkget.data.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Some CDNs (notably Douyin's wallpaper CDN) return WebP bytes even when the URL has no
 * image extension. Image tasks are named .jpg, so re-encode non-JPEG bytes to JPEG before
 * publishing; otherwise galleries and third-party viewers may reject the file.
 */
object ImageFormatNormalizer {

    fun ensureJpeg(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val header = ByteArray(12)
        val read = file.inputStream().use { it.read(header) }
        if (read >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()) {
            return true
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val temp = File(file.parentFile, file.name + ".normalized")
        val ok = try {
            temp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        } finally {
            bitmap.recycle()
        }
        if (!ok || temp.length() <= 0L) {
            temp.delete()
            return false
        }
        file.delete()
        return temp.renameTo(file)
    }
}
