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
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import com.olgaz.aichat.mcptools.reminder.ReminderEntity

@Database(
    entities = [MessageEntity::class, SettingsEntity::class, ReminderEntity::class],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "aichat_database"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_server_url TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_weather_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN mcp_reminder_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE settings SET mcp_weather_enabled = mcp_enabled")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        interval_minutes INTEGER,
                        created_at INTEGER NOT NULL,
                        completed_at INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN reminder_time INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN is_notified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN reminder_check_interval_minutes INTEGER NOT NULL DEFAULT 30")
            }
        }
    }
}
