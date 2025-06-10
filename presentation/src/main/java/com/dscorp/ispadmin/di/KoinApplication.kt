package com.dscorp.ispadmin.di

import android.app.Application
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.di.apiModule
import com.example.data2.data.di.fileStorageModule
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.dscorp.ispadmin.di.apiModule as wispApiModule

class KoinApplication : Application() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)
        firebaseAnalytics = Firebase.analytics

        startKoin {
            androidContext(this@KoinApplication)
            allowOverride(true)
            modules(
                retrofitModule,
                apiModule,
                wispApiModule,
                repositoryModule,
                viewModelModule,
                dialogFactoryModule,
                localDataModule,
                applicationModule,
                useCaseModule,
                module {
                    single { firebaseAnalytics }
                },
                fileStorageModule,
            )
        }

        getKoin().run {
            setProperty(BASE_URL, BuildConfig.BASE_URL)
            setProperty(STORAGE_BASE_URL, BuildConfig.FIREBASE_STORAGE_BUCKET)
        }
    }
}
