package com.linkget.data.library

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.linkget.data.model.ResourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalMediaFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val mime: String,
    val kind: ResourceKind,
    val relativePath: String
)

/** Queries only the LinkGet folders in the public media collections. */
class LibraryRepository(private val context: Context) {

    suspend fun load(folder: String): List<LocalMediaFile> = withContext(Dispatchers.IO) {
        val safeFolder = folder.ifBlank { "LinkGet" }
        val out = ArrayList<LocalMediaFile>()
        val roots = listOf(
            Triple(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/" + safeFolder, ResourceKind.VIDEO),
            Triple(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Music/" + safeFolder, ResourceKind.AUDIO),
            Triple(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/" + safeFolder, ResourceKind.IMAGE)
        )
        for ((collection, path, kind) in roots) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.RELATIVE_PATH
            )
            runCatching {
                context.contentResolver.query(
                    collection,
                    projection,
                    MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?",
                    arrayOf(path + "%"),
                    MediaStore.MediaColumns.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        out.add(
                            LocalMediaFile(
                                uri = ContentUris.withAppendedId(collection, id),
                                name = cursor.getString(nameCol) ?: "未命名",
                                size = cursor.getLong(sizeCol),
                                dateAdded = cursor.getLong(dateCol) * 1000L,
                                mime = cursor.getString(mimeCol) ?: "*/*",
                                kind = kind,
                                relativePath = cursor.getString(pathCol) ?: ""
                            )
                        )
                    }
                }
            }
        }
        out.sortedByDescending { it.dateAdded }
    }

    suspend fun delete(file: LocalMediaFile): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.delete(file.uri, null, null) > 0 }.getOrDefault(false)
    }

    suspend fun rename(file: LocalMediaFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        if (newName.isBlank()) return@withContext false
        val oldExt = file.name.substringAfterLast('.', "")
        val hasExt = newName.substringAfterLast('.', "").isNotEmpty()
        val name = if (!hasExt && oldExt.isNotEmpty()) newName + "." + oldExt else newName
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }
        runCatching {
            context.contentResolver.update(file.uri, values, null, null) > 0
        }.getOrDefault(false)
    }
}
