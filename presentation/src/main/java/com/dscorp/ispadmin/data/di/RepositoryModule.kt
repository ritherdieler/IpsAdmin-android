package com.dscorp.ispadmin.data.di

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepositoryImpl
import com.dscorp.ispadmin.data.repository.PaymentRepositoryImpl
import com.dscorp.ispadmin.data.repository.Repository
import com.dscorp.ispadmin.data.repository.UserRepository
import com.dscorp.ispadmin.data.repository.UserRepositoryImpl
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import org.koin.dsl.module

/**
 * Created by Sergio Carrillo Diestra on 20/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/

val repositoryModule = module {
    single<IRepository> { Repository() }
    single<InstallationOrderRepository> { InstallationOrderRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
}
