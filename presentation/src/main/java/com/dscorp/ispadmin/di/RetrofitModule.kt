package com.dscorp.ispadmin.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.dscorp.ispadmin.data.utils.LocalDateTimeAdapter
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

const val BASE_URL = "BASE_URL"

val retrofitModule = module {
    single { provideRetrofit(getProperty(BASE_URL), provideHttpClient(get())) }
    single { provideHttpClient(get()) }
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


fun provideHttpClient(context:Context): OkHttpClient {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    httpClient.addInterceptor(logging)
    httpClient.addInterceptor(ChuckerInterceptor.Builder(context).build())

    return httpClient.build()
}

