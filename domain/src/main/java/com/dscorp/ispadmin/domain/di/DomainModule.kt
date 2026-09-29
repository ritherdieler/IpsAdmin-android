package com.dscorp.ispadmin.domain.di

import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.RefreshRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.EnqueuePendingSubscriptionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObserveOfflineRegistrationModeUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.SyncPendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.OnuRegistrationOperationUseCase
import org.koin.dsl.module

val domainModule = module {
    single { GetRegistrationCatalogUseCase(get()) }
    single { RefreshRegistrationCatalogUseCase(get()) }
    single { EnqueuePendingSubscriptionUseCase(get(), get()) }
    single { ObservePendingSubscriptionsUseCase(get()) }
    single { ObserveOfflineRegistrationModeUseCase(get(), get()) }
    single { SyncPendingSubscriptionsUseCase(get(), get(), get(), get()) }
    single { OnuRegistrationOperationUseCase(get()) }
}
