package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.presentation.util.DialogFactory
import com.dscorp.ispadmin.presentation.util.IDialogFactory
import org.koin.dsl.module

/**
 * Created by Sergio Carrillo Diestra on 20/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/

val dialogFactoryModule = module {
    single<IDialogFactory> { DialogFactory() }
}
