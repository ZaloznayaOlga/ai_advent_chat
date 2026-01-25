package com.olgaz.aichat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.olgaz.aichat.data.local.converter.Converters
import com.olgaz.aichat.data.local.dao.MessageDao
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.entity.MessageEntity
import com.olgaz.aichat.data.local.entity.SettingsEntity

@Database(
    entities = [MessageEntity::class, SettingsEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "aichat_database"
    }
}
