package com.dscorp.ispadmin.data.di

import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApi
import com.dscorp.ispadmin.data.datasource.remote.WispApiService
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApiService
import com.dscorp.ispadmin.data.datasource.remote.SendMessagingCloudApi
import com.example.data2.data.datasource.RestApiServices
import org.koin.dsl.module
import retrofit2.Retrofit
import kotlin.jvm.java

val apiModule = module {
    single { providesApi(get()) }
    single { provideFirebaseCloudMessagingApi(provideRetrofit(BuildConfig.FIRE_BASE_URL, get())) }
    single { provideInstallationOrderApi(get()) }
    single { provideInstallationOrderApiDirect(get()) }
    single<WispApiService> {
        get<Retrofit>().create(WispApiService::class.java)
    }
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

fun provideInstallationOrderApiDirect(retrofit: Retrofit): InstallationOrderApi {
    return retrofit.create(InstallationOrderApi::class.java)
}
