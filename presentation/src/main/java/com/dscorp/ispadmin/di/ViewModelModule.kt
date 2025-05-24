package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo de Koin para inyectar ViewModels relacionados con órdenes de instalación
 */
val viewModelModule = module {
    // Proveemos el ViewModel con sus dependencias
    viewModel { 
        InstallationOrderListViewModel(
            repository = get(),
            userUseCase = get()
        )
    }
} 