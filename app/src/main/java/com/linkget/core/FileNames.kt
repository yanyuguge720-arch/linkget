package com.linkget.core

import com.linkget.data.model.ResourceKind
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Filesystem naming rules. Pure Kotlin so they can be unit-tested off-device. */
object FileNames {

    private const val FALLBACK_FOLDER = "LinkGet"
    private const val MAX_BASE_LENGTH = 56
    private val ILLEGAL = Regex("""[\\/:*?"<>|\r\n\t]""")
    private val SPACES = Regex("\\s+")

    fun sanitizeFolder(raw: String): String =
        sanitize(raw, FALLBACK_FOLDER).take(32).ifBlank { FALLBACK_FOLDER }

    fun sanitizeTitle(raw: String): String =
        sanitize(raw, "LinkGet 作品").take(MAX_BASE_LENGTH).ifBlank { "LinkGet 作品" }

    fun baseName(title: String, timeMillis: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date(timeMillis))
        return sanitizeTitle(title) + "_" + stamp
    }

    fun extension(url: String, kind: ResourceKind): String {
        val path = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            kind == ResourceKind.VIDEO -> if (ext in setOf("mp4", "webm", "mov", "m4v")) ext else "mp4"
            kind == ResourceKind.AUDIO -> when (ext) {
                "mp3", "m4a", "aac", "wav", "flac", "ogg" -> ext
                "m4s" -> "m4a"
                else -> "m4a"
            }
            else -> if (ext in setOf("jpg", "jpeg", "png", "webp", "gif")) ext else "jpg"
        }
    }

    fun mimeType(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "m4a", "m4s" -> "audio/mp4"
            "aac" -> "audio/aac"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "application/octet-stream"
        }

    fun formatBytes(bytes: Long): String = when {
        bytes <= 0 -> ""
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> {
            val kb = bytes / 1024.0
            if (kb < 10) String.format(Locale.US, "%.1f KB", kb)
            else String.format(Locale.US, "%.0f KB", kb)
        }
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }

    fun uniqueFile(directory: File, fileName: String): File {
        var candidate = File(directory, fileName)
        if (!candidate.exists()) return candidate
        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var counter = 2
        while (candidate.exists()) {
            val name = if (ext.isEmpty()) "$base ($counter)" else "$base ($counter).$ext"
            candidate = File(directory, name)
            counter++
        }
        return candidate
    }

    private fun sanitize(raw: String, fallback: String): String {
        val cleaned = raw.replace(ILLEGAL, " ")
            .replace(SPACES, " ")
            .trim()
            .trim('.')
            .trim()
        return cleaned.ifBlank { fallback }
    }
}
