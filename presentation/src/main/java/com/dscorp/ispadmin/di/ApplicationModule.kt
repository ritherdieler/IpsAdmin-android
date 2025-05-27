package com.dscorp.ispadmin.di

import org.koin.dsl.module

val applicationModule = module {
    single<ResourceProvider> { ResourceProviderImpl(get()) }
}