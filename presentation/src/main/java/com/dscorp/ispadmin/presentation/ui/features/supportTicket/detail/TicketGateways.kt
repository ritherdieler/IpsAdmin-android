package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import com.dscorp.ispadmin.data.apirequestmodel.RescheduleTicketRequest
import com.dscorp.ispadmin.data.datasource.remote.FichaApi
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.service.RebootFiberOnuUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import java.io.File

/** Ticket screens over the existing repository calls and the ficha API. */
class RepositoryTicketGateway(
    private val repository: IRepository,
    private val fichaApi: FichaApi,
    private val rebootFiberOnu: RebootFiberOnuUseCase,
    private val retryTr069: RetryTr069ProvisioningUseCase,
) : TicketDetailGateway, CloseTicketGateway {
    override suspend fun ticket(ticketId: Int): AssistanceTicketResponse = repository.getTicket(ticketId.toString())
    override suspend fun subscription(subscriptionId: Int): SubscriptionResponse = repository.subscriptionById(subscriptionId)
    override suspend fun ticketsBySubscription(subscriptionId: Int) = fichaApi.ticketsBySubscription(subscriptionId)
    override suspend fun cpeReading(subscriptionId: Int): CpeReading = fichaApi.cpeStatus(subscriptionId).toReading()
    override suspend fun technicians(): List<User> = repository.getTechnicians()
    override suspend fun take(ticketId: Int, userId: Int) {
        repository.assignSupportTicketToUser(ticketId, AssistanceTicketStatus.ASSIGNED, userId)
    }
    override suspend fun assign(ticketId: Int, userId: Int) {
        repository.assignSupportTicketToUser(ticketId, AssistanceTicketStatus.ASSIGNED, userId)
    }
    override suspend fun reschedule(ticketId: Int, scheduledAt: Long) {
        repository.rescheduleTicket(ticketId, RescheduleTicketRequest(scheduledAt))
    }
    override suspend fun cancel(ticketId: Int, userId: Int) {
        repository.closeUnattendedTicket(ticketId, AssistanceTicketStatus.CANCELLED, userId)
    }
    override suspend fun close(ticketId: Int, userId: Int, photo: File) {
        repository.closeTicket(ticketId, AssistanceTicketStatus.CLOSED, userId, photo)
    }
    override suspend fun rebootOnu(subscriptionId: Int): Result<Unit> = rebootFiberOnu(subscriptionId)
    override suspend fun resendConfig(subscriptionId: Int): Result<Unit> = retryTr069(subscriptionId).map { }
    override fun currentUser(): User? = repository.getUserSession()
}
