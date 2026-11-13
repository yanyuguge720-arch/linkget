package com.linkget.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadTaskEntity::class, HistoryEntry::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao

    companion object {
        /** Adds the audio extraction flag. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE download_tasks ADD COLUMN extractFromVideo INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Adds the live-photo motion URL used for Motion Photo compositing. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE download_tasks ADD COLUMN liveVideoUrl TEXT"
                )
            }
        }
    }
}
