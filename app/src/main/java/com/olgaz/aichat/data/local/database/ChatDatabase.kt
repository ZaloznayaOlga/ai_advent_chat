package com.olgaz.aichat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.olgaz.aichat.data.local.converter.Converters
import com.olgaz.aichat.data.local.dao.MessageDao
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.entity.MessageEntity
import com.olgaz.aichat.data.local.entity.SettingsEntity

@Database(
    entities = [MessageEntity::class, SettingsEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "aichat_database"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_server_url TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
