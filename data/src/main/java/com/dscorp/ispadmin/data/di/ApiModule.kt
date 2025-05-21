package com.dscorp.ispadmin.data.di

import com.dscorp.ispadmin.data.remote.WispApiService
import com.example.data2.data.datasource.RestApiServices
import org.koin.dsl.module
import retrofit2.Retrofit

val apiModule = module {
    single<WispApiService> { 
        get<Retrofit>().create(WispApiService::class.java) 
    }
} 