package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import android.widget.CompoundButton
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.presentation.extension.formIsValid
import com.dscorp.ispadmin.presentation.extension.removeSpecialCharacters
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.ui.features.forms.subscription.RegisterSubscriptionForm
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.CouponIsValid
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.FiberDevicesFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.FormDataFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.OnOnuDataFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.PlansFound
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.RefreshingOnus
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.RegisterSubscriptionSuccess
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.ShimmerVisibility
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionUiState.WirelessDevicesFound
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RegisterSubscriptionViewModel(
    private val repository: IRepository,
    val subscriptionForm: RegisterSubscriptionForm
) : BaseViewModel<RegisterSubscriptionUiState>() {


    var subscription: SubscriptionResponse? = null
    var installationType = MutableLiveData(InstallationType.FIBER)
    var selectedAdditionalDevice = MutableLiveData<NetworkDevice?>(null)
    val addButtonIsEnabled = selectedAdditionalDevice.map { it != null }
    var additionalNetworkDevicesList = mutableSetOf<NetworkDevice>()
    private val cpeDevices = MutableStateFlow<List<NetworkDevice>?>(null)

    private val fiberCpeDevices = cpeDevices.map { cpeDevices ->
        cpeDevices?.filter { it.networkDeviceType == NetworkDevice.NetworkDeviceType.FIBER_ROUTER }
    }

    private val wirelessCpeDevices = cpeDevices.map { cpeDevices ->
        cpeDevices?.filter { it.networkDeviceType == NetworkDevice.NetworkDeviceType.WIRELESS_ROUTER }
    }

    private val plans = MutableStateFlow<List<PlanResponse>?>(null)

    private val plansFiber = plans.map { plans ->
        plans?.filter { it.type == InstallationType.FIBER }
    }

    private val plansWireless = plans.map { plans ->
        plans?.filter { it.type == InstallationType.WIRELESS }
    }

    fun getFormData() =
        executeNoProgress(onSuccess = {
            uiState.value = BaseUiState(uiState = ShimmerVisibility(false))
        }) {
            val cpeDevicesJob = it.async { repository.getCpeDevices() }
            val cpeDevicesList = cpeDevicesJob.await()
            cpeDevices.value = cpeDevicesList

            val genericDevicesJob = it.async { repository.getGenericDevices() }
            val plansJob = it.async { repository.getPlans() }
            val placeJob = it.async { repository.getPlaces() }
            val napBoxesJob = it.async { repository.getNapBoxes() }
            val deferredTechnicians = it.async { repository.getTechnicians() }
            val coreDevicesJob = it.async { repository.getCoreDevices() }
            val unconfirmedOnusJob = it.async { repository.getUnconfirmedOnus() }

            val genericDevices = genericDevicesJob.await()
            this.plans.value = plansJob.await()
            val places = placeJob.await()
            val technicians = deferredTechnicians.await()
            val napBoxes = napBoxesJob.await()
            val coreDevices = coreDevicesJob.await()
            val unconfirmedOnus = unconfirmedOnusJob.await()
            uiState.value = BaseUiState(
                uiState = FormDataFound(
                    genericDevices, places,
                    technicians, napBoxes, coreDevices, unconfirmedOnus
                )
            )
        }

    fun getOnuData() = executeNoProgress(doFinally = {
        uiState.value = BaseUiState(uiState = RefreshingOnus(false))
    }) {
        uiState.value = BaseUiState(uiState = RefreshingOnus(true))
        val unconfirmedOnus = repository.getUnconfirmedOnus()
        uiState.value = BaseUiState(uiState = OnOnuDataFound(unconfirmedOnus))
    }


    fun getFiberPlans() = executeNoProgress {
        plansFiber.collectLatest {
            it?.let {
                uiState.value = BaseUiState(PlansFound(it))
            }
        }
    }

    fun getWirelessPlans() = executeNoProgress {
        plansWireless.collectLatest {
            it?.let {
                uiState.value = BaseUiState(PlansFound(it))
            }
        }
    }

    fun getFiberDevices() = executeNoProgress {
        fiberCpeDevices.collectLatest {
            it?.let {
                uiState.value = BaseUiState(FiberDevicesFound(it))
            }
        }
    }

    fun getWirelessDevices() = viewModelScope.launch {
        wirelessCpeDevices.collectLatest {
            uiState.value = BaseUiState(WirelessDevicesFound(it!!))
        }
    }

    fun activateCoupon() = executeWithProgress {
        val response = repository.applyCoupon(subscriptionForm.couponField.getValue()!!)
        if (response != null) uiState.value = BaseUiState(CouponIsValid(true))
        else uiState.value = BaseUiState(CouponIsValid(false))
    }

    fun registerSubscription() = executeWithProgress {
        if (!formIsValid()) return@executeWithProgress
        val subscription = createSubscription()
        val subscriptionFromRepository = repository.registerSubscription(subscription)
        uiState.value = BaseUiState(
            RegisterSubscriptionSuccess(subscriptionFromRepository)
        )
    }

    private fun createSubscription(): Subscription {
        with(subscriptionForm) {
            val subscription = Subscription(
                firstName = firstNameField.getValue()!!.removeSpecialCharacters(),
                lastName = lastNameField.getValue()!!.removeSpecialCharacters(),
                dni = dniField.getValue(),
                address = addressField.getValue(),
                phone = phoneField.getValue(),
                subscriptionDate = subscriptionDateField.getValue(),
                planId = planField.getValue()?.id,
                additionalDeviceIds = additionalNetworkDevicesList.map { it.id!! },
                placeId = placeField.getValue()?.id,
                technicianId = technicianField.getValue()?.id,
                hostDeviceId = hostDeviceField.getValue()?.id,
                location = GeoLocation(
                    locationField.getValue()?.latitude ?: 0.0,
                    locationField.getValue()?.longitude ?: 0.0
                ),
                cpeDeviceId = cpeDeviceField.getValue()?.id,
                installationType = installationType.value,
                price = priceField.getValue()?.toDouble(),
                coupon = couponField.getValue(),
                isMigration = migrationField.getValue(),
                note = noteField.getValue()
            )

            return when (installationType.value) {
                InstallationType.FIBER -> subscription.apply {
                    napBoxId = napBoxField.getValue()?.id
                    onu = onuField.getValue()
                }

                InstallationType.WIRELESS -> subscription
                InstallationType.ONLY_TV_FIBER -> subscription
                else -> throw Exception("Invalid Installation Type")
            }
        }
    }

    private fun formIsValid(): Boolean {
        with(subscriptionForm) {

            val formFields = mutableListOf(
                firstNameField,
                lastNameField,
                dniField,
                addressField,
                phoneField,
                priceField,
                locationField,
                planField,
                placeField,
                technicianField,
                hostDeviceField,
                subscriptionDateField,
                cpeDeviceField,
            )

            if (installationType.value == InstallationType.FIBER) {
                formFields.apply {
                    add(napBoxField)
                    add(onuField)
                }
            }

            return formFields.formIsValid()
        }
    }

    fun addSelectedAdditionalNetworkDeviceToList() {
        selectedAdditionalDevice.value?.let {
            additionalNetworkDevicesList.add(it)
        }
    }

    fun resetAdditionalDevicesValues() {
        selectedAdditionalDevice.value = null
        additionalNetworkDevicesList = mutableSetOf()
    }

    fun onIsMigrationCheckedChanged(button: CompoundButton, isChecked: Boolean) {
        subscriptionForm.migrationField.liveData.value = isChecked
    }

}



