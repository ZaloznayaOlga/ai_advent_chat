package com.olgaz.aichat.di

import android.content.Context
import androidx.room.Room
import com.olgaz.aichat.data.local.dao.MessageDao
import com.olgaz.aichat.data.local.dao.SettingsDao
import com.olgaz.aichat.data.local.database.ChatDatabase
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            ChatDatabase.DATABASE_NAME
        )
            .addMigrations(ChatDatabase.MIGRATION_2_3, ChatDatabase.MIGRATION_3_4, ChatDatabase.MIGRATION_4_5, ChatDatabase.MIGRATION_5_6, ChatDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: ChatDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideReminderDao(database: ChatDatabase): ReminderDao {
        return database.reminderDao()
    }
}
