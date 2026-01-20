package com.olgaz.aichat.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.olgaz.aichat.BuildConfig
import com.olgaz.aichat.data.remote.api.ChatApi
import com.olgaz.aichat.data.remote.logging.PrettyJsonLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeepSeekApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAiApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor(PrettyJsonLogger()).apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // DeepSeek
    @Provides
    @Singleton
    @DeepSeekApi
    fun provideDeepSeekAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @DeepSeekApi
    fun provideDeepSeekOkHttpClient(
        @DeepSeekApi authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @DeepSeekApi
    fun provideDeepSeekRetrofit(
        @DeepSeekApi okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @DeepSeekApi
    fun provideDeepSeekChatApi(@DeepSeekApi retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    // OpenAI
    @Provides
    @Singleton
    @OpenAiApi
    fun provideOpenAiAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @OpenAiApi
    fun provideOpenAiOkHttpClient(
        @OpenAiApi authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @OpenAiApi
    fun provideOpenAiRetrofit(
        @OpenAiApi okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @OpenAiApi
    fun provideOpenAiChatApi(@OpenAiApi retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)
}
