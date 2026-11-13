package com.linkget.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A lightweight record; re-parsing starts from [sourceUrl], so no signed URLs are stored. */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val title: String,
    val author: String?,
    val coverUrl: String?,
    val platform: String,
    val parsedAt: Long
)
