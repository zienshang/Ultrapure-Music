package com.ultrapuremusic.core.di

import com.ultrapuremusic.BuildConfig
import com.ultrapuremusic.core.network.api.BackendApiService
import com.ultrapuremusic.core.network.api.YoutubeApiService
import com.ultrapuremusic.core.util.Constants
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
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YoutubeRetrofit

/** Dedicated client for large file downloads — no response-body logging, no read timeout. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // BODY logging buffers the entire response — only safe for small JSON payloads.
            // Use HEADERS in release so large binary responses don't OOM.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", Constants.USER_AGENT)
            .build()
        chain.proceed(request)
    }

    /** Standard client for JSON API calls — short timeouts, header-only logging. */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        userAgentInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Dedicated client for streaming large file downloads.
     * - No response-body logging (avoids buffering GB-scale audio streams in memory).
     * - 120 s read timeout — long enough for slow connections, short enough to
     *   eventually fail if the remote server keeps the connection open forever
     *   (e.g. a misrouted request that gets an HTML keep-alive response).
     * - 30 s connect timeout to fail fast if the CDN is unreachable.
     */
    @Provides
    @Singleton
    @DownloadOkHttpClient
    fun provideDownloadOkHttpClient(
        userAgentInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @YoutubeRetrofit
    fun provideYoutubeRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.YOUTUBE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideYoutubeApiService(@YoutubeRetrofit retrofit: Retrofit): YoutubeApiService =
        retrofit.create(YoutubeApiService::class.java)

    @Provides
    @Singleton
    fun provideBackendApiService(retrofit: Retrofit): BackendApiService =
        retrofit.create(BackendApiService::class.java)
}
