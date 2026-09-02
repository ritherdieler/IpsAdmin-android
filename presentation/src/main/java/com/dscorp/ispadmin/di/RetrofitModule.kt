package com.dscorp.ispadmin.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.dscorp.ispadmin.data.datasource.remote.auth.AuthApiService
import com.dscorp.ispadmin.data.datasource.remote.auth.AuthInterceptor
import com.dscorp.ispadmin.data.datasource.remote.auth.SessionEventBus
import com.dscorp.ispadmin.data.datasource.remote.auth.TokenAuthenticator
import com.dscorp.ispadmin.data.datasource.remote.auth.TokenStore
import com.dscorp.ispadmin.data.utils.LocalDateTimeAdapter
import com.dscorp.ispadmin.observability.ObservabilityHttpInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

const val BASE_URL = "BASE_URL"

val retrofitModule = module {
    single { SessionEventBus() }
    single { TokenStore(get()) }
    single { provideAuthApiService(getProperty(BASE_URL)) }
    single { AuthInterceptor(get(), getProperty(BASE_URL)) }
    single { TokenAuthenticator(get(), get(), get()) }
    single { provideHttpClient(get(), get(), get(), get()) }
    single { provideRetrofit(getProperty(BASE_URL), get()) }
}

fun provideRetrofit(url: String, okHttpClient: OkHttpClient): Retrofit {
    val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()
}

fun provideAuthApiService(url: String): AuthApiService {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(AuthApiService::class.java)
}

fun provideHttpClient(
    context: Context,
    observabilityHttpInterceptor: ObservabilityHttpInterceptor,
    authInterceptor: AuthInterceptor,
    tokenAuthenticator: TokenAuthenticator
): OkHttpClient {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(90, TimeUnit.SECONDS)
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    httpClient.addInterceptor(authInterceptor)
    httpClient.addInterceptor(observabilityHttpInterceptor)
    httpClient.addInterceptor(logging)
    httpClient.addInterceptor(ChuckerInterceptor.Builder(context).build())
    httpClient.authenticator(tokenAuthenticator)

    return httpClient.build()
}
