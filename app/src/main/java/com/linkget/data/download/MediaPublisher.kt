package com.linkget.data.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.linkget.core.FileNames
import com.linkget.data.db.DownloadTaskEntity
import com.linkget.data.model.ResourceKind
import java.io.File
import java.io.IOException

/** Publishes a finished temp file into the public media collections. */
class MediaPublisher(private val context: Context) {

    fun publish(temp: File, task: DownloadTaskEntity): String {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            throw IOException("外部存储不可用")
        }
        val mime = FileNames.mimeType(task.fileName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishScoped(temp, task, mime)
        } else {
            publishLegacy(temp, task, mime)
        }
    }

    private fun publishScoped(temp: File, task: DownloadTaskEntity, mime: String): String {
        val collection = when (task.kind) {
            ResourceKind.VIDEO.name -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ResourceKind.IMAGE.name -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val baseDir = when (task.kind) {
            ResourceKind.VIDEO.name -> Environment.DIRECTORY_MOVIES
            ResourceKind.IMAGE.name -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MUSIC
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, task.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, baseDir + "/" + task.folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("无法写入媒体库")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                temp.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("无法打开输出流")
            val finished = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, finished, null, null)
            return uri.toString()
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun publishLegacy(temp: File, task: DownloadTaskEntity, mime: String): String {
        val baseDir = when (task.kind) {
            ResourceKind.VIDEO.name -> Environment.DIRECTORY_MOVIES
            ResourceKind.IMAGE.name -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MUSIC
        }
        val directory = File(Environment.getExternalStoragePublicDirectory(baseDir), task.folder)
        if (!directory.exists() && !directory.mkdirs()) throw IOException("无法创建保存目录")
        val target = FileNames.uniqueFile(directory, task.fileName)
        try {
            temp.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        } catch (e: SecurityException) {
            throw IOException("没有存储权限，请在系统设置中允许", e)
        }
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            target
        ).toString()
    }
}
