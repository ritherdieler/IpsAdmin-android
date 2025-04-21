package com.example.data2.data.di

import com.example.data2.BuildConfig
import com.example.data2.data.datasource.InstallationOrderApiService
import com.example.data2.data.datasource.RestApiServices
import com.example.data2.data.datasource.SendMessagingCloudApi
import org.koin.dsl.module
import retrofit2.Retrofit

val apiModule = module {
    single { providesApi(get()) }
    single { provideFirebaseCloudMessagingApi(provideRetrofit(BuildConfig.FIRE_BASE_URL, get())) }
    single { provideInstallationOrderApi(get()) }
}

fun providesApi(retrofit: Retrofit): RestApiServices {
    return retrofit.create(RestApiServices::class.java)
}

fun provideFirebaseCloudMessagingApi(retrofit: Retrofit): SendMessagingCloudApi {
    return retrofit.create(SendMessagingCloudApi::class.java)
}

fun provideInstallationOrderApi(retrofit: Retrofit): InstallationOrderApiService {
    return retrofit.create(InstallationOrderApiService::class.java)
}

