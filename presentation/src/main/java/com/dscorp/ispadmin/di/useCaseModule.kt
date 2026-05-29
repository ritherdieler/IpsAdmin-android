package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.data.usecase.InstallationOrderUseCaseImpl
import com.dscorp.ispadmin.data.usecase.PlaceUseCaseImpl
import com.dscorp.ispadmin.data.usecase.UpdateDeviceTokenUseCaseImpl
import com.dscorp.ispadmin.data.usecase.UserUseCaseImpl
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.PlaceUseCase
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import com.dscorp.ispadmin.domain.usecase.outlay.RegisterOutlayUseCase
import com.dscorp.ispadmin.domain.usecase.payment.GetPaymentByIdUseCase
import com.dscorp.ispadmin.domain.usecase.plan.GetPlanListUseCase
import com.dscorp.ispadmin.domain.usecase.plan.UpdatePlanUseCase
import com.dscorp.ispadmin.domain.usecase.service.ReactivateServiceUseCase
import com.dscorp.ispadmin.domain.usecase.service.RebootFiberOnuUseCase
import com.dscorp.ispadmin.domain.usecase.service.RestoreInternetConnectionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetCoreDevicesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNapBoxListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { GetAvailableOnuListUseCase(get()) }
    single { GetPlanListUseCase(get()) }
    single { GetPlaceListUseCase(get()) }
    single { GetPlaceFromLocationUseCase(get()) }
    single { GetNapBoxListUseCase(get()) }
    single { RegisterSubscriptionUseCase(get(), get()) }
    single { GetUserSessionUseCase(get()) }
    single { GetCoreDevicesUseCase(get()) }
    single { GetNearNapBoxesUseCase(get()) }
    single<InstallationOrderUseCase> { InstallationOrderUseCaseImpl(get()) }
    single<UserUseCase> { UserUseCaseImpl(get()) }
    single<PlaceUseCase> { PlaceUseCaseImpl(get()) }
    single<UpdateDeviceTokenUseCase> { UpdateDeviceTokenUseCaseImpl(get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { UpdatePlanUseCase(get()) }
    single { ReactivateServiceUseCase(get()) }
    single { RebootFiberOnuUseCase(get()) }
    single { RestoreInternetConnectionUseCase(get()) }
    single { RegisterOutlayUseCase(get(), get()) }
}
