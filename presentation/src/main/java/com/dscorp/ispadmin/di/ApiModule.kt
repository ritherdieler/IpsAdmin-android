package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.datasource.remote.AccessMigrationApiService
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApi
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApiService
import com.dscorp.ispadmin.data.datasource.remote.SendMessagingCloudApi
import com.dscorp.ispadmin.data.datasource.remote.RestApiServices
import org.koin.dsl.module
import retrofit2.Retrofit

val apiModule = module {
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.financial.FinancialApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.dashboard.ClientsDashboardApi::class.java) }
    single { providesApi(get()) }
    single { provideFirebaseCloudMessagingApi(provideRetrofit(BuildConfig.FIRE_BASE_URL, get())) }
    single { provideInstallationOrderApi(get()) }
    single { provideInstallationOrderApiDirect(get()) }
    single { provideAccessMigrationApi(get()) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.FichaApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.HomeApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.CashApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.OutlayApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.ChargeApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.PlanApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.NapMoveApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.LocationApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.CutDeferralApi::class.java) }
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.datasource.remote.NetworkMapApi::class.java) }
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

fun provideAccessMigrationApi(retrofit: Retrofit): AccessMigrationApiService {
    return retrofit.create(AccessMigrationApiService::class.java)
}
