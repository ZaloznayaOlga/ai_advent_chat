package com.olgaz.aichat.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.olgaz.aichat.BuildConfig
import com.olgaz.aichat.data.remote.api.RagApi
import com.olgaz.aichat.data.repository.RagRepositoryImpl
import com.olgaz.aichat.domain.repository.RagRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RagApiClient

@Module
@InstallIn(SingletonComponent::class)
object RagModule {

    @Provides
    @Singleton
    @RagApiClient
    fun provideRagOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @RagApiClient
    fun provideRagRetrofit(
        @RagApiClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.RAG_SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideRagApi(@RagApiClient retrofit: Retrofit): RagApi =
        retrofit.create(RagApi::class.java)

    @Provides
    @Singleton
    fun provideRagRepository(ragApi: RagApi): RagRepository =
        RagRepositoryImpl(ragApi)
}
