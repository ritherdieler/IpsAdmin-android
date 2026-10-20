package com.dscorp.ispadmin.domain.di

import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.DiscardPendingSubscriptionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.SyncPendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.OnuRegistrationOperationUseCase
import org.koin.dsl.module

val domainModule = module {
    single { GetRegistrationCatalogUseCase(get()) }
    single { ObservePendingSubscriptionsUseCase(get()) }
    single { SyncPendingSubscriptionsUseCase(get(), get(), get(), get()) }
    single { DiscardPendingSubscriptionUseCase(get(), get()) }
    single { OnuRegistrationOperationUseCase(get()) }
}
