package com.linkget.data.parser

/** The media types this app knows how to save. */
enum class MediaKind { VIDEO, IMAGE, AUDIO, COVER }

/** A single downloadable file extracted from one share link. */
data class MediaAsset(
    val key: String,
    val kind: MediaKind,
    val url: String,
    val index: Int,
    val label: String
)

/**
 * Normalised result of parsing one Douyin share link. This is the only model the UI,
 * downloader and database know about, so a platform change only touches the parser layer.
 */
data class Aweme(
    val awemeId: String,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val videoUrl: String?,
    val imageUrls: List<String>,
    val audioUrl: String?,
    val durationMs: Long?,
    val sourceUrl: String,
    val musicTitle: String? = null,
    val musicDurationMs: Long? = null,
    /** Live-photo motion URL aligned with imageUrls; null for a still image. */
    val imageLiveVideos: List<String?> = emptyList()
) {
    val primaryKind: MediaKind
        get() = when {
            imageUrls.isNotEmpty() -> MediaKind.IMAGE
            videoUrl != null -> MediaKind.VIDEO
            else -> MediaKind.AUDIO
        }

    fun assets(): List<MediaAsset> = buildList {
        videoUrl?.takeIf { imageUrls.isEmpty() }?.let {
            add(MediaAsset("video", MediaKind.VIDEO, it, 0, "视频"))
        }
        coverUrl?.takeIf { imageUrls.isEmpty() && videoUrl != null }?.let {
            add(MediaAsset("cover", MediaKind.COVER, it, 0, "封面"))
        }
        imageUrls.forEachIndexed { index, url ->
            add(MediaAsset("image:" + index, MediaKind.IMAGE, url, index + 1, "图片 " + (index + 1)))
        }
        audioUrl?.let { add(MediaAsset("audio", MediaKind.AUDIO, it, 0, "背景音乐")) }
    }
}
