package com.dscorp.ispadmin.TestApp

import android.app.Application
import com.example.data2.data.di.apiModule
import com.example.data2.data.di.repositoryModule
import com.dscorp.ispadmin.presentation.di.modules.viewModelModule
import com.example.data2.data.di.BASE_URL
import com.example.data2.data.di.localDataModule
import com.example.data2.data.di.retrofitModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Created by Sergio Carrillo Diestra on 25/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 */
class KoinAppForInstrumentation : Application() {
    private val FAKE_BASE_URL = "http://127.0.0.1:8081"

    override fun onCreate() = runTest {
        super.onCreate()

        launch {
            startKoin {
                androidContext(this@KoinAppForInstrumentation)

                getKoin().run {
                    setProperty(BASE_URL, FAKE_BASE_URL)
                }

                modules(
                    listOf(
                        apiModule,
                        retrofitModule,
                        viewModelModule,
                        repositoryModule,
                        localDataModule,
                    )
                )
            }
        }
    }

}
