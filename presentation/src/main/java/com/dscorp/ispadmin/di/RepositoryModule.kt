package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepositoryImpl
import com.dscorp.ispadmin.data.repository.OutlayReceiptPreparerImpl
import com.dscorp.ispadmin.data.repository.OutlayRepositoryImpl
import com.dscorp.ispadmin.data.repository.PaymentRepositoryImpl
import com.dscorp.ispadmin.data.repository.Repository
import com.dscorp.ispadmin.data.repository.UserRepository
import com.dscorp.ispadmin.data.repository.UserRepositoryImpl
import com.dscorp.ispadmin.data.repository.adapters.PlanRepositoryAdapter
import com.dscorp.ispadmin.data.repository.adapters.SubscriptionActionsRepositoryAdapter
import com.dscorp.ispadmin.data.repository.adapters.SubscriptionRegistrationQueryRepositoryAdapter
import com.dscorp.ispadmin.data.repository.adapters.SubscriptionWriteRepositoryAdapter
import com.dscorp.ispadmin.data.repository.adapters.UserSessionReaderAdapter
import com.dscorp.ispadmin.domain.repository.OutlayReceiptPreparer
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import com.dscorp.ispadmin.domain.repository.PlanRepository
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import com.dscorp.ispadmin.domain.repository.UserSessionReader
import org.koin.dsl.module

val repositoryModule = module {
    single<IRepository> { Repository() }
    single<InstallationOrderRepository> { InstallationOrderRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
    single<OutlayRepository> { OutlayRepositoryImpl(get()) }
    single<OutlayReceiptPreparer> { OutlayReceiptPreparerImpl(get()) }
    single<PlanRepository> { PlanRepositoryAdapter(get()) }
    single<SubscriptionActionsRepository> { SubscriptionActionsRepositoryAdapter(get()) }
    single<SubscriptionRegistrationQueryRepository> { SubscriptionRegistrationQueryRepositoryAdapter(get()) }
    single<UserSessionReader> { UserSessionReaderAdapter(get()) }
    single<SubscriptionWriteRepository> { SubscriptionWriteRepositoryAdapter(get()) }
}
