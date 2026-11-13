package com.linkget.data.model

import java.util.Locale

/** Where a link came from. Kept small: new platforms only add an entry. */
enum class Platform(val label: String, val badge: String) {
    DOUYIN("抖音", "抖"),
    BILIBILI("哔哩哔哩", "B"),
    XIAOHONGSHU("小红书", "红"),
    KUAISHOU("快手", "快"),
    TOUTIAO("今日头条", "头"),
    GENERIC("网页", "链"),
    UNKNOWN("未知", "?")
}

enum class ResourceKind { VIDEO, AUDIO, IMAGE }

/**
 * One downloadable thing extracted from a work. A video post can yield several qualities,
 * an image post yields every image, and a Bilibili video can yield a separate audio track.
 */
data class MediaResource(
    val key: String,
    val kind: ResourceKind,
    val url: String,
    val label: String,
    val quality: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val extension: String? = null,
    val needMerge: Boolean = false,
    val audioUrl: String? = null,
    /** For a Douyin live photo: the short motion clip aligned with this still image. */
    val liveVideoUrl: String? = null,
    val liveDurationMs: Long? = null,
    /** True for the standalone MP4 resource of a Douyin live photo. */
    val liveMotion: Boolean = false,
    val index: Int = 0
) {
    fun resolvedExtension(): String {
        extension?.takeIf { it.isNotBlank() }?.let { return it.lowercase(Locale.ROOT) }
        val path = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val raw = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (kind) {
            ResourceKind.VIDEO -> when (raw) {
                "webm", "mov", "m4v", "mp4" -> raw
                else -> "mp4"
            }
            ResourceKind.AUDIO -> when (raw) {
                "mp3", "m4a", "aac", "wav", "flac", "ogg" -> raw
                "m4s" -> "m4a"
                else -> "m4a"
            }
            ResourceKind.IMAGE -> when (raw) {
                "jpg", "jpeg", "png", "webp", "gif" -> raw
                else -> "jpg"
            }
        }
    }
}

/** Normalised result of parsing one share link. The UI, history and downloader share it. */
data class ParsedWork(
    val id: String,
    val platform: Platform,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val durationMs: Long?,
    val sourceUrl: String,
    val resources: List<MediaResource>
) {
    val videos: List<MediaResource> get() = resources.filter { it.kind == ResourceKind.VIDEO }
    val normalVideos: List<MediaResource> get() = videos.filter { !it.liveMotion }
    val liveMotions: List<MediaResource> get() = resources.filter { it.liveMotion }
    val audios: List<MediaResource> get() = resources.filter { it.kind == ResourceKind.AUDIO }
    val images: List<MediaResource> get() = resources.filter { it.kind == ResourceKind.IMAGE }

    val isLivePhoto: Boolean
        get() = liveMotions.isNotEmpty() || images.any { it.liveVideoUrl != null }

    fun has(kind: ResourceKind): Boolean = resources.any { it.kind == kind }
}

fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val total = millis / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
