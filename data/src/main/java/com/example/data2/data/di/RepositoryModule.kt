package com.example.data2.data.di

import com.dscorp.ispadmin.data.repository.PaymentRepositoryImpl
import com.dscorp.ispadmin.domain.repository.PaymentRepository
import com.example.data2.data.repository.IRepository
import com.example.data2.data.repository.InstallationOrderRepository
import com.example.data2.data.repository.InstallationOrderRepositoryImpl
import com.example.data2.data.repository.Repository
import com.example.data2.data.repository.UserRepository
import com.example.data2.data.repository.UserRepositoryImpl
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
    single<InstallationOrderRepository> { InstallationOrderRepositoryImpl() }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
}
