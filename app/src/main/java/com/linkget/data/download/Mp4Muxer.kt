package com.linkget.data.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Muxes one video-only MP4 and one audio-only MP4/M4A into a single H.264/AAC MP4 using the
 * platform MediaMuxer. Used for Bilibili DASH qualities where the streams are separate.
 */
object Mp4Muxer {

    fun merge(videoFile: File, audioFile: File, outputFile: File): Boolean {
        if (!videoFile.exists() || !audioFile.exists()) return false
        if (outputFile.exists()) outputFile.delete()

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }

            val videoTrack = findTrack(videoExtractor, "video/") ?: return false
            val audioTrack = findTrack(audioExtractor, "audio/") ?: return false
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioTrack)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideo = muxer.addTrack(videoFormat)
            val outAudio = muxer.addTrack(audioFormat)
            val rotation = runCatching { videoFormat.getInteger("rotation-degrees") }.getOrDefault(0)
            if (rotation != 0) muxer.setOrientationHint(rotation)
            muxer.start()

            copyTrack(videoExtractor, videoTrack, outVideo, muxer)
            copyTrack(audioExtractor, audioTrack, outAudio, muxer)
            muxer.stop()
            return outputFile.exists() && outputFile.length() > 0
        } catch (_: Exception) {
            runCatching { outputFile.delete() }
            return false
        } finally {
            runCatching { muxer?.release() }
            runCatching { videoExtractor?.release() }
            runCatching { audioExtractor?.release() }
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) {
                extractor.selectTrack(i)
                return i
            }
        }
        return null
    }

    private fun copyTrack(
        extractor: MediaExtractor,
        track: Int,
        outTrack: Int,
        muxer: MediaMuxer
    ) {
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val maxSize = runCatching {
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else 0
        }.getOrDefault(0)
        val buffer = ByteBuffer.allocateDirect(maxOf(1024 * 1024, maxSize))
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime
            val sampleFlags = extractor.sampleFlags
            info.flags = when {
                sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0 -> MediaCodec.BUFFER_FLAG_KEY_FRAME
                sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0 -> MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                else -> 0
            }
            muxer.writeSampleData(outTrack, buffer, info)
            extractor.advance()
        }
    }
}
