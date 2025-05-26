package com.dscorp.ispadmin.presentation.di.app

import android.app.Application
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.di.BASE_URL
import com.dscorp.ispadmin.di.STORAGE_BASE_URL
import com.dscorp.ispadmin.di.apiModule
import com.dscorp.ispadmin.di.localDataModule
import com.dscorp.ispadmin.di.repositoryModule
import com.dscorp.ispadmin.di.retrofitModule
import com.dscorp.ispadmin.presentation.di.modules.applicationModule
import com.dscorp.ispadmin.presentation.di.modules.dialogFactoryModule
import com.dscorp.ispadmin.presentation.di.modules.formFieldModule
import com.dscorp.ispadmin.presentation.di.modules.useCaseModule
import com.dscorp.ispadmin.presentation.di.modules.viewModelModule
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
                formFieldModule,
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
