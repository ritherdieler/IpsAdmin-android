package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.AppVersion
import com.dscorp.ispadmin.domain.model.Coupon
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.FireBaseResponse
import com.dscorp.ispadmin.domain.model.FirebaseBody
import com.dscorp.ispadmin.domain.model.FixedCost
import com.dscorp.ispadmin.domain.model.Ip
import com.dscorp.ispadmin.domain.model.IpPool
import com.dscorp.ispadmin.domain.model.Loging
import com.dscorp.ispadmin.domain.model.Mufa
import com.dscorp.ispadmin.domain.model.NapBox
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.NetworkDeviceResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlaceResponse
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.ServiceOrder
import com.dscorp.ispadmin.domain.model.ServiceOrderResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.PayerFinderResult
import com.example.data2.data.apirequestmodel.AssistanceTicketRequest
import com.example.data2.data.apirequestmodel.FixedCostRequest
import com.example.data2.data.apirequestmodel.IpPoolRequest
import com.example.data2.data.apirequestmodel.MigrationRequest
import com.example.data2.data.apirequestmodel.MoveOnuRequest
import com.example.data2.data.apirequestmodel.SearchPaymentsRequest
import com.example.data2.data.apirequestmodel.UpdateSubscriptionDataBody
import com.example.data2.data.apirequestmodel.UpdateSubscriptionPlanBody
import com.example.data2.data.response.AdministrativeOnuResponse
import com.example.data2.data.response.AssistanceTicketResponse
import com.example.data2.data.response.AssistanceTicketStatus
import java.io.File

/**
 * Created by Sergio Carrillo Diestra on 25/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
interface IRepository {
    suspend fun registerUser(user: User): User
    suspend fun doLogin(login: Loging): User
    suspend fun saveUserSession(user: User, rememberSessionCheckBoxStatus: Boolean?)
    fun getUserSession(): User?
    fun getRememberSessionCheckBoxStatus(): Boolean
    fun clearUserSession()
    suspend fun registerPlan(plan: Plan): Plan
    suspend fun registerNetworkDevice(registerNetworkDevice: NetworkDevice): NetworkDevice
    suspend fun getGenericDevices(): List<NetworkDevice>
    suspend fun registerSubscription(subscription: Subscription): Subscription
    suspend fun getPlans(): List<PlanResponse>
    suspend fun getDevices(): List<NetworkDeviceResponse>
    suspend fun getSubscriptions(): List<SubscriptionResponse>
    suspend fun registerPlace(registerPlace: Place): Place
    suspend fun getPlaces(): List<PlaceResponse>
    suspend fun registerNapBox(napBox: NapBox): NapBox
    suspend fun registerServiceOrder(serviceOrder: ServiceOrder): ServiceOrder
    suspend fun getServicesOrder(): List<ServiceOrderResponse>
    suspend fun getTechnicians(): List<User>
    suspend fun getNapBoxes(): List<NapBoxResponse>
    suspend fun getNapBoxesOrderedByLocation(latitude:Double, longitude: Double): List<NapBoxResponse>

    suspend fun getFilteredPaymentHistory(request: SearchPaymentsRequest): List<Payment>
    suspend fun findSubscriptionByDNI(id: String): List<SubscriptionResume>
    suspend fun registerPayment(payment: Payment): Payment
    suspend fun getNetworkDeviceTypes(): List<String>
    suspend fun getDebtors(): List<SubscriptionResponse>
    suspend fun registerIpPool(ipPool: IpPoolRequest): IpPool
    suspend fun getIpPoolList(): List<IpPool>
    suspend fun getRecentPaymentsHistory(idSubscription: Int, itemsLimit: Int): List<Payment>
    suspend fun getCoreDevices(): List<NetworkDevice>
    suspend fun updateSubscriptionPlan(subscription: UpdateSubscriptionPlanBody): SubscriptionResponse
    suspend fun downloadDebtorWithActiveSubscriptionsReport(): DownloadDocumentResponse

    suspend fun downloadPaymentCommitmentSubscriptionsReport(): DownloadDocumentResponse

    suspend fun downloadSuspendedSubscriptionsReport(): DownloadDocumentResponse

    suspend fun downloadCutOffSubscriptionsReport(): DownloadDocumentResponse

    suspend fun downloadPastMonthDebtorsReport(): DownloadDocumentResponse

    suspend fun getDashBoardData(): DashBoardDataResponse
    suspend fun startServiceCut()
    suspend fun getHostDevices(): List<NetworkDevice>
    suspend fun getIpList(poolId: Int): List<Ip>
    suspend fun getCpeDevices(): List<NetworkDevice>
    suspend fun editNapBox(napBox: NapBox): NapBoxResponse
    suspend fun editServiceOrder(serviceOrder: ServiceOrder): ServiceOrderResponse
    suspend fun getMufas(): List<Mufa>
    suspend fun  getUnconfirmedOnus(): List<Onu>
    suspend fun applyCoupon(code: String): Coupon?
    suspend fun findSubscriptionBySubscriptionDate(
        startDate: String,
        endDate: String
    ): List<SubscriptionResume>

    suspend fun sendCloudMessaging(body: FirebaseBody?): FireBaseResponse
    suspend fun updatePlan(plan: Plan): PlanResponse
    suspend fun savePaymentCommitment(id: Int)
    suspend fun reactivateService(subscription: Int, responsibleId: Int)
    suspend fun findSubscriptionByNameAndLastName(
        name: String?,
        lastName: String?
    ): List<SubscriptionResume>

    suspend fun downloadDebtorsCutOffCandidatesSubscriptionsReport(): DownloadDocumentResponse
    suspend fun cancelSubscription(subscriptionId: Int)
    suspend fun updateSubscriptionData(subscriptionData: UpdateSubscriptionDataBody)
    suspend fun downloadDebtorWithCancelledSubscriptionsReport(): DownloadDocumentResponse
    suspend fun downloadCancelledSubscriptionsFromCurrentMonthReport(): DownloadDocumentResponse
    suspend fun downloadCancelledSubscriptionsFromPastMonthReport(): DownloadDocumentResponse

    suspend fun getTicket(ticketId: String): AssistanceTicketResponse
    suspend fun getTicketsByStatus(pending: AssistanceTicketStatus): List<AssistanceTicketResponse>
    suspend fun assignSupportTicketToUser(
        id: Int,
        newStatus: AssistanceTicketStatus,
        userId: Int,
    ): AssistanceTicketResponse

    suspend fun closeTicket(
        id: Int,
        newStatus: AssistanceTicketStatus,
        userId: Int,
        imageBase64: File
    ): AssistanceTicketResponse

    suspend fun closeUnattendedTicket(
        id: Int,
        newStatus: AssistanceTicketStatus,
        userId: Int,
    ): AssistanceTicketResponse

    suspend fun createTicket(value: AssistanceTicketRequest): AssistanceTicketResponse
    suspend fun findSubscriptionByNames(names: String): List<SubscriptionFastSearchResponse>
    suspend fun doMigration(migrationRequest: MigrationRequest): SubscriptionResponse
    suspend fun getOnuBySn(s: String): AdministrativeOnuResponse
    suspend fun deleteOnuFromOlt(onuExternalId: String)
    suspend fun saveOutLay(apply: Outlay)
    suspend fun getElectronicPayers(subscriptionId: Int): List<String>
    suspend fun updateCustomerData(customer: CustomerData): Unit
    suspend fun subscriptionById(subscriptionId: Int): SubscriptionResponse
    suspend fun changeSubscriptionNapBox(request: MoveOnuRequest)
    suspend fun getRemoteAppVersion(): AppVersion
    suspend fun getTicketsByDateRange(
        closed: AssistanceTicketStatus,
        firstDayOfMonth: Long,
        lastDayOfMonth: Long
    ): List<AssistanceTicketResponse>
    suspend fun saveFixedCost(fixedCostRequest: FixedCostRequest)
    suspend fun getAllFixedCosts(): List<FixedCost>
    suspend fun findPaymentByElectronicPayerName(electronicPayerName: String): List<PayerFinderResult>
    suspend fun getPlaceFromLocation(latitude: Double, longitude: Double): PlaceResponse
    suspend fun updateSubscriptionLocation(subscriptionId: Int, latitude: Double, longitude: Double)
}