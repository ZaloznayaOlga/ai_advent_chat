package com.olgaz.aichat.di

import com.olgaz.aichat.data.provider.FileContentReaderImpl
import com.olgaz.aichat.data.provider.SystemPromptProviderImpl
import com.olgaz.aichat.data.repository.ChatHistoryRepositoryImpl
import com.olgaz.aichat.data.repository.ChatRepositoryImpl
import com.olgaz.aichat.domain.provider.FileContentReader
import com.olgaz.aichat.domain.provider.SystemPromptProvider
import com.olgaz.aichat.domain.repository.ChatHistoryRepository
import com.olgaz.aichat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindChatHistoryRepository(
        chatHistoryRepositoryImpl: ChatHistoryRepositoryImpl
    ): ChatHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSystemPromptProvider(
        systemPromptProviderImpl: SystemPromptProviderImpl
    ): SystemPromptProvider

    @Binds
    @Singleton
    abstract fun bindFileContentReader(
        fileContentReaderImpl: FileContentReaderImpl
    ): FileContentReader

}