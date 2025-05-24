package com.dscorp.ispadmin.data.di

import android.content.Context
import android.content.SharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localDataModule = module {
    single<SharedPreferences>{
          androidContext().getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    }
}