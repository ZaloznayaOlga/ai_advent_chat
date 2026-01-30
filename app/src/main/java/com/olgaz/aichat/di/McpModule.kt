package com.olgaz.aichat.di

import com.olgaz.aichat.data.repository.McpRepositoryImpl
import com.olgaz.aichat.domain.repository.McpRepository
import com.olgaz.aichat.mcptools.datetime.DateTimeToolHandler
import com.olgaz.aichat.mcptools.reminder.ReminderDao
import com.olgaz.aichat.mcptools.reminder.ReminderToolHandler
import com.olgaz.aichat.notification.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class McpHttpClient

@Module
@InstallIn(SingletonComponent::class)
object McpModule {

    @Provides
    @Singleton
    @McpHttpClient
    fun provideMcpHttpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(SSE)
        install(ContentNegotiation) {
            json(json)
        }
        engine {
            requestTimeout = 60_000
        }
    }

    @Provides
    @Singleton
    fun provideMcpRepository(
        @McpHttpClient httpClient: HttpClient
    ): McpRepository = McpRepositoryImpl(httpClient)
}
