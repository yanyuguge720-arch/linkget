package com.linkget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY parsedAt DESC")
    fun observeAll(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}
