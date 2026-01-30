package com.olgaz.aichat.di

import com.olgaz.aichat.domain.repository.LocalToolHandler
import com.olgaz.aichat.mcptools.datetime.DateTimeToolHandler
import com.olgaz.aichat.mcptools.reminder.ReminderToolHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReminderTool

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DateTimeTool

@Module
@InstallIn(SingletonComponent::class)
abstract class ToolModule {

    @Binds
    @Singleton
    @ReminderTool
    abstract fun bindReminderTool(handler: ReminderToolHandler): LocalToolHandler

    @Binds
    @Singleton
    @DateTimeTool
    abstract fun bindDateTimeTool(handler: DateTimeToolHandler): LocalToolHandler
}