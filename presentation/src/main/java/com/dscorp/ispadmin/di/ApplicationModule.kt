package com.dscorp.ispadmin.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val applicationModule = module {
    single<ResourceProvider> { ResourceProviderImpl(get()) }
    single<CoroutineDispatcher>(named("mainImmediate")) { Dispatchers.Main.immediate }
}