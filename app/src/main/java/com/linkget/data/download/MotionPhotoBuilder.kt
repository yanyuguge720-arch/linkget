package com.linkget.data.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Builds an Android Motion Photo: a JPEG still with the short MP4 appended and an XMP
 * Container directory describing the video. Android galleries (Google/Samsung/Xiaomi)
 * recognize the `_MP.jpg` naming convention and the GCamera:MotionPhoto metadata.
 *
 * Spec: https://developer.android.com/media/platform/motion-photo-format
 */
object MotionPhotoBuilder {

    fun build(imageFile: File, videoFile: File, outputFile: File): Boolean {
        if (!imageFile.exists() || !videoFile.exists()) return false
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return false
        val jpeg = try {
            ByteArrayOutputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) return false
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        if (jpeg.size < 4) return false

        val videoBytes = videoFile.readBytes()
        if (videoBytes.isEmpty()) return false
        val xmp = buildXmp(videoBytes.size)
        val xmpHeader = "http://ns.adobe.com/xap/1.0/".toByteArray(Charsets.UTF_8) + byteArrayOf(0)
        val payload = xmpHeader + xmp.toByteArray(Charsets.UTF_8)
        if (payload.size + 2 > 65535) return false

        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().buffered().use { out ->
            out.write(0xFF)
            out.write(0xD8)
            // APP1 / XMP segment
            out.write(0xFF)
            out.write(0xE1)
            out.write((payload.size + 2) ushr 8 and 0xFF)
            out.write((payload.size + 2) and 0xFF)
            out.write(payload)
            // JPEG body, skipping the original SOI
            out.write(jpeg, 2, jpeg.size - 2)
            // Motion photo video, appended at the very end of the file
            out.write(videoBytes)
        }
        return outputFile.length() > jpeg.size
    }

    private fun buildXmp(videoLength: Int): String =
        "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core\">" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
            "<rdf:Description rdf:about=\"\" " +
            "xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\" " +
            "xmlns:Container=\"http://ns.google.com/photos/1.0/container/\" " +
            "xmlns:Item=\"http://ns.google.com/photos/1.0/container/item/\" " +
            "GCamera:MicroVideo=\"1\" " +
            "GCamera:MicroVideoVersion=\"1\" " +
            "GCamera:MicroVideoOffset=\"$videoLength\" " +
            "GCamera:MicroVideoPresentationTimestampUs=\"0\" " +
            "GCamera:MotionPhoto=\"1\" " +
            "GCamera:MotionPhotoVersion=\"1\" " +
            "GCamera:MotionPhotoPresentationTimestampUs=\"0\">" +
            "<Container:Directory>" +
            "<rdf:Seq>" +
            "<rdf:li rdf:parseType=\"Resource\">" +
            "<Container:Item Item:Mime=\"image/jpeg\" Item:Semantic=\"Primary\" Item:Length=\"0\" Item:Padding=\"0\"/>" +
            "</rdf:li>" +
            "<rdf:li rdf:parseType=\"Resource\">" +
            "<Container:Item Item:Mime=\"video/mp4\" Item:Semantic=\"MotionPhoto\" Item:Length=\"$videoLength\" Item:Padding=\"0\"/>" +
            "</rdf:li>" +
            "</rdf:Seq>" +
            "</Container:Directory>" +
            "</rdf:Description>" +
            "</rdf:RDF>" +
            "</x:xmpmeta>"
}
