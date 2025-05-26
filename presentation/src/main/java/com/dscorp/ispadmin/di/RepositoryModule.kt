package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepositoryImpl
import com.dscorp.ispadmin.data.repository.PaymentRepositoryImpl
import com.dscorp.ispadmin.data.repository.Repository
import com.dscorp.ispadmin.data.repository.UserRepository
import com.dscorp.ispadmin.data.repository.UserRepositoryImpl
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<IRepository> { Repository() }
    single<InstallationOrderRepository> { InstallationOrderRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
}
