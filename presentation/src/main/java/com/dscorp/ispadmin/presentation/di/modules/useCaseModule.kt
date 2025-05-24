package com.dscorp.ispadmin.presentation.di.modules

import com.dscorp.ispadmin.data.usecase.InstallationOrderUseCaseImpl
import com.dscorp.ispadmin.data.usecase.PlaceUseCaseImpl
import com.dscorp.ispadmin.data.usecase.UpdateDeviceTokenUseCaseImpl
import com.dscorp.ispadmin.data.usecase.UserUseCaseImpl
import com.dscorp.ispadmin.domain.usecase.GetPaymentByIdUseCase
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.PlaceUseCase
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetCoreDevicesUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetNapBoxListUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetPlaceListUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetPlanListUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetUserSessionUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterSubscriptionUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { GetAvailableOnuListUseCase(get()) }
    single { GetPlanListUseCase(get()) }
    single { GetPlaceListUseCase(get()) }
    single { GetPlaceFromLocationUseCase(get()) }
    single { GetNapBoxListUseCase(get()) }
    single { RegisterSubscriptionUseCase(get()) }
    single { GetUserSessionUseCase(get()) }
    single { GetCoreDevicesUseCase(get()) }
    single { GetNearNapBoxesUseCase(get()) }
    single<InstallationOrderUseCase> { InstallationOrderUseCaseImpl(get()) }
    single<UserUseCase> { UserUseCaseImpl(get()) }
    single<PlaceUseCase> { PlaceUseCaseImpl(get()) }
    single<UpdateDeviceTokenUseCase> { UpdateDeviceTokenUseCaseImpl(get()) }
    single { GetPaymentByIdUseCase(get()) }
}
