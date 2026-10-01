package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.observability.AppObsAppInfo
import com.dscorp.ispadmin.observability.AppObsUserProvider
import com.dscorp.ispadmin.observability.FirebaseObsCrashReporter
import com.dscorp.ispadmin.observability.ObsAppInfo
import com.dscorp.ispadmin.observability.ObsCrashReporter
import com.dscorp.ispadmin.observability.ObsUserProvider
import com.dscorp.ispadmin.observability.ObservabilityApi
import com.dscorp.ispadmin.observability.ObservabilityApiKeyInterceptor
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.observability.ObservabilityConfig
import com.dscorp.ispadmin.observability.ObservabilityConnectivityMonitor
import com.dscorp.ispadmin.observability.ObservabilityContextProvider
import com.dscorp.ispadmin.observability.ObservabilityHttpInterceptor
import com.dscorp.ispadmin.observability.ObservabilityEventStore
import com.dscorp.ispadmin.observability.ObservabilityFlushScheduler
import com.dscorp.ispadmin.observability.ObservabilityQualifiers
import com.dscorp.ispadmin.observability.ObservabilityQueue
import com.dscorp.ispadmin.observability.ObservabilityReplayConfig
import com.dscorp.ispadmin.observability.ObservabilityReplaySender
import com.dscorp.ispadmin.observability.ObservabilityScreenRecorder
import com.dscorp.ispadmin.observability.ObservabilitySpanApi
import com.dscorp.ispadmin.observability.ObservabilityTracer
import com.dscorp.ispadmin.observability.ProdObsApiKeyResolver
import com.dscorp.ispadmin.observability.ObservabilityWorkScheduler
import com.dscorp.ispadmin.observability.ObservabilityActivityTracker
import com.dscorp.ispadmin.observability.ObservabilityComposeClick
import com.dscorp.ispadmin.observability.ObservabilityComposeText
import com.dscorp.ispadmin.observability.ObservabilityUiCapture
import com.dscorp.ispadmin.observability.ObservabilityUiTracker
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val OBS_BASE_URL = "OBS_BASE_URL"

val observabilityQualifier = ObservabilityQualifiers.gson
val observabilitySpansQueueQualifier = ObservabilityQualifiers.spansQueue

val observabilityModule = module {

    single<ObsAppInfo> { AppObsAppInfo() }
    single<ObsUserProvider> { AppObsUserProvider(get()) }
    single<ObsCrashReporter> { FirebaseObsCrashReporter() }
    single {
        ObservabilityConfig(
            apiKey = resolveObservabilityApiKey(
                flavor = BuildConfig.FLAVOR,
                buildConfigKey = BuildConfig.OBS_API_KEY,
                buildConfigAndroidKey = BuildConfig.OBS_API_KEY_ANDROID
            ),
            sanitizePayloads = BuildConfig.FLAVOR != "dev"
        )
    }

    single(ObservabilityQualifiers.gson) { Gson() }

    single(ObservabilityQualifiers.httpClient) {
        provideObservabilityHttpClient(
            get(ObservabilityQualifiers.gson),
            get<ObservabilityConfig>().apiKey
        )
    }

    single(ObservabilityQualifiers.retrofit) {
        provideObservabilityRetrofit(
            url = getProperty(OBS_BASE_URL),
            okHttpClient = get(ObservabilityQualifiers.httpClient),
            gson = get(ObservabilityQualifiers.gson)
        )
    }

    single { get<Retrofit>(ObservabilityQualifiers.retrofit).create(ObservabilityApi::class.java) }

    single { get<Retrofit>(ObservabilityQualifiers.retrofit).create(ObservabilitySpanApi::class.java) }

    single { ObservabilityContextProvider(get(), get()) }

    single { ObservabilityQueue(androidContext()) } bind ObservabilityEventStore::class

    single(ObservabilityQualifiers.spansQueue) {
        ObservabilityQueue(androidContext(), "spans-queue.jsonl")
    } bind ObservabilityEventStore::class

    single { ObservabilityWorkScheduler(androidContext()) } bind ObservabilityFlushScheduler::class

    single { ObservabilityReplayConfig() }

    single { ObservabilityScreenRecorder(config = get()) }

    single {
        ObservabilityReplaySender(
            api = get(),
            recorder = get(),
            gson = get(ObservabilityQualifiers.gson),
            apiKey = get<ObservabilityConfig>().apiKey,
            config = get()
        )
    }

    single {
        ObservabilityClient(
            api = get(),
            queue = get(),
            contextProvider = get(),
            gson = get(ObservabilityQualifiers.gson),
            apiKey = get<ObservabilityConfig>().apiKey,
            workScheduler = get(),
            replaySender = get(),
            crashReporter = get(),
            config = get()
        )
    }

    single {
        ObservabilityUiCapture(clientProvider = lazy { get<ObservabilityClient>() }).also {
            ObservabilityComposeText.bind(it)
            ObservabilityComposeClick.bind(it)
        }
    }

    single { ObservabilityUiTracker(uiCapture = get()) }

    single { ObservabilityActivityTracker(recorder = get(), uiTracker = get()) }

    single {
        ObservabilityTracer(
            api = get(),
            queue = get(ObservabilityQualifiers.spansQueue),
            contextProvider = get(),
            gson = get(ObservabilityQualifiers.gson),
            apiKey = get<ObservabilityConfig>().apiKey,
            tagsProvider = { get<ObservabilityClient>().currentWorkflowTags() }
        )
    }

    single {
        ObservabilityHttpInterceptor(
            clientProvider = lazy { get<ObservabilityClient>() },
            tracerProvider = lazy { get<ObservabilityTracer>() }
        )
    }

    single {
        ObservabilityConnectivityMonitor(
            context = androidContext(),
            clientProvider = lazy { get<ObservabilityClient>() },
            tracerProvider = lazy { get<ObservabilityTracer>() }
        )
    }
}

internal fun resolveObservabilityApiKey(
    flavor: String,
    buildConfigKey: String,
    buildConfigAndroidKey: String = ""
): String =
    if (flavor.equals("dev", ignoreCase = true)) {
        "dev-obs-android-key"
    } else {
        ProdObsApiKeyResolver.resolve(buildConfigAndroidKey, buildConfigKey)
    }

private fun provideObservabilityHttpClient(gson: Gson, apiKey: String): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(ObservabilityApiKeyInterceptor(apiKey))
    if (BuildConfig.DEBUG) {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC
        builder.addInterceptor(logging)
    }
    return builder.build()
}

private fun provideObservabilityRetrofit(url: String, okHttpClient: OkHttpClient, gson: Gson): Retrofit =
    Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()
