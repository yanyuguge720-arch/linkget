package com.linkget.data.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Extracts the first audio track from a downloaded MP4 into an M4A container. */
object AudioExtractor {

    fun extract(videoFile: File, outputFile: File): Boolean {
        if (!videoFile.exists()) return false
        if (outputFile.exists()) outputFile.delete()
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(i)
                val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    track = i
                    format = candidate
                    break
                }
            }
            if (track < 0 || format == null) return false
            extractor.selectTrack(track)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(format)
            muxer.start()

            val maxSize = runCatching {
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                } else 0
            }.getOrDefault(0)
            val buffer = java.nio.ByteBuffer.allocateDirect(maxOf(512 * 1024, maxSize))
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                muxer.writeSampleData(outTrack, buffer, info)
                extractor.advance()
            }
            muxer.stop()
            return outputFile.exists() && outputFile.length() > 0
        } catch (_: Exception) {
            runCatching { outputFile.delete() }
            return false
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }
}
