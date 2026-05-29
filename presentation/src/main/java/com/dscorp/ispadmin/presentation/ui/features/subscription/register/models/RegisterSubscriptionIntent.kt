package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse

sealed interface RegisterSubscriptionIntent {
    data class FirstNameChanged(val value: String) : RegisterSubscriptionIntent
    data class LastNameChanged(val value: String) : RegisterSubscriptionIntent
    data class DniChanged(val value: String) : RegisterSubscriptionIntent
    data class AddressChanged(val value: String) : RegisterSubscriptionIntent
    data class PhoneChanged(val value: String) : RegisterSubscriptionIntent
    data class PlanSelected(val value: PlanResponse) : RegisterSubscriptionIntent
    data class PlaceSelected(val value: Place) : RegisterSubscriptionIntent
    data class OnuSelected(val value: Onu) : RegisterSubscriptionIntent
    data class NapBoxSelected(val value: NapBoxResponse) : RegisterSubscriptionIntent
    data object PlaceSelectionCleared : RegisterSubscriptionIntent
    data object NapBoxSelectionCleared : RegisterSubscriptionIntent
    data class InstallationTypeSelected(val type: InstallationType) : RegisterSubscriptionIntent
    data object RefreshOnuList : RegisterSubscriptionIntent
    data class NoteChanged(val value: String) : RegisterSubscriptionIntent
    data class EquipmentConditionChanged(val value: EquipmentCondition) : RegisterSubscriptionIntent
    data object RegisterClick : RegisterSubscriptionIntent
}
