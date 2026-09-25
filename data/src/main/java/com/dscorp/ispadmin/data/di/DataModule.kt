package com.dscorp.ispadmin.data.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import com.dscorp.ispadmin.data.connectivity.MikrotikReachabilityMonitorImpl
import com.dscorp.ispadmin.data.connectivity.NetworkConnectivityMonitorImpl
import com.dscorp.ispadmin.data.local.IspAdminDatabase
import com.dscorp.ispadmin.data.local.PendingFacadePhotoStorage
import com.dscorp.ispadmin.data.remote.PendingSubscriptionSyncApi
import com.dscorp.ispadmin.data.remote.RegistrationCatalogApi
import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSource
import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSourceImpl
import com.dscorp.ispadmin.data.remote.SubscriptionSyncRemoteImpl
import com.dscorp.ispadmin.data.repository.PendingSubscriptionRepositoryImpl
import com.dscorp.ispadmin.data.repository.RegistrationCatalogRepositoryImpl
import com.dscorp.ispadmin.domain.connectivity.MikrotikReachabilityMonitor
import com.dscorp.ispadmin.domain.connectivity.NetworkConnectivityMonitor
import com.dscorp.ispadmin.domain.repository.PendingSubscriptionRepository
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import com.dscorp.ispadmin.domain.repository.SubscriptionSyncRemote
import com.dscorp.ispadmin.domain.storage.FacadePhotoStorage
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val dataModule = module {
    single { get<Retrofit>().create(com.dscorp.ispadmin.data.remote.ProvisioningApi::class.java) }
    single<com.dscorp.ispadmin.domain.repository.ProvisioningRepository> {
        com.dscorp.ispadmin.data.repository.ProvisioningRepositoryImpl(get())
    }
    single {
        Room.databaseBuilder(
            androidContext(),
            IspAdminDatabase::class.java,
            "ispadmin.db"
        ).build()
    }
    single { get<IspAdminDatabase>().pendingSubscriptionDao() }
    single { get<IspAdminDatabase>().catalogDao() }
    single { PendingFacadePhotoStorage(androidContext()) }
    single<FacadePhotoStorage> { get<PendingFacadePhotoStorage>() }
    single<PendingSubscriptionRepository> {
        PendingSubscriptionRepositoryImpl(get())
    }
    single { get<Retrofit>().create(RegistrationCatalogApi::class.java) }
    single<RegistrationCatalogRemoteDataSource> {
        RegistrationCatalogRemoteDataSourceImpl(get())
    }
    single<RegistrationCatalogRepository> {
        RegistrationCatalogRepositoryImpl(get(), get())
    }
    single<NetworkConnectivityMonitor> {
        NetworkConnectivityMonitorImpl(
            androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
    }
    single<MikrotikReachabilityMonitor> {
        MikrotikReachabilityMonitorImpl(
            okHttpClient = get<OkHttpClient>(),
            baseUrl = getProperty("BASE_URL")
        )
    }
    single { get<Retrofit>().create(PendingSubscriptionSyncApi::class.java) }
    single<SubscriptionSyncRemote> { SubscriptionSyncRemoteImpl(get()) }
}
