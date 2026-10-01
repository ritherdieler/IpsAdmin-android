package com.dscorp.ispadmin.di

import android.app.Application
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.di.dataModule
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.di.apiModule
import com.dscorp.ispadmin.domain.di.domainModule
import com.dscorp.ispadmin.observability.ObservabilityActivityTracker
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.observability.ObservabilityConnectivityMonitor
import com.dscorp.ispadmin.observability.ObservabilityCrashHandler
import com.dscorp.ispadmin.observability.ObservabilityTracer
import com.example.data2.data.di.fileStorageModule
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

        if (BuildConfig.FLAVOR == DEV_FLAVOR) {
            Stetho.initializeWithDefaults(this)
        } else {
            deleteDatabase(LEGACY_CHUCKER_DATABASE)
        }
        firebaseAnalytics = Firebase.analytics

        startKoin {
            androidContext(this@KoinApplication)
            allowOverride(true)
            modules(
                domainModule,
                dataModule,
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
                observabilityModule,
            )
        }

        getKoin().run {
            setProperty(BASE_URL, BuildConfig.BASE_URL)
            setProperty(OBS_BASE_URL, BuildConfig.OBS_BASE_URL)
        }

        setupCrashlytics()
        setupObservability()
    }

    private fun setupCrashlytics() {
        val repository = getKoin().get<IRepository>()
        val session = runCatching { repository.getUserSession() }.getOrNull()
        FirebaseCrashlytics.getInstance().apply {
            setUserId(session?.id?.toString() ?: "anonymous")
            setCustomKey("flavor", BuildConfig.FLAVOR)
            setCustomKey("versionName", BuildConfig.VERSION_NAME)
            setCustomKey("versionCode", BuildConfig.VERSION_CODE)
            setCustomKey("environment", if (BuildConfig.DEBUG) "debug" else "release")
        }
    }

    private fun setupObservability() {
        val observabilityClient = getKoin().get<ObservabilityClient>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            ObservabilityCrashHandler(observabilityClient, previousHandler)
        )
        observabilityClient.start()
        getKoin().get<ObservabilityTracer>().flush()
        getKoin().get<ObservabilityConnectivityMonitor>().register()
        registerActivityLifecycleCallbacks(getKoin().get<ObservabilityActivityTracker>())
    }

    private companion object {
        const val DEV_FLAVOR = "dev"
        const val LEGACY_CHUCKER_DATABASE = "chucker.db"
    }
}
