package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashBoardViewModel
import com.dscorp.ispadmin.presentation.ui.features.fixedCost.FixedCostViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorders.CreateInstallationOrderViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderListViewModel
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivityViewModel
import com.dscorp.ispadmin.presentation.ui.features.main.MainViewModel
import com.dscorp.ispadmin.presentation.ui.features.migration.MigrationViewModel
import com.dscorp.ispadmin.presentation.ui.features.mufas.MufaViewModel
import com.dscorp.ispadmin.presentation.ui.features.napbox.NapBoxViewModel
import com.dscorp.ispadmin.presentation.ui.features.oltadministrator.OltAdministrationViewModel
import com.dscorp.ispadmin.presentation.ui.features.outlay.OutLayViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.history.PaymentHistoryViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder.PayerFinderViewmodel
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentViewModel
import com.dscorp.ispadmin.presentation.ui.features.plan.PlanListViewModel
import com.dscorp.ispadmin.presentation.ui.features.profile.ProfileViewModel
import com.dscorp.ispadmin.presentation.ui.features.registration.RegisterViewModel
import com.dscorp.ispadmin.presentation.ui.features.report.ReportsViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.EditSubscriptionViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.pending.PendingSubscriptionsViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterSubscriptionComposeViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.RepositorySubscriptionHubGateway
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration.AccessMigrationViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SharedPrefsRecentSubscriptionsStore
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.SupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Created by Sergio Carrillo Diestra on 20/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
val viewModelModule = module {
    viewModel { com.dscorp.ispadmin.presentation.ui.features.financial.FinancialViewModel(get(), get<CoroutineDispatcher>(named("mainImmediate"))) }
    viewModel { com.dscorp.ispadmin.presentation.ui.features.dashboard.clients.ClientsViewModel(get(), get<CoroutineDispatcher>(named("mainImmediate"))) }
    viewModel { com.dscorp.ispadmin.presentation.ui.features.dashboard.CollectionsViewModel(get(), get<CoroutineDispatcher>(named("mainImmediate"))) }
    viewModel { com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning.ProvisioningViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get(), get(), get()) }
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
            get(),
            get(),
            get(),
            get<CoroutineDispatcher>(named("mainImmediate")),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    viewModel { NapBoxViewModel(get(), get()) }
    viewModel { PaymentHistoryViewModel(get(), get(), get(), get()) }
    viewModel { PaymentDetailViewModel(get(), get()) }
    viewModel { RegisterPaymentViewModel(get(), get()) }
    viewModel { ProfileViewModel() }
    viewModel { MainActivityViewModel() }
    viewModel { ReportsViewModel(get(), get()) }
    viewModel { DashBoardViewModel() }
    viewModel { MufaViewModel(get(), get()) }
    viewModel { EditSubscriptionViewModel(get(), get()) }
    viewModel { SubscriptionDetailViewModel(get(), get(), get()) }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.TicketDetailViewModel(
            gateway = com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.RepositoryTicketGateway(get(), get(), get(), get()),
            mainImmediate = get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.home.TechnicianTodayViewModel(
            com.dscorp.ispadmin.presentation.ui.features.home.RepositoryFieldGateway(get(), get(), get()),
            get<CoroutineDispatcher>(named("mainImmediate")),
        ) { java.time.Instant.now() }
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.home.SalesAgendaViewModel(
            com.dscorp.ispadmin.presentation.ui.features.home.RepositoryFieldGateway(get(), get(), get()),
            get<CoroutineDispatcher>(named("mainImmediate")),
        ) { java.time.Instant.now() }
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashDayViewModel(
            com.dscorp.ispadmin.presentation.ui.features.payment.cash.ApiCashDayGateway(get()),
            get<CoroutineDispatcher>(named("mainImmediate")),
        ) { java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")) }
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.home.HomeViewModel(
            gateway = com.dscorp.ispadmin.presentation.ui.features.home.ApiHomeGateway(get()),
            mainImmediate = get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.TicketInboxViewModel(
            gateway = com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.RepositoryTicketInboxGateway(get(), get()),
            mainImmediate = get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel {
        com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.CloseTicketViewModel(
            gateway = com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.RepositoryTicketGateway(get(), get(), get(), get()),
            mainImmediate = get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel {
        SubscriptionHubViewModel(
            gateway = RepositorySubscriptionHubGateway(get(), get(), get(), get(), get()),
            mainImmediate = get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel {
        AccessMigrationViewModel(
            get(),
            get(),
            get<CoroutineDispatcher>(named("mainImmediate")),
        )
    }
    viewModel { SupportTicketViewModel(get(), get(), get()) }
    viewModel { CreateSupportTicketViewModel(get(), get()) }
    viewModel { MigrationViewModel(get(), get()) }
    viewModel { OltAdministrationViewModel(get(), get()) }
    viewModel { OutLayViewModel(get(), get()) }
    viewModel {
        SubscriptionFinderViewModel(
            get(), get(), get(), get(), get(), get(), get(),
            recentSubscriptionsStore = SharedPrefsRecentSubscriptionsStore(get()),
        )
    }
    viewModel { FixedCostViewModel(get(), get()) }
    viewModel { CreateInstallationOrderViewModel(get(), get(), get(), get()) }
    viewModel { SupportTicketListViewModel(get(), get(), get()) }
    viewModel { InstallationOrderListViewModel(get(), get(), get()) }
    viewModel { PayerFinderViewmodel(get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { PlanListViewModel(get(), get(), get()) }
    viewModel {
        PendingSubscriptionsViewModel(
            get(),
            get(),
            get<CoroutineDispatcher>(named("mainImmediate")),
            discardPendingSubscriptionUseCase = get(),
        )
    }

}
