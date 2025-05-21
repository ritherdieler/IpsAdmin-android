package com.dscorp.ispadmin.presentation.di.modules

import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashBoardViewModel
import com.dscorp.ispadmin.presentation.ui.features.fixedCost.FixedCostViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorder.AssignedInstallationOrdersViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorder.InstallationOrderViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorder.PendingInstallationOrdersViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorder.SellerClosedOrdersViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorder.SellerInProgressOrdersViewModel
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivityViewModel
import com.dscorp.ispadmin.presentation.ui.features.migration.MigrationViewModel
import com.dscorp.ispadmin.presentation.ui.features.mufas.MufaViewModel
import com.dscorp.ispadmin.presentation.ui.features.napbox.NapBoxViewModel
import com.dscorp.ispadmin.presentation.ui.features.napboxeslist.NapBoxesListViewModel
import com.dscorp.ispadmin.presentation.ui.features.oltadministrator.OltAdministrationViewModel
import com.dscorp.ispadmin.presentation.ui.features.outlay.OutLayViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.history.PaymentHistoryViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentViewModel

import com.dscorp.ispadmin.presentation.ui.features.profile.ProfileViewModel
import com.dscorp.ispadmin.presentation.ui.features.registration.RegisterViewModel
import com.dscorp.ispadmin.presentation.ui.features.report.ReportsViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.EditSubscriptionViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterSubscriptionComposeViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.SupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListViewModel

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Created by Sergio Carrillo Diestra on 20/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get(), get()) }
    viewModel {
        RegisterSubscriptionComposeViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    viewModel { NapBoxViewModel(get()) }
    viewModel { NapBoxesListViewModel() }
    viewModel { PaymentHistoryViewModel(get()) }
    viewModel { PaymentDetailViewModel(get()) }
//    viewModel { FindSubscriptionViewModel(get(), get()) }
    viewModel { RegisterPaymentViewModel(get()) }
    viewModel { ProfileViewModel() }
    viewModel { MainActivityViewModel() }
    viewModel { ReportsViewModel(get()) }
    viewModel { DashBoardViewModel() }
    viewModel { MufaViewModel(get()) }
    viewModel { EditSubscriptionViewModel(get()) }
    viewModel { SubscriptionDetailViewModel(get(), get()) }
    viewModel { SupportTicketViewModel(get(), get()) }
    viewModel { CreateSupportTicketViewModel(get()) }
    viewModel { MigrationViewModel(get()) }
    viewModel { OltAdministrationViewModel(get()) }
    viewModel { OutLayViewModel(get()) }
    viewModel { SubscriptionFinderViewModel(get()) }
    viewModel { FixedCostViewModel(get()) }
    viewModel { InstallationOrderViewModel() }
    viewModel { PendingInstallationOrdersViewModel() }
    viewModel { AssignedInstallationOrdersViewModel() }
    viewModel { SellerInProgressOrdersViewModel() }
    viewModel { SellerClosedOrdersViewModel() }
    viewModel { SupportTicketListViewModel(get(), get()) }
}
